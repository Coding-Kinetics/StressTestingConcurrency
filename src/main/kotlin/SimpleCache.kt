package org.example

class SimpleCache<K, V>(private val dataLoader: (K) -> V) {

    private val cache = HashMap<K, V>() // ConcurrentHashMap<K, V>()

    fun get(key: K): V {
        return cache.computeIfAbsent(key) { dataLoader(it) }
    }

    fun getAll(): Map<K, V> = cache.toMap()
}