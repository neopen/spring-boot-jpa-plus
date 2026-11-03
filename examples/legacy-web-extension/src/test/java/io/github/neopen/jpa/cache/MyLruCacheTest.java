package io.github.neopen.jpa.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MyLruCacheTest {

    @Test
    void evictsLeastRecentlyUsedEntry() {
        MyLruCache<String, Integer> cache = new MyLruCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);

        assertEquals(1, cache.get("a"));
        cache.put("c", 3);

        assertEquals(1, cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals(3, cache.get("c"));
        assertEquals(2, cache.size());
    }

    @Test
    void updatesExistingEntryWithoutDuplicatingIt() {
        MyLruCache<String, Integer> cache = new MyLruCache<>(1);
        cache.put("a", 1);
        cache.put("a", 2);

        assertEquals(2, cache.get("a"));
        assertEquals(1, cache.size());
    }
}
