package io.github.neopen.extension.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于哈希表和双向链表的最近最少使用缓存。
 *
 * <p>读取或写入条目时将其移动到链表头部，容量达到上限时
 * 优先处理链表尾部条目。</p>
 *
 * @param <V> 缓存值类型
 */
public class MyLruCache<V> {

    /** 缓存最大容量。 */
    private int capacity = 1024;

    /** 以缓存键索引链表节点。 */
    private final Map<String, ListNode<String, V>> table = new ConcurrentHashMap<>();
    /** 双向链表的头哨兵节点。 */
    private final ListNode<String, V> head;
    /** 双向链表的尾节点。 */
    private ListNode<String, V> tail;

    /**
     * 创建指定容量的缓存。
     *
     * @param capacity 最大缓存容量
     */
    public MyLruCache(int capacity) {
        this();
        this.capacity = capacity;
    }

    /**
     * 创建使用默认容量的缓存。
     */
    public MyLruCache() {
        head = new ListNode<>(null, null);
        tail = new ListNode<>(null, null);
        head.next = tail;
        head.prev = null;
        tail.prev = head;
        tail.next = null;
    }

    /**
     * 获取缓存值并将对应节点移动到链表头部。
     *
     * @param key 缓存键
     * @return 缓存值；未命中时返回 {@code null}
     */
    public V get(String key) {
        ListNode<String, V> node = table.get(key);
        // 节点不存在表示缓存未命中。
        if (node == null) {
            return null;
        }
        // 命中后先从原位置断开，再移动到链表头部。
        node.prev.next = node.next;
        node.next.prev = node.prev;

        // 将节点移动到头哨兵之后。
        node.next = head.next;
        head.next.prev = node;
        node.prev = head;
        head.next = node;
        table.put(key, node);
        return node.value;
    }

    /**
     * 写入缓存值并将对应节点放入链表头部。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void put(String key, V value) {
        ListNode<String, V> node = table.get(key);
        // 节点不存在表示缓存未命中。
        if (node == null) {
            if (table.size() == capacity) {
                // 达到容量上限时先处理尾部节点。
                table.remove(tail.prev.key);
                tail.prev = tail.next;
                tail.next = null;
                tail = tail.prev;
            }
            node = new ListNode<>(key, value);
            table.put(key, node);
        }
        // 新写入或已有节点都放置到链表头部。
        node.next = head.next;
        head.next.prev = node;
        node.prev = head;
        head.next = node;
    }

    /**
     * 缓存使用的双向链表节点。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public static class ListNode<K, V> {
        /** 节点键。 */
        private final K key;

        /** 节点值。 */
        private final V value;

        /** 前一个节点。 */
        private ListNode<K, V> prev;

        /** 后一个节点。 */
        private ListNode<K, V> next;

        /**
         * 创建链表节点。
         *
         * @param key   节点键
         * @param value 节点值
         */
        public ListNode(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "ListNode{" + "key=" + key + ", value=" + value + '}';
        }
    }


    /**
     * 演示缓存的基本读写。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        MyLruCache<ListNode> cache = new MyLruCache<>(4);
        ListNode<String, Integer> node1 = new ListNode<>("key1", 1);
        ListNode<String, Integer> node2 = new ListNode<>("key2", 2);
        ListNode<String, Integer> node3 = new ListNode<>("key3", 3);
        ListNode<String, Integer> node4 = new ListNode<>("key4", 4);
        ListNode<String, Integer> node5 = new ListNode<>("key5", 5);
        cache.put("key1", node1);
        cache.put("key2", node2);
        cache.put("key3", node3);
        cache.put("key4", node4);
        System.out.println(cache.get("key1"));

        cache.put("key5", node5);
        System.out.println(cache.get("key2"));
        System.out.println(cache.get("key3"));

    }
}
