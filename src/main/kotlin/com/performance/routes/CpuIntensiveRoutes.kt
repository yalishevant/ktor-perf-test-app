package com.performance.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
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
    val last10Primes: List<Int>,
    val calculationTimeNs: Long
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
 * Data class representing the response from the recursive endpoint.
 */
@Serializable
data class RecursiveResponse(
    val depth: Int,
    val calculationTimeNs: Long,
    val result: Double
)

/**
 * Data class representing the response from the parallel endpoint.
 */
@Serializable
data class ParallelResponse(
    val iterations: Int,
    val threads: Int,
    val calculationTimeNs: Long,
    val results: List<Double>
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
            val startTime = System.nanoTime()
            val primes = withContext(Dispatchers.Default) {
                calculatePrimes(limit)
            }
            val endTime = System.nanoTime()

            call.respond(PrimesResponse(
                count = primes.size,
                limit = limit,
                last10Primes = primes.takeLast(10),
                calculationTimeNs = (endTime - startTime)
            ))
        }

        /**
         * Sorting endpoint.
         * Generates and sorts a random array of integers using multiple sorting algorithms.
         * Query parameters:
         * - size: The size of the array to sort (default: 100000)
         */
        get("/sort") {
            val size = call.parameters["size"]?.toIntOrNull() ?: 100000

            // Use Dispatchers.Default for CPU-bound operations
            val result = withContext(Dispatchers.Default) {
                // Generate multiple arrays to sort
                val array1 = generateRandomArray(size)
                val array2 = array1.copyOf()
                val array3 = array1.copyOf()

                // Sort using built-in sort (quicksort)
                val startTime = System.nanoTime()

                // First sort with built-in sort
                array1.sort()

                // Then sort with bubble sort (inefficient, but CPU-intensive)
                bubbleSort(array2)

                // Then sort with merge sort
                val array3Sorted = mergeSort(array3.toList())

                val endTime = System.nanoTime()

                // Verify all sorts produced the same result (sanity check)
                val allSortsMatch = array1.contentEquals(array2) && 
                                   array1.contentEquals(array3Sorted.toIntArray())

                SortResponse(
                    size = size,
                    sortTimeNs = (endTime - startTime),
                    first10Elements = array1.take(10),
                    last10Elements = array1.takeLast(10)
                )
            }

            call.respond(result)
        }

        /**
         * Matrix multiplication endpoint.
         * Generates random matrices and performs multiple operations on them.
         * Query parameters:
         * - size: The size of the matrices (default: 200)
         */
        get("/matrix") {
            val size = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(500) ?: 200

            // Use Dispatchers.Default for CPU-bound operations
            val result = withContext(Dispatchers.Default) {
                // Generate three random matrices
                val matrixA = generateRandomMatrix(size)
                val matrixB = generateRandomMatrix(size)
                val matrixC = generateRandomMatrix(size)

                val startTime = System.nanoTime()

                // Perform multiple matrix operations
                // 1. Multiply A and B
                val resultAB = multiplyMatrices(matrixA, matrixB)

                // 2. Multiply the result with C
                val resultABC = multiplyMatrices(resultAB, matrixC)

                // 3. Calculate the transpose of the result
                val transposeABC = transposeMatrix(resultABC)

                // 4. Multiply the original result with its transpose
                val finalResult = multiplyMatrices(resultABC, transposeABC)

                // 5. Calculate determinant of a submatrix (for smaller matrices)
                val detValue = if (size <= 10) {
                    calculateDeterminant(extractSubMatrix(finalResult, 0, 0, minOf(size, 10)))
                } else {
                    0.0 // Skip for large matrices
                }

                val endTime = System.nanoTime()

                MatrixResponse(
                    size = size,
                    multiplicationTimeNs = (endTime - startTime),
                    sampleValue = finalResult[0][0]
                )
            }

            call.respond(result)
        }

        /**
         * Recursive computation endpoint.
         * Performs a CPU-intensive recursive calculation.
         * Query parameters:
         * - depth: The recursion depth (default: 30)
         */
        get("/recursive") {
            val depth = call.parameters["depth"]?.toIntOrNull()?.coerceIn(10, 40) ?: 30

            // Use Dispatchers.Default for CPU-bound operations
            val result = withContext(Dispatchers.Default) {
                val startTime = System.nanoTime()

                // Perform recursive Fibonacci calculation
                val fibResult = calculateFibonacci(depth)

                val endTime = System.nanoTime()

                RecursiveResponse(
                    depth = depth,
                    calculationTimeNs = (endTime - startTime),
                    result = fibResult
                )
            }

            call.respond(result)
        }

        /**
         * Parallel processing endpoint.
         * Performs CPU-intensive calculations in parallel.
         * Query parameters:
         * - iterations: The number of iterations (default: 1000000)
         * - threads: The number of parallel threads (default: 4)
         */
        get("/parallel") {
            val iterations = call.parameters["iterations"]?.toIntOrNull()?.coerceAtMost(10000000) ?: 1000000
            val threads = call.parameters["threads"]?.toIntOrNull()?.coerceIn(1, 16) ?: 4

            // Use Dispatchers.Default for CPU-bound operations
            val result = withContext(Dispatchers.Default) {
                val startTime = System.nanoTime()

                // Perform parallel calculations
                val results = parallelCalculation(iterations, threads)

                val endTime = System.nanoTime()

                ParallelResponse(
                    iterations = iterations,
                    threads = threads,
                    calculationTimeNs = (endTime - startTime),
                    results = results.take(10)
                )
            }

            call.respond(result)
        }
    }
}

/**
 * Calculates prime numbers up to the specified limit.
 * Uses a more CPU-intensive approach combining Sieve of Eratosthenes with primality testing.
 */
private fun calculatePrimes(limit: Int): List<Int> {
    if (limit <= 1) return emptyList()

    // First use Sieve of Eratosthenes to get candidates
    val isPrime = BooleanArray(limit + 1) { true }
    isPrime[0] = false
    isPrime[1] = false

    for (i in 2..sqrt(limit.toDouble()).toInt()) {
        if (isPrime[i]) {
            var j = i * i
            while (j <= limit) {
                isPrime[j] = false
                j += i
            }
        }
    }

    val primes = mutableListOf<Int>()

    // Then do additional primality testing for each candidate
    // This is more CPU-intensive than necessary but good for benchmarking
    for (i in 2..limit) {
        if (isPrime[i] && isPrimeWithTrialDivision(i)) {
            // Do some extra math operations to increase CPU load
            val sinValue = sin(i.toDouble())
            val cosValue = cos(i.toDouble())
            val tanValue = tan(i.toDouble())

            // Only add if it passes this arbitrary condition (always true, but forces computation)
            if (sinValue * sinValue + cosValue * cosValue >= 0.99) {
                primes.add(i)
            }
        }
    }

    return primes
}

/**
 * Checks if a number is prime using trial division.
 * This is less efficient than Sieve of Eratosthenes but adds more CPU load.
 */
private fun isPrimeWithTrialDivision(n: Int): Boolean {
    if (n <= 1) return false
    if (n <= 3) return true
    if (n % 2 == 0 || n % 3 == 0) return false

    var i = 5
    while (i * i <= n) {
        if (n % i == 0 || n % (i + 2) == 0) return false
        i += 6
    }
    return true
}

/**
 * Generates a random array of integers.
 */
private fun generateRandomArray(size: Int): IntArray {
    return IntArray(size) { Random.nextInt(0, 1000000) }
}

/**
 * Bubble sort implementation.
 * This is an inefficient sorting algorithm, but it's CPU-intensive.
 */
private fun bubbleSort(array: IntArray) {
    val n = array.size
    for (i in 0 until n - 1) {
        for (j in 0 until n - i - 1) {
            if (array[j] > array[j + 1]) {
                // Swap elements
                val temp = array[j]
                array[j] = array[j + 1]
                array[j + 1] = temp
            }
        }
    }
}

/**
 * Merge sort implementation.
 * This is a more efficient sorting algorithm than bubble sort.
 */
private fun mergeSort(list: List<Int>): List<Int> {
    if (list.size <= 1) {
        return list
    }

    val middle = list.size / 2
    val left = list.subList(0, middle)
    val right = list.subList(middle, list.size)

    return merge(mergeSort(left), mergeSort(right))
}

/**
 * Merge function for merge sort.
 */
private fun merge(left: List<Int>, right: List<Int>): List<Int> {
    var indexLeft = 0
    var indexRight = 0
    val result = mutableListOf<Int>()

    while (indexLeft < left.size && indexRight < right.size) {
        if (left[indexLeft] <= right[indexRight]) {
            result.add(left[indexLeft])
            indexLeft++
        } else {
            result.add(right[indexRight])
            indexRight++
        }
    }

    while (indexLeft < left.size) {
        result.add(left[indexLeft])
        indexLeft++
    }

    while (indexRight < right.size) {
        result.add(right[indexRight])
        indexRight++
    }

    return result
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

/**
 * Transposes a matrix.
 */
private fun transposeMatrix(matrix: Array<IntArray>): Array<IntArray> {
    val size = matrix.size
    val result = Array(size) { IntArray(size) }

    for (i in 0 until size) {
        for (j in 0 until size) {
            result[j][i] = matrix[i][j]
        }
    }

    return result
}

/**
 * Extracts a submatrix of the specified size from the given matrix.
 */
private fun extractSubMatrix(matrix: Array<IntArray>, startRow: Int, startCol: Int, size: Int): Array<DoubleArray> {
    val result = Array(size) { DoubleArray(size) }

    for (i in 0 until size) {
        for (j in 0 until size) {
            if (startRow + i < matrix.size && startCol + j < matrix[0].size) {
                result[i][j] = matrix[startRow + i][startCol + j].toDouble()
            }
        }
    }

    return result
}

/**
 * Calculates the determinant of a matrix using a recursive approach.
 * This is a CPU-intensive operation, especially for larger matrices.
 */
private fun calculateDeterminant(matrix: Array<DoubleArray>): Double {
    val n = matrix.size

    if (n == 1) {
        return matrix[0][0]
    }

    if (n == 2) {
        return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
    }

    var determinant = 0.0
    for (j in 0 until n) {
        determinant += Math.pow(-1.0, j.toDouble()) * matrix[0][j] * calculateDeterminant(getMinor(matrix, 0, j))
    }

    return determinant
}

/**
 * Gets the minor of a matrix by removing the specified row and column.
 */
private fun getMinor(matrix: Array<DoubleArray>, row: Int, col: Int): Array<DoubleArray> {
    val n = matrix.size
    val minor = Array(n - 1) { DoubleArray(n - 1) }

    var r = 0
    for (i in 0 until n) {
        if (i == row) continue

        var c = 0
        for (j in 0 until n) {
            if (j == col) continue
            minor[r][c] = matrix[i][j]
            c++
        }
        r++
    }

    return minor
}

/**
 * Calculates the Fibonacci number at the specified position using recursion.
 * This is deliberately inefficient to create CPU load.
 */
private fun calculateFibonacci(n: Int): Double {
    if (n <= 0) return 0.0
    if (n == 1) return 1.0

    // Add some extra computation to make it more CPU-intensive
    val result = calculateFibonacci(n - 1) + calculateFibonacci(n - 2)

    // Do some extra math operations to increase CPU load
    val sinValue = sin(result)
    val cosValue = cos(result)

    return result * (sinValue * sinValue + cosValue * cosValue)
}

/**
 * Performs CPU-intensive calculations in parallel.
 * Returns a list of results.
 */
private suspend fun parallelCalculation(iterations: Int, threads: Int): List<Double> = coroutineScope {
    val iterationsPerThread = iterations / threads
    val results = mutableListOf<Double>()

    // Create and execute parallel tasks
    val deferreds = List(threads) { threadIndex ->
        async {
            var result = 0.0
            val startValue = threadIndex * iterationsPerThread

            // Perform CPU-intensive calculations
            for (i in startValue until startValue + iterationsPerThread) {
                // Calculate a complex mathematical expression
                val x = i.toDouble() / iterations
                result += sin(x) * cos(x) * tan(x.pow(2)) + sqrt(abs(sin(x.pow(3))))

                // Add some branching to make it harder to optimize
                if (i % 2 == 0) {
                    result = result.pow(1.01)
                } else {
                    result = sqrt(result.pow(2) + 1.0)
                }
            }

            result
        }
    }

    // Collect results
    deferreds.awaitAll().forEach { results.add(it) }

    results
}

/**
 * Returns the absolute value of a double.
 */
private fun abs(value: Double): Double {
    return if (value < 0) -value else value
}
