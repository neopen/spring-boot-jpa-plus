/*
 * Copyright 2026 neopen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.persistence.audit.trail;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步变更历史发布者。
 *
 * <p>把发布动作交给后台线程，避免审计写入拖慢业务事务。队列有界，
 * 队列满时丢弃新记录并输出 WARNING 日志，不会阻塞业务线程。</p>
 *
 * <pre>{@code
 * @Bean(destroyMethod = "close")
 * public AuditLogPublisher auditLogPublisher(AuditLogRepository repo) {
 *     return new AsyncAuditLogPublisher(repo::save, 1024);
 * }
 * }</pre>
 *
 * <p>容器关闭时调用 {@link #close()}，等待队列中的记录处理完毕；
 * 超时后中断后台线程，剩余记录会被丢弃并输出 WARNING 日志。</p>
 *
 * <p>默认使用单个后台线程，保证同一进程内的审计记录顺序与产生
 * 顺序一致。确需提高吞吐量时可以增加线程数，但顺序不再有保证。</p>
 *
 * @author neopen
 * @see AuditLogPublisher
 * @see AuditTrails#setPublisher(AuditLogPublisher)
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class AsyncAuditLogPublisher
        implements AuditLogPublisher, AutoCloseable {

    /** 日志器。 */
    private static final System.Logger LOG =
            System.getLogger(AsyncAuditLogPublisher.class.getName());

    /** 默认队列容量。 */
    private static final int DEFAULT_QUEUE_CAPACITY = 1024;

    /** 后台线程取队列的超时时间，单位毫秒。 */
    private static final long POLL_TIMEOUT_MS = 200L;

    /** 关闭时等待队列处理完成的时间，单位秒。 */
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    /** 委托发布者。 */
    private final AuditLogPublisher delegate;

    /** 有界记录队列。 */
    private final BlockingQueue<AuditEntry> queue;

    /** 后台执行器。 */
    private final ExecutorService executor;

    /** 丢弃记录数量。 */
    private final AtomicLong discarded = new AtomicLong();

    /** 是否已经关闭。 */
    private volatile boolean closed;

    /**
     * 使用单个后台线程与默认队列容量创建异步发布者。
     *
     * @param delegate 委托发布者，不能为 {@code null}
     */
    public AsyncAuditLogPublisher(AuditLogPublisher delegate) {
        this(delegate, DEFAULT_QUEUE_CAPACITY);
    }

    /**
     * 使用单个后台线程创建异步发布者。
     *
     * @param delegate      委托发布者，不能为 {@code null}
     * @param queueCapacity 队列容量，必须大于 0
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public AsyncAuditLogPublisher(
            AuditLogPublisher delegate,
            int queueCapacity
    ) {
        this(delegate, queueCapacity, 1);
    }

    /**
     * 创建异步发布者。
     *
     * @param delegate      委托发布者，不能为 {@code null}
     * @param queueCapacity 队列容量，必须大于 0
     * @param workerThreads 后台线程数，必须大于 0
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public AsyncAuditLogPublisher(
            AuditLogPublisher delegate,
            int queueCapacity,
            int workerThreads
    ) {
        if (delegate == null) {
            throw new IllegalArgumentException(
                    "Audit log delegate must not be null"
            );
        }

        if (queueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Queue capacity must be greater than zero"
            );
        }

        if (workerThreads <= 0) {
            throw new IllegalArgumentException(
                    "Worker threads must be greater than zero"
            );
        }

        this.delegate = delegate;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.executor = Executors.newFixedThreadPool(
                workerThreads,
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "jpa-plus-audit"
                    );

                    thread.setDaemon(true);

                    return thread;
                }
        );

        for (int i = 0; i < workerThreads; i++) {
            executor.execute(this::drain);
        }
    }

    /**
     * 投递一条变更历史。
     *
     * <p>队列满时丢弃并记录 WARNING 日志；已经关闭时退化为同步
     * 发布，尽量不丢失记录。</p>
     *
     * @param entry 变更历史，允许为 {@code null}
     */
    @Override
    public void publish(AuditEntry entry) {
        if (entry == null) {
            return;
        }

        if (closed) {
            deliver(entry);
            return;
        }

        if (queue.offer(entry)) {
            return;
        }

        discarded.incrementAndGet();
        LOG.log(
                System.Logger.Level.WARNING,
                "Audit log queue is full, entry discarded: {0}#{1}",
                entry.entityName(),
                entry.entityId()
        );
    }

    /**
     * 关闭后台线程。
     *
     * <p>等待队列中的记录处理完毕；超时后中断后台线程，剩余记录
     * 会被丢弃。关闭后再次投递会退化为同步发布。</p>
     */
    @Override
    public void close() {
        closed = true;
        executor.shutdown();

        try {
            if (!executor.awaitTermination(
                    SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {
                LOG.log(
                        System.Logger.Level.WARNING,
                        "Timed out flushing audit log queue, "
                                + "{0} entries left",
                        queue.size()
                );
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    /**
     * 获取因队列满而丢弃的记录数量。
     *
     * @return 丢弃数量
     */
    public long discardedCount() {
        return discarded.get();
    }

    /**
     * 获取当前排队中的记录数量。
     *
     * @return 排队数量
     */
    public int queuedCount() {
        return queue.size();
    }

    /**
     * 判断是否已经关闭。
     *
     * @return 已关闭时返回 {@code true}
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 循环消费队列，直到关闭且队列为空。
     */
    private void drain() {
        while (true) {
            AuditEntry entry;

            try {
                entry = queue.poll(
                        POLL_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (entry == null) {
                if (closed && queue.isEmpty()) {
                    return;
                }

                continue;
            }

            deliver(entry);
        }
    }

    /**
     * 发布单条记录，异常只记录日志不影响后台线程。
     *
     * @param entry 变更历史
     */
    private void deliver(AuditEntry entry) {
        try {
            delegate.publish(entry);
        } catch (RuntimeException e) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "Failed to publish audit entry: "
                            + entry.entityName() + "#" + entry.entityId(),
                    e
            );
        }
    }
}
