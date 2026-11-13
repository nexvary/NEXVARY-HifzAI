package com.nexvary.hifzai.learning

/** A failed blocking read invalidates the capture unless the caller requested a stop. */
internal fun readLocalPcm(
    output: FloatArray,
    shouldStop: () -> Boolean,
    read: (ShortArray, Int) -> Int,
): Int? {
    val chunk = ShortArray(2048)
    var count = 0
    try {
        while (count < output.size && !shouldStop()) {
            val requested = minOf(chunk.size, output.size - count)
            val received = read(chunk, requested)
            if (received !in 1..requested) return if (shouldStop()) count else null
            repeat(received) { index -> output[count++] = chunk[index] / 32768f }
        }
        return count
    } finally {
        chunk.fill(0)
    }
}
