package io.github.neopen.core.id;

/**
 * 雪花算法 ID 生成器。
 *
 * <p>64 位 ID 结构为：41 位时间戳、5 位数据中心 ID、5 位机器 ID、
 * 12 位序列号。默认纪元为 2020-01-01T00:00:00Z，可使用自定义纪元适配
 * 已有数据。单实例内每秒最多生成约 409.6 万个 ID。</p>
 *
 * <pre>{@code
 * SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
 * long id = generator.nextId();
 * }</pre>
 *
 * <p>同一 JVM 内应使用单例；多实例部署时需保证机器 ID 与数据中心 ID
 * 组合唯一。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class SnowflakeIdGenerator {

    /** 默认纪元：2020-01-01T00:00:00Z。 */
    public static final long DEFAULT_EPOCH = 1_577_836_800_000L;

    /** 机器 ID 位数。 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心 ID 位数。 */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /** 序列号位数。 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器 ID 最大值。 */
    private static final long MAX_WORKER_ID =
            ~(-1L << WORKER_ID_BITS);

    /** 数据中心 ID 最大值。 */
    private static final long MAX_DATA_CENTER_ID =
            ~(-1L << DATA_CENTER_ID_BITS);

    /** 序列号掩码。 */
    private static final long SEQUENCE_MASK =
            ~(-1L << SEQUENCE_BITS);

    /** 机器 ID 左移位数。 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心 ID 左移位数。 */
    private static final long DATA_CENTER_ID_SHIFT =
            SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数。 */
    private static final long TIMESTAMP_SHIFT =
            SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /** 机器 ID。 */
    private final long workerId;

    /** 数据中心 ID。 */
    private final long dataCenterId;

    /** 纪元时间戳。 */
    private final long epoch;

    /** 当前序列号。 */
    private long sequence;

    /** 上次生成 ID 的时间戳。 */
    private long lastTimestamp = -1L;

    /**
     * 使用默认纪元创建生成器。
     *
     * @param workerId     机器 ID，取值 0 到 31
     * @param dataCenterId 数据中心 ID，取值 0 到 31
     * @throws IllegalArgumentException 参数越界时抛出
     */
    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        this(workerId, dataCenterId, DEFAULT_EPOCH);
    }

    /**
     * 使用自定义纪元创建生成器。
     *
     * @param workerId     机器 ID，取值 0 到 31
     * @param dataCenterId 数据中心 ID，取值 0 到 31
     * @param epoch        纪元时间戳，单位毫秒，不能为负数
     * @throws IllegalArgumentException 参数越界时抛出
     */
    public SnowflakeIdGenerator(
            long workerId,
            long dataCenterId,
            long epoch
    ) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "Worker id must be between 0 and " + MAX_WORKER_ID
            );
        }

        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException(
                    "Data center id must be between 0 and "
                            + MAX_DATA_CENTER_ID
            );
        }

        if (epoch < 0) {
            throw new IllegalArgumentException(
                    "Epoch must not be negative"
            );
        }

        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        this.epoch = epoch;
    }

    /**
     * 生成下一个 ID。
     *
     * <p>方法为同步方法，可安全用于多线程环境。检测到系统时钟回拨时
     * 抛出异常，避免生成重复 ID。</p>
     *
     * @return 雪花 ID
     * @throws IllegalStateException 系统时钟回拨时抛出
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards by "
                            + (lastTimestamp - timestamp) + " ms"
            );
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;

            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一个毫秒。
     *
     * @param lastTimestamp 当前毫秒
     * @return 大于当前毫秒的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();

        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }

        return timestamp;
    }
}
