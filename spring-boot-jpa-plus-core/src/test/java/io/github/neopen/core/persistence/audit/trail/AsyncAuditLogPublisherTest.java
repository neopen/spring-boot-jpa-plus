package io.github.neopen.core.persistence.audit.trail;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AsyncAuditLogPublisher} 单元测试。
 */
class AsyncAuditLogPublisherTest {

    /**
     * 验证记录被异步投递给委托发布者。
     *
     * @throws InterruptedException 等待被中断时抛出
     */
    @Test
    void shouldPublishAsynchronously() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<AuditEntry> received = new CopyOnWriteArrayList<>();
        AsyncAuditLogPublisher publisher = new AsyncAuditLogPublisher(entry -> {
            received.add(entry);
            latch.countDown();
        });

        try {
            publisher.publish(entry("Order"));

            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        } finally {
            publisher.close();
        }

        assertThat(received).hasSize(1);
        assertThat(received.get(0).entityName()).isEqualTo("Order");
    }

    /**
     * 验证队列满时丢弃新记录并计数。
     *
     * @throws InterruptedException 等待被中断时抛出
     */
    @Test
    void shouldDiscardWhenQueueIsFull() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AsyncAuditLogPublisher publisher = new AsyncAuditLogPublisher(
                entry -> {
                    started.countDown();
                    await(release);
                },
                1
        );

        try {
            publisher.publish(entry("A"));
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();

            publisher.publish(entry("B"));
            publisher.publish(entry("C"));

            assertThat(publisher.discardedCount()).isEqualTo(1);
        } finally {
            release.countDown();
            publisher.close();
        }
    }

    /**
     * 验证关闭时等待队列处理完成。
     */
    @Test
    void shouldFlushQueueOnClose() {
        List<AuditEntry> received = new CopyOnWriteArrayList<>();
        AsyncAuditLogPublisher publisher = new AsyncAuditLogPublisher(
                received::add,
                16
        );

        for (int i = 0; i < 10; i++) {
            publisher.publish(entry("Order" + i));
        }

        publisher.close();

        assertThat(publisher.isClosed()).isTrue();
        assertThat(publisher.queuedCount()).isZero();
        assertThat(received).hasSize(10);
    }

    /**
     * 验证关闭后投递退化为同步发布。
     */
    @Test
    void shouldPublishSynchronouslyAfterClose() {
        List<AuditEntry> received = new CopyOnWriteArrayList<>();
        AsyncAuditLogPublisher publisher = new AsyncAuditLogPublisher(
                received::add
        );

        publisher.close();
        publisher.publish(entry("AfterClose"));

        assertThat(received).hasSize(1);
    }

    /**
     * 验证委托发布者异常不会影响后续记录。
     *
     * @throws InterruptedException 等待被中断时抛出
     */
    @Test
    void shouldContinueAfterDelegateFailure() throws InterruptedException {
        AtomicBoolean failed = new AtomicBoolean();
        CountDownLatch second = new CountDownLatch(1);
        AsyncAuditLogPublisher publisher = new AsyncAuditLogPublisher(entry -> {
            if (entry.entityName().equals("Broken")) {
                failed.set(true);
                throw new IllegalStateException("boom");
            }

            second.countDown();
        });

        try {
            publisher.publish(entry("Broken"));
            publisher.publish(entry("Order"));

            assertThat(second.await(3, TimeUnit.SECONDS)).isTrue();
        } finally {
            publisher.close();
        }

        assertThat(failed).isTrue();
    }

    /**
     * 验证非法参数被拒绝。
     */
    @Test
    void shouldRejectInvalidArguments() {
        assertThatThrownBy(() -> new AsyncAuditLogPublisher(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delegate");

        assertThatThrownBy(() -> new AsyncAuditLogPublisher(entry -> {
        }, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Queue capacity");
    }

    /**
     * 创建测试记录。
     *
     * @param entityName 实体名称
     * @return 变更历史
     */
    private static AuditEntry entry(String entityName) {
        return new AuditEntry(
                entityName,
                Object.class,
                1L,
                AuditAction.UPDATE,
                List.of(),
                "tester",
                Instant.now()
        );
    }

    /**
     * 等待开关放开。
     *
     * @param latch 开关
     */
    private static void await(CountDownLatch latch) {
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
