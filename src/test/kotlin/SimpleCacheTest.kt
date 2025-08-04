import org.example.SimpleCache
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *  Thread Coordination Primitives
 *  - [CyclicBarrier] tests for race conditions/deadlocks by making threads to wait for each other before continuing
 *  - [CountDownLatch] lets the main thread wait for other threads to finish
 *
 *  Thread-Safe Data Structures
 */
class SimpleCacheTest {

    @Test
    fun `Break simple cache with concurrent get calls`() {
        val numThreads = 10
        val service = Executors.newFixedThreadPool(numThreads)

        val loadCount = AtomicInteger(0)

        val cache = SimpleCache<String, List<Int>> { key ->
            loadCount.incrementAndGet()
            Thread.sleep(10) // Simulate expensive load
            listOf(1, 2)
        }

        val barrier = CyclicBarrier(numThreads)
        val latch = CountDownLatch(numThreads)

        repeat(numThreads) {
            service.submit {
                try {
                    barrier.await()
                    val result = cache.get("shared-key")
                    assertEquals(listOf(1, 2), result)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        service.shutdown()

        println("Loader was called ${loadCount.get()} times")
        assertEquals(1, loadCount.get(), "Loader should be called only once for the same key")
    }
}