package org.example.backpressure

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class VaccinationPen(cows: Int) {
    private val pen = LinkedBlockingQueue<String>(cows)
    val totalVaccinated = AtomicInteger(0)

    fun processCow(cowId: String) {
        pen.put(cowId)
        println("[PRODUCER] $cowId entered the pen.")
    }

    fun vaccinate() {
        val cow = pen.poll(500, TimeUnit.MILLISECONDS)
        if (cow != null) {
            Thread.sleep(50) // simulated processing
            totalVaccinated.incrementAndGet()
            println("[CONSUMER] Vaccinated $cow. Total: ${totalVaccinated.get()}")
        } else {
            println("[CONSUMER] Pen is empty, Vet is waiting...")
        }
    }
}