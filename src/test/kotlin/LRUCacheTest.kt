import org.example.EnhancedCache
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LRUCacheTest {

    @Test
    fun `Stress test concurrency calls for cache consistency and validating for no crashes`() {
        val numThreads = 10
        val numReadsPerThread = 100
        val service = Executors.newFixedThreadPool(numThreads)
        val barrier = CyclicBarrier(numThreads)
        val latch = CountDownLatch(numThreads)

        // Track how many times the loader is called per key
        val loadCounts = ConcurrentHashMap<String, AtomicInteger>()

        val cache = EnhancedCache<String, List<Int>>(
            maxSize = 10,
            ttlMillis = 1000L
        ) { key ->
            loadCounts.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
            Thread.sleep(5) // Simulate expensive load
            listOf(1, 2) // Simulated expensive computation
        }

        val exceptions = ConcurrentLinkedQueue<Throwable>()
        val resultsSizeCounter = AtomicInteger(0)

        repeat(numThreads) { i ->
            service.submit {
                try {
                    barrier.await()

                    repeat(numReadsPerThread) { j ->
                        val key = "key-${j % 5}" // only 5 keys shared by all threads
                        val result = cache.get(key)
                        resultsSizeCounter.incrementAndGet()

                        assertNotNull(result, "Result is null on read attempt $j in thread $i")
                        assertEquals(2, result.size, "Result should be size 2 on read attempt $j in thread $i")
                    }

                } catch (e: Exception) {
                    exceptions.add(e)
                    System.err.println("Thread $i encountered an exception: ${e.message}")
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for all threads to finish
        val completed = latch.await(30, TimeUnit.SECONDS)
        service.shutdown()

        // Final assertions must run on main test thread
        assertTrue(completed, "Timeout: not all threads completed in time")
        assertTrue(exceptions.isEmpty(), "Some threads encountered exceptions")
        assertEquals(
            numThreads * numReadsPerThread,
            resultsSizeCounter.get(),
            "All expected reads should have completed successfully"
        )


        //This will fail if cache is not thread-safe
        loadCounts.forEach { (key, count) ->
            println("Loader call count for $key = ${count.get()}")
            assertEquals(1, count.get(), "Loader was called multiple times for key=$key")
        }

        println("Cache size after test: ${cache.size()}")
        println("Cache contents: ${cache.getAll()}")
    }
}