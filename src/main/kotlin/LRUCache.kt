package org.example

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class EnhancedCache<K, V>(
    private val maxSize: Int = 100,
    private val ttlMillis: Long = 10_000,
    private val dataLoader: (K) -> V
) {

    private data class CacheEntry<V>(
        val value: V,
        val expiryTime: Long
    )

    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val accessOrder = ConcurrentLinkedQueue<K>()

    fun get(key: K): V {
        val now = System.currentTimeMillis()

        val entry = cache[key]
        if (entry != null && entry.expiryTime > now) {
            touchKey(key)
            return entry.value
        }

        synchronized(this) {
            // Double-check after acquiring lock
            val freshEntry = cache[key]
            if (freshEntry != null && freshEntry.expiryTime > now) {
                touchKey(key)
                return freshEntry.value
            }

            val newValue = dataLoader(key)
            val newEntry = CacheEntry(newValue, now + ttlMillis)
            cache[key] = newEntry
            accessOrder.add(key)

            enforceSizeLimit()
            return newValue
        }
    }

    private fun touchKey(key: K) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }

    private fun enforceSizeLimit() {
        while (cache.size > maxSize) {
            val oldestKey = accessOrder.poll()
            if (oldestKey != null) {
                cache.remove(oldestKey)
            }
        }
    }

    fun size(): Int = cache.size

    fun getAll(): Map<K, V> {
        val now = System.currentTimeMillis()
        return cache.filterValues { it.expiryTime > now }.mapValues { it.value.value }
    }
}