import org.example.EnhancedCache
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.Future
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
        val barrier = CyclicBarrier(numThreads)  // force threads to run at once

        val cache = EnhancedCache<String, List<Int>>(
            maxSize = 100,
            ttlMillis = 5000L
        ) { key ->
            listOf(1, 2) // Simulated expensive computation
        }

        /**
         * Set up your class to test
         */
        val exceptions = ConcurrentLinkedQueue<Throwable>()
        val resultsSizeCounter = AtomicInteger(0)
        val tasks = mutableListOf<Future<*>>()
        for (i in 0 until numThreads) {
            val task = service.submit {
                try {
                    barrier.await()                                             // ensure all threads start together
                    for (j in 0 until numReadsPerThread) {


                        val result = cache.get("some_key")
                        resultsSizeCounter.incrementAndGet()

                        assertNotNull(actual = result, message = "Result is null on read attempt $j in thread $i")
                        assertEquals(
                            expected = 2,
                            actual = result.size,
                            message = "Result should be size 2 read attempt $j in thread $i",
                        )
                    }
                } catch (e: Exception) {
                    exceptions.add(e)
                    System.err.println("Thread $i encountered an exception: ${e.message}")
                    e.printStackTrace()
                } finally {
                    service.shutdown()
                }
            }
            tasks.add(task)
        }

        service.shutdown()
        service.awaitTermination(30, TimeUnit.SECONDS)

        assertTrue(actual = exceptions.isEmpty(), message = "All expected reads should have completed successfully.")
        assertEquals(
            expected = numThreads * numReadsPerThread,
            actual = resultsSizeCounter.get(),
            message = "All expected reads should have completed successfully.",
        )

        /**
         * assert consistency and correctness for each result in cache
         */
        println("Cache size after test: ${cache.size()}")
        println("Cache contents: ${cache.getAll()}")
    }
}