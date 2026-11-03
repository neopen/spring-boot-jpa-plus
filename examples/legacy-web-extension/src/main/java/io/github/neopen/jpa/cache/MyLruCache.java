package io.github.neopen.jpa.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A small, synchronized, in-memory LRU cache.
 *
 * <p>This cache is process-local and is not suitable for distributed coordination.</p>
 */
public class MyLruCache<K, V> {

    private final int capacity;
    private final Map<K, Node<K, V>> index;
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    public MyLruCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        this.capacity = capacity;
        this.index = new HashMap<>(capacity);
        head.next = tail;
        tail.prev = head;
    }

    public synchronized V get(K key) {
        Node<K, V> node = index.get(key);
        if (node == null) {
            return null;
        }
        moveToHead(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Node<K, V> node = index.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
            return;
        }

        node = new Node<>(key, value);
        index.put(key, node);
        addAfterHead(node);

        if (index.size() > capacity) {
            Node<K, V> eldest = tail.prev;
            removeNode(eldest);
            index.remove(eldest.key);
        }
    }

    public synchronized int size() {
        return index.size();
    }

    public synchronized void clear() {
        index.clear();
        head.next = tail;
        tail.prev = head;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addAfterHead(node);
    }

    private void addAfterHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> prev;
        private Node<K, V> next;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
