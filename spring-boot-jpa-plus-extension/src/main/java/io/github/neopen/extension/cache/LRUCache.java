package io.github.neopen.extension.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 {@link LinkedHashMap} 的固定容量 LRU 缓存。
 *
 * <p>当条目数超过最大容量时，自动移除最久未访问或最早插入的
 * 条目，具体取决于构造器的访问顺序配置。</p>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 缓存允许保存的最大条目数。 */
    private final int maxSize;

    /**
     * 使用默认初始容量和负载因子创建缓存。
     *
     * @param maxSize 最大条目数
     */
    public LRUCache(int maxSize) {
        this(maxSize, 16, 0.75f, false);
    }


    /**
     * 使用指定初始容量创建缓存。
     *
     * @param maxSize         最大条目数
     * @param initialCapacity 初始容量
     */
    public LRUCache(int maxSize, int initialCapacity) {
        this(maxSize, initialCapacity, 0.75f, false);
    }

    /**
     * 创建可配置底层映射策略的缓存。
     *
     * @param maxSize         最大条目数
     * @param initialCapacity 初始容量
     * @param loadFactor      负载因子
     * @param accessOrder     是否按访问顺序排列条目
     */
    public LRUCache(int maxSize, int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
        this.maxSize = maxSize;
    }

    /**
     * 判断插入后是否应移除最老条目。
     *
     * @param eldest 最老条目
     * @return 当前条目数超过最大容量时返回 {@code true}
     */
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.size() > this.maxSize;
    }
}
