package com.performance.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Data class representing the response from the primes endpoint.
 */
@Serializable
data class PrimesResponse(
    val count: Int,
    val limit: Int,
    val last10Primes: List<Int>
)

/**
 * Data class representing the response from the sort endpoint.
 */
@Serializable
data class SortResponse(
    val size: Int,
    val sortTimeNs: Long,
    val first10Elements: List<Int>,
    val last10Elements: List<Int>
)

/**
 * Data class representing the response from the matrix endpoint.
 */
@Serializable
data class MatrixResponse(
    val size: Int,
    val multiplicationTimeNs: Long,
    val sampleValue: Int
)

/**
 * Extension function to register CPU-intensive routes.
 * These routes are designed to test CPU performance by performing computation-heavy operations.
 */
fun Routing.cpuIntensiveRoutes() {
    route("/cpu") {
        /**
         * Prime number calculation endpoint.
         * Calculates prime numbers up to the specified limit.
         * Query parameters:
         * - limit: The upper limit for prime number calculation (default: 100000)
         */
        get("/primes") {
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 100000

            // Use Dispatchers.Default for CPU-bound operations
            val primes = withContext(Dispatchers.Default) {
                calculatePrimes(limit)
            }

            call.respond(PrimesResponse(
                count = primes.size,
                limit = limit,
                last10Primes = primes.takeLast(10)
            ))
        }

        /**
         * Sorting endpoint.
         * Generates and sorts a random array of integers.
         * Query parameters:
         * - size: The size of the array to sort (default: 100000)
         */
        get("/sort") {
            val size = call.parameters["size"]?.toIntOrNull() ?: 100000

            // Use Dispatchers.Default for CPU-bound operations
            val result = withContext(Dispatchers.Default) {
                val array = generateRandomArray(size)
                val startTime = System.nanoTime()
                array.sort()
                val endTime = System.nanoTime()

                SortResponse(
                    size = size,
                    sortTimeNs = (endTime - startTime),
                    first10Elements = array.take(10),
                    last10Elements = array.takeLast(10)
                )
            }

            call.respond(result)
        }

        /**
         * Matrix multiplication endpoint.
         * Generates two random matrices and multiplies them.
         * Query parameters:
         * - size: The size of the matrices (default: 200)
         */
        get("/matrix") {
            val size = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(500) ?: 200

            // Use Dispatchers.Default for CPU-bound operations
            val result = withContext(Dispatchers.Default) {
                val matrixA = generateRandomMatrix(size)
                val matrixB = generateRandomMatrix(size)

                val startTime = System.nanoTime()
                val resultMatrix = multiplyMatrices(matrixA, matrixB)
                val endTime = System.nanoTime()

                MatrixResponse(
                    size = size,
                    multiplicationTimeNs = (endTime - startTime),
                    sampleValue = resultMatrix[0][0]
                )
            }

            call.respond(result)
        }
    }
}

/**
 * Calculates prime numbers up to the specified limit.
 * Uses the Sieve of Eratosthenes algorithm.
 */
private fun calculatePrimes(limit: Int): List<Int> {
    if (limit <= 1) return emptyList()

    val isPrime = BooleanArray(limit + 1) { true }
    isPrime[0] = false
    isPrime[1] = false

    val primes = mutableListOf<Int>()

    for (i in 2..limit) {
        if (isPrime[i]) {
            primes.add(i)

            // Mark multiples as non-prime
            var j = i * i
            while (j <= limit) {
                isPrime[j] = false
                j += i
            }
        }
    }

    return primes
}

/**
 * Generates a random array of integers.
 */
private fun generateRandomArray(size: Int): IntArray {
    return IntArray(size) { Random.nextInt(0, 1000000) }
}

/**
 * Generates a random matrix of the specified size.
 */
private fun generateRandomMatrix(size: Int): Array<IntArray> {
    return Array(size) { IntArray(size) { Random.nextInt(0, 100) } }
}

/**
 * Multiplies two matrices.
 */
private fun multiplyMatrices(a: Array<IntArray>, b: Array<IntArray>): Array<IntArray> {
    val size = a.size
    val result = Array(size) { IntArray(size) }

    for (i in 0 until size) {
        for (j in 0 until size) {
            var sum = 0
            for (k in 0 until size) {
                sum += a[i][k] * b[k][j]
            }
            result[i][j] = sum
        }
    }

    return result
}
