package com.performance.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Extension function to register I/O-intensive routes.
 * These routes are designed to test I/O performance by performing file operations and simulating database operations.
 */
fun Routing.ioIntensiveRoutes() {
    route("/io") {
        /**
         * Enhanced file write endpoint.
         * Performs multiple file write operations using different methods.
         * Query parameters:
         * - size: The size of the file to write in KB (default: 1024)
         * - method: The write method to use (1=standard, 2=buffered, 3=channel, 4=compressed, default: all)
         */
        get("/write") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val sizeBytes = sizeKB * 1024
            val method = call.parameters["method"]?.toIntOrNull() ?: 0 // 0 means all methods

            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()

                // Create random data once to use for all methods
                val randomData = Random.nextBytes(sizeBytes)

                // Results for each method
                val methodResults = mutableMapOf<String, Any>()

                // Perform file operations based on the selected method
                when (method) {
                    1 -> {
                        // Standard write
                        val tempFile = createTempFile("ktor-standard", ".tmp")
                        tempFile.deleteOnExit()
                        val standardTime = measureTime {
                            writeStandardFile(tempFile, randomData)
                        }
                        methodResults["standard"] = mapOf(
                            "timeNs" to standardTime,
                            "fileSize" to tempFile.length(),
                            "filePath" to tempFile.absolutePath
                        )
                    }
                    2 -> {
                        // Buffered write
                        val tempFile = createTempFile("ktor-buffered", ".tmp")
                        tempFile.deleteOnExit()
                        val bufferedTime = measureTime {
                            writeBufferedFile(tempFile, randomData)
                        }
                        methodResults["buffered"] = mapOf(
                            "timeNs" to bufferedTime,
                            "fileSize" to tempFile.length(),
                            "filePath" to tempFile.absolutePath
                        )
                    }
                    3 -> {
                        // Channel write
                        val tempFile = createTempFile("ktor-channel", ".tmp")
                        tempFile.deleteOnExit()
                        val channelTime = measureTime {
                            writeFileWithChannel(tempFile, randomData)
                        }
                        methodResults["channel"] = mapOf(
                            "timeNs" to channelTime,
                            "fileSize" to tempFile.length(),
                            "filePath" to tempFile.absolutePath
                        )
                    }
                    4 -> {
                        // Compressed write
                        val tempFile = createTempFile("ktor-compressed", ".gz")
                        tempFile.deleteOnExit()
                        val compressedTime = measureTime {
                            writeCompressedFile(tempFile, randomData)
                        }
                        methodResults["compressed"] = mapOf(
                            "timeNs" to compressedTime,
                            "fileSize" to tempFile.length(),
                            "filePath" to tempFile.absolutePath
                        )
                    }
                    else -> {
                        // All methods
                        // Standard write
                        val standardFile = createTempFile("ktor-standard", ".tmp")
                        standardFile.deleteOnExit()
                        val standardTime = measureTime {
                            writeStandardFile(standardFile, randomData)
                        }

                        // Buffered write
                        val bufferedFile = createTempFile("ktor-buffered", ".tmp")
                        bufferedFile.deleteOnExit()
                        val bufferedTime = measureTime {
                            writeBufferedFile(bufferedFile, randomData)
                        }

                        // Channel write
                        val channelFile = createTempFile("ktor-channel", ".tmp")
                        channelFile.deleteOnExit()
                        val channelTime = measureTime {
                            writeFileWithChannel(channelFile, randomData)
                        }

                        // Compressed write
                        val compressedFile = createTempFile("ktor-compressed", ".gz")
                        compressedFile.deleteOnExit()
                        val compressedTime = measureTime {
                            writeCompressedFile(compressedFile, randomData)
                        }

                        methodResults["standard"] = mapOf(
                            "timeNs" to standardTime,
                            "fileSize" to standardFile.length(),
                            "filePath" to standardFile.absolutePath
                        )
                        methodResults["buffered"] = mapOf(
                            "timeNs" to bufferedTime,
                            "fileSize" to bufferedFile.length(),
                            "filePath" to bufferedFile.absolutePath
                        )
                        methodResults["channel"] = mapOf(
                            "timeNs" to channelTime,
                            "fileSize" to channelFile.length(),
                            "filePath" to channelFile.absolutePath
                        )
                        methodResults["compressed"] = mapOf(
                            "timeNs" to compressedTime,
                            "fileSize" to compressedFile.length(),
                            "filePath" to compressedFile.absolutePath
                        )
                    }
                }

                val endTime = System.nanoTime()

                mapOf(
                    "requestedSizeKB" to sizeKB,
                    "totalTimeNs" to (endTime - startTime),
                    "methods" to methodResults
                )
            }

            call.respond(result)
        }

        /**
         * Enhanced file read endpoint.
         * Performs multiple file read operations using different methods.
         * Query parameters:
         * - size: The size of the file to read in KB (default: 1024)
         * - method: The read method to use (1=standard, 2=buffered, 3=channel, 4=compressed, default: all)
         */
        get("/read") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val sizeBytes = sizeKB * 1024
            val method = call.parameters["method"]?.toIntOrNull() ?: 0 // 0 means all methods

            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()

                // Create random data once to use for all methods
                val randomData = Random.nextBytes(sizeBytes)

                // Results for each method
                val methodResults = mutableMapOf<String, Any>()

                // Prepare files for each method
                val standardFile = createTempFile("ktor-read-standard", ".tmp")
                standardFile.deleteOnExit()
                writeStandardFile(standardFile, randomData)

                val bufferedFile = createTempFile("ktor-read-buffered", ".tmp")
                bufferedFile.deleteOnExit()
                writeBufferedFile(bufferedFile, randomData)

                val channelFile = createTempFile("ktor-read-channel", ".tmp")
                channelFile.deleteOnExit()
                writeFileWithChannel(channelFile, randomData)

                val compressedFile = createTempFile("ktor-read-compressed", ".gz")
                compressedFile.deleteOnExit()
                writeCompressedFile(compressedFile, randomData)

                // Perform file operations based on the selected method
                when (method) {
                    1 -> {
                        // Standard read
                        val standardTime = measureTime {
                            val content = readStandardFile(standardFile)
                        }
                        methodResults["standard"] = mapOf(
                            "timeNs" to standardTime,
                            "fileSize" to standardFile.length(),
                            "filePath" to standardFile.absolutePath
                        )
                    }
                    2 -> {
                        // Buffered read
                        val bufferedTime = measureTime {
                            val content = readBufferedFile(bufferedFile)
                        }
                        methodResults["buffered"] = mapOf(
                            "timeNs" to bufferedTime,
                            "fileSize" to bufferedFile.length(),
                            "filePath" to bufferedFile.absolutePath
                        )
                    }
                    3 -> {
                        // Channel read
                        val channelTime = measureTime {
                            val content = readFileWithChannel(channelFile)
                        }
                        methodResults["channel"] = mapOf(
                            "timeNs" to channelTime,
                            "fileSize" to channelFile.length(),
                            "filePath" to channelFile.absolutePath
                        )
                    }
                    4 -> {
                        // Compressed read
                        val compressedTime = measureTime {
                            val content = readCompressedFile(compressedFile)
                        }
                        methodResults["compressed"] = mapOf(
                            "timeNs" to compressedTime,
                            "fileSize" to compressedFile.length(),
                            "filePath" to compressedFile.absolutePath
                        )
                    }
                    else -> {
                        // All methods
                        // Standard read
                        val standardTime = measureTime {
                            val content = readStandardFile(standardFile)
                        }

                        // Buffered read
                        val bufferedTime = measureTime {
                            val content = readBufferedFile(bufferedFile)
                        }

                        // Channel read
                        val channelTime = measureTime {
                            val content = readFileWithChannel(channelFile)
                        }

                        // Compressed read
                        val compressedTime = measureTime {
                            val content = readCompressedFile(compressedFile)
                        }

                        methodResults["standard"] = mapOf(
                            "timeNs" to standardTime,
                            "fileSize" to standardFile.length(),
                            "filePath" to standardFile.absolutePath
                        )
                        methodResults["buffered"] = mapOf(
                            "timeNs" to bufferedTime,
                            "fileSize" to bufferedFile.length(),
                            "filePath" to bufferedFile.absolutePath
                        )
                        methodResults["channel"] = mapOf(
                            "timeNs" to channelTime,
                            "fileSize" to channelFile.length(),
                            "filePath" to channelFile.absolutePath
                        )
                        methodResults["compressed"] = mapOf(
                            "timeNs" to compressedTime,
                            "fileSize" to compressedFile.length(),
                            "filePath" to compressedFile.absolutePath
                        )
                    }
                }

                val endTime = System.nanoTime()

                mapOf(
                    "requestedSizeKB" to sizeKB,
                    "totalTimeNs" to (endTime - startTime),
                    "methods" to methodResults
                )
            }

            call.respond(result)
        }

        /**
         * Enhanced database simulation endpoint.
         * Simulates complex database operations to test I/O performance.
         * Query parameters:
         * - records: The number of records to process (default: 1000)
         * - delay: The simulated database latency in ms (default: 10)
         * - complexity: The complexity level of operations (1-5, default: 3)
         */
        get("/db") {
            val records = call.parameters["records"]?.toIntOrNull()?.coerceAtMost(50000) ?: 1000
            val dbLatencyMs = call.parameters["delay"]?.toLongOrNull()?.coerceAtMost(1000) ?: 10
            val complexity = call.parameters["complexity"]?.toIntOrNull()?.coerceIn(1, 5) ?: 3

            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()

                // Create simulated database tables
                val usersTable = createUsersTable(records / 10)
                val ordersTable = createOrdersTable(records, usersTable)
                val productsTable = createProductsTable(records / 5)
                val orderItemsTable = createOrderItemsTable(records * 3, ordersTable, productsTable)

                // Perform database operations with varying complexity
                val operationResults = mutableMapOf<String, Any>()

                // 1. Basic CRUD operations
                val crudTime = measureTime {
                    // Create
                    val newUser = DbUser(
                        id = usersTable.size + 1,
                        name = "New User ${Random.nextInt(1000)}",
                        email = "user${Random.nextInt(1000)}@example.com",
                        registrationDate = System.currentTimeMillis()
                    )
                    usersTable.add(newUser)

                    // Read
                    val user = usersTable.find { it.id == Random.nextInt(1, usersTable.size) }

                    // Update
                    if (user != null) {
                        val updatedUser = user.copy(name = "Updated ${user.name}")
                        usersTable[usersTable.indexOf(user)] = updatedUser
                    }

                    // Delete
                    usersTable.removeIf { it.id == usersTable.size }

                    // Simulate database latency
                    delay(dbLatencyMs)
                }
                operationResults["crud"] = mapOf("timeNs" to crudTime)

                if (complexity >= 2) {
                    // 2. Simple join operation
                    val joinTime = measureTime {
                        val userOrders = simulateJoinOperation(usersTable, ordersTable)
                        delay(dbLatencyMs)
                    }
                    operationResults["join"] = mapOf("timeNs" to joinTime)
                }

                if (complexity >= 3) {
                    // 3. Complex join with multiple tables
                    val complexJoinTime = measureTime {
                        val orderDetails = simulateComplexJoin(usersTable, ordersTable, orderItemsTable, productsTable)
                        delay(dbLatencyMs * 2)
                    }
                    operationResults["complexJoin"] = mapOf("timeNs" to complexJoinTime)
                }

                if (complexity >= 4) {
                    // 4. Aggregation operations
                    val aggregationTime = measureTime {
                        val userStats = simulateAggregation(usersTable, ordersTable, orderItemsTable)
                        delay(dbLatencyMs)
                    }
                    operationResults["aggregation"] = mapOf("timeNs" to aggregationTime)
                }

                if (complexity >= 5) {
                    // 5. Transaction simulation with multiple operations
                    val transactionTime = measureTime {
                        simulateTransaction(usersTable, ordersTable, orderItemsTable, productsTable)
                        delay(dbLatencyMs * 3)
                    }
                    operationResults["transaction"] = mapOf("timeNs" to transactionTime)
                }

                val endTime = System.nanoTime()

                mapOf(
                    "records" to records,
                    "simulatedDbLatencyMs" to dbLatencyMs,
                    "complexity" to complexity,
                    "totalTimeNs" to (endTime - startTime),
                    "operations" to operationResults,
                    "tableStats" to mapOf(
                        "users" to usersTable.size,
                        "orders" to ordersTable.size,
                        "products" to productsTable.size,
                        "orderItems" to orderItemsTable.size
                    ),
                    "sampleData" to mapOf(
                        "users" to usersTable.take(3),
                        "orders" to ordersTable.take(3),
                        "products" to productsTable.take(3)
                    )
                )
            }

            call.respond(result)
        }

        /**
         * Enhanced network simulation endpoint.
         * Simulates complex network operations to test I/O performance.
         * Query parameters:
         * - size: The size of the data to transfer in KB (default: 1024)
         * - delay: The simulated network latency in ms (default: 50)
         * - requests: The number of requests to simulate (default: 5)
         * - complexity: The complexity level of operations (1-5, default: 3)
         * - errorRate: The simulated error rate as a percentage (0-100, default: 10)
         */
        get("/network") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val networkLatencyMs = call.parameters["delay"]?.toLongOrNull()?.coerceAtMost(1000) ?: 50
            val requests = call.parameters["requests"]?.toIntOrNull()?.coerceIn(1, 50) ?: 5
            val complexity = call.parameters["complexity"]?.toIntOrNull()?.coerceIn(1, 5) ?: 3
            val errorRate = call.parameters["errorRate"]?.toIntOrNull()?.coerceIn(0, 100) ?: 10

            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()

                // Results for different operations
                val operationResults = mutableMapOf<String, Any>()

                // 1. Basic network request simulation
                val basicRequestTime = measureTime {
                    val data = simulateNetworkOperations(sizeKB, networkLatencyMs)
                    operationResults["basicRequestData"] = mapOf(
                        "sizeKB" to sizeKB,
                        "dataHashCode" to data.hashCode()
                    )
                }
                operationResults["basicRequest"] = mapOf("timeNs" to basicRequestTime)

                if (complexity >= 2) {
                    // 2. Multiple parallel requests
                    val parallelRequestsTime = measureTime {
                        val results = simulateParallelRequests(requests, sizeKB / requests, networkLatencyMs, errorRate)
                        operationResults["parallelRequestsData"] = mapOf(
                            "successfulRequests" to results.count { it.second },
                            "failedRequests" to results.count { !it.second },
                            "totalDataSizeKB" to results.sumOf { it.first.size } / 1024
                        )
                    }
                    operationResults["parallelRequests"] = mapOf("timeNs" to parallelRequestsTime)
                }

                if (complexity >= 3) {
                    // 3. Simulate different protocols (HTTP, HTTPS, FTP, etc.)
                    val protocolsTime = measureTime {
                        val protocolResults = simulateDifferentProtocols(sizeKB, networkLatencyMs, errorRate)
                        operationResults["protocolsData"] = protocolResults
                    }
                    operationResults["protocols"] = mapOf("timeNs" to protocolsTime)
                }

                if (complexity >= 4) {
                    // 4. Simulate network congestion and retries
                    val congestionTime = measureTime {
                        val congestionResults = simulateNetworkCongestion(sizeKB, networkLatencyMs, requests)
                        operationResults["congestionData"] = congestionResults
                    }
                    operationResults["congestion"] = mapOf("timeNs" to congestionTime)
                }

                if (complexity >= 5) {
                    // 5. Simulate complex data streaming
                    val streamingTime = measureTime {
                        val streamingResults = simulateDataStreaming(sizeKB * 2, networkLatencyMs)
                        operationResults["streamingData"] = streamingResults
                    }
                    operationResults["streaming"] = mapOf("timeNs" to streamingTime)
                }

                val endTime = System.nanoTime()

                mapOf(
                    "sizeKB" to sizeKB,
                    "simulatedNetworkLatencyMs" to networkLatencyMs,
                    "requests" to requests,
                    "complexity" to complexity,
                    "errorRate" to errorRate,
                    "totalTimeNs" to (endTime - startTime),
                    "operations" to operationResults
                )
            }

            call.respond(result)
        }

        /**
         * Combined IO operations endpoint.
         * Performs multiple IO-intensive operations in sequence and in parallel.
         * Query parameters:
         * - size: The base size for operations in KB (default: 1024)
         * - delay: The base delay for operations in ms (default: 20)
         * - parallel: Whether to run operations in parallel (default: true)
         */
        get("/combined") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val baseDelayMs = call.parameters["delay"]?.toLongOrNull()?.coerceAtMost(1000) ?: 20
            val parallel = call.parameters["parallel"]?.toBoolean() ?: true

            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()

                // Results for each operation
                val operationResults = mutableMapOf<String, Any>()

                if (parallel) {
                    // Run operations in parallel
                    val fileWriteJob = async {
                        val writeTime = measureTime {
                            val randomData = Random.nextBytes(sizeKB * 1024)
                            val tempFile = createTempFile("ktor-combined-write", ".tmp")
                            tempFile.deleteOnExit()
                            writeBufferedFile(tempFile, randomData)
                            operationResults["fileWritePath"] = tempFile.absolutePath
                        }
                        operationResults["fileWrite"] = mapOf("timeNs" to writeTime)
                    }

                    val fileReadJob = async {
                        val readTime = measureTime {
                            val tempFile = createTempFile("ktor-combined-read", ".tmp")
                            tempFile.deleteOnExit()
                            val randomData = Random.nextBytes(sizeKB * 1024)
                            writeStandardFile(tempFile, randomData)
                            val content = readBufferedFile(tempFile)
                            operationResults["fileReadSize"] = content.size
                        }
                        operationResults["fileRead"] = mapOf("timeNs" to readTime)
                    }

                    val dbJob = async {
                        val dbTime = measureTime {
                            val usersTable = createUsersTable(sizeKB / 100)
                            val ordersTable = createOrdersTable(sizeKB / 50, usersTable)
                            val userOrders = simulateJoinOperation(usersTable, ordersTable)
                            operationResults["dbSize"] = mapOf(
                                "users" to usersTable.size,
                                "orders" to ordersTable.size,
                                "joinResults" to userOrders.size
                            )
                        }
                        operationResults["database"] = mapOf("timeNs" to dbTime)
                    }

                    val networkJob = async {
                        val networkTime = measureTime {
                            val protocols = simulateDifferentProtocols(sizeKB / 4, baseDelayMs, 5)
                            operationResults["networkProtocols"] = protocols.keys.toList()
                        }
                        operationResults["network"] = mapOf("timeNs" to networkTime)
                    }

                    // Wait for all operations to complete
                    awaitAll(fileWriteJob, fileReadJob, dbJob, networkJob)
                } else {
                    // Run operations in sequence

                    // 1. File write
                    val writeTime = measureTime {
                        val randomData = Random.nextBytes(sizeKB * 1024)
                        val tempFile = createTempFile("ktor-combined-write", ".tmp")
                        tempFile.deleteOnExit()
                        writeBufferedFile(tempFile, randomData)
                        operationResults["fileWritePath"] = tempFile.absolutePath
                    }
                    operationResults["fileWrite"] = mapOf("timeNs" to writeTime)

                    // 2. File read
                    val readTime = measureTime {
                        val tempFile = createTempFile("ktor-combined-read", ".tmp")
                        tempFile.deleteOnExit()
                        val randomData = Random.nextBytes(sizeKB * 1024)
                        writeStandardFile(tempFile, randomData)
                        val content = readBufferedFile(tempFile)
                        operationResults["fileReadSize"] = content.size
                    }
                    operationResults["fileRead"] = mapOf("timeNs" to readTime)

                    // 3. Database operations
                    val dbTime = measureTime {
                        val usersTable = createUsersTable(sizeKB / 100)
                        val ordersTable = createOrdersTable(sizeKB / 50, usersTable)
                        val userOrders = simulateJoinOperation(usersTable, ordersTable)
                        operationResults["dbSize"] = mapOf(
                            "users" to usersTable.size,
                            "orders" to ordersTable.size,
                            "joinResults" to userOrders.size
                        )
                    }
                    operationResults["database"] = mapOf("timeNs" to dbTime)

                    // 4. Network operations
                    val networkTime = measureTime {
                        val protocols = simulateDifferentProtocols(sizeKB / 4, baseDelayMs, 5)
                        operationResults["networkProtocols"] = protocols.keys.toList()
                    }
                    operationResults["network"] = mapOf("timeNs" to networkTime)
                }

                val endTime = System.nanoTime()

                mapOf(
                    "sizeKB" to sizeKB,
                    "baseDelayMs" to baseDelayMs,
                    "parallel" to parallel,
                    "totalTimeNs" to (endTime - startTime),
                    "operations" to operationResults
                )
            }

            call.respond(result)
        }
    }
}

/**
 * Measures the execution time of a block of code in nanoseconds.
 */
private inline fun measureTime(block: () -> Unit): Long {
    val startTime = System.nanoTime()
    block()
    val endTime = System.nanoTime()
    return endTime - startTime
}

/**
 * Writes random data to a file using standard Java IO.
 */
private fun writeRandomDataToFile(file: File, sizeBytes: Int) {
    val buffer = ByteArray(8192) // 8KB buffer
    val randomData = Random.nextBytes(buffer)

    Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { outputStream ->
        var remaining = sizeBytes
        while (remaining > 0) {
            val bytesToWrite = minOf(buffer.size, remaining)
            outputStream.write(randomData, 0, bytesToWrite)
            remaining -= bytesToWrite
        }
        outputStream.flush()
    }
}

/**
 * Writes data to a file using standard Java IO.
 */
private fun writeStandardFile(file: File, data: ByteArray) {
    FileOutputStream(file).use { outputStream ->
        outputStream.write(data)
        outputStream.flush()
    }
}

/**
 * Writes data to a file using buffered IO.
 */
private fun writeBufferedFile(file: File, data: ByteArray) {
    BufferedOutputStream(FileOutputStream(file), 65536).use { outputStream ->
        outputStream.write(data)
        outputStream.flush()
    }
}

/**
 * Writes data to a file using NIO channels.
 */
private fun writeFileWithChannel(file: File, data: ByteArray) {
    FileOutputStream(file).channel.use { channel ->
        val buffer = ByteBuffer.wrap(data)
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
        channel.force(true)
    }
}

/**
 * Writes compressed data to a file using GZIP.
 */
private fun writeCompressedFile(file: File, data: ByteArray) {
    GZIPOutputStream(BufferedOutputStream(FileOutputStream(file), 65536)).use { outputStream ->
        outputStream.write(data)
        outputStream.flush()
    }
}

/**
 * Reads a file using standard Java IO.
 */
private fun readStandardFile(file: File): ByteArray {
    return FileInputStream(file).use { inputStream ->
        inputStream.readBytes()
    }
}

/**
 * Reads a file using buffered IO.
 */
private fun readBufferedFile(file: File): ByteArray {
    return BufferedInputStream(FileInputStream(file), 65536).use { inputStream ->
        inputStream.readBytes()
    }
}

/**
 * Reads a file using NIO channels.
 */
private fun readFileWithChannel(file: File): ByteArray {
    return FileInputStream(file).channel.use { channel ->
        val size = channel.size().toInt()
        val buffer = ByteBuffer.allocate(size)
        channel.read(buffer)
        buffer.flip()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        bytes
    }
}

/**
 * Reads a compressed file using GZIP.
 */
private fun readCompressedFile(file: File): ByteArray {
    return GZIPInputStream(BufferedInputStream(FileInputStream(file), 65536)).use { inputStream ->
        inputStream.readBytes()
    }
}

/**
 * Data classes for simulated database tables
 */
@Serializable
private data class DbUser(
    val id: Int,
    val name: String,
    val email: String,
    val registrationDate: Long
)

@Serializable
private data class DbOrder(
    val id: Int,
    val userId: Int,
    val totalAmount: Double,
    val status: String,
    val orderDate: Long
)

@Serializable
private data class DbProduct(
    val id: Int,
    val name: String,
    val price: Double,
    val category: String,
    val stockQuantity: Int
)

@Serializable
private data class DbOrderItem(
    val id: Int,
    val orderId: Int,
    val productId: Int,
    val quantity: Int,
    val price: Double
)

@Serializable
private data class DbUserOrderSummary(
    val userId: Int,
    val userName: String,
    val totalOrders: Int,
    val totalSpent: Double
)

/**
 * Legacy data classes for backward compatibility
 */
private data class DbRecord(
    val id: Int,
    val name: String,
    val value: Double,
    val timestamp: Long
)

@Serializable
data class DbRecordResponse(
    val id: Int,
    val name: String,
    val value: Double,
    val timestamp: Long
)

@Serializable
data class DbResponse(
    val records: Int,
    val simulatedDbLatencyMs: Long,
    val totalTimeNs: Long,
    val sampleRecords: List<DbRecordResponse>
)

/**
 * Creates a simulated users table.
 */
private fun createUsersTable(count: Int): MutableList<DbUser> {
    val users = mutableListOf<DbUser>()

    for (i in 1..count) {
        users.add(
            DbUser(
                id = i,
                name = "User-${Random.nextInt(0, 1000000)}",
                email = "user${i}@example.com",
                registrationDate = System.currentTimeMillis() - Random.nextLong(0, 30 * 24 * 60 * 60 * 1000)
            )
        )
    }

    return users
}

/**
 * Creates a simulated orders table.
 */
private fun createOrdersTable(count: Int, users: List<DbUser>): MutableList<DbOrder> {
    val orders = mutableListOf<DbOrder>()
    val statuses = listOf("pending", "processing", "shipped", "delivered", "cancelled")

    for (i in 1..count) {
        val userId = if (users.isNotEmpty()) users[Random.nextInt(users.size)].id else Random.nextInt(1, 1000)

        orders.add(
            DbOrder(
                id = i,
                userId = userId,
                totalAmount = Random.nextDouble(10.0, 1000.0),
                status = statuses[Random.nextInt(statuses.size)],
                orderDate = System.currentTimeMillis() - Random.nextLong(0, 30 * 24 * 60 * 60 * 1000)
            )
        )
    }

    return orders
}

/**
 * Creates a simulated products table.
 */
private fun createProductsTable(count: Int): MutableList<DbProduct> {
    val products = mutableListOf<DbProduct>()
    val categories = listOf("electronics", "clothing", "books", "home", "food", "toys")

    for (i in 1..count) {
        products.add(
            DbProduct(
                id = i,
                name = "Product-${Random.nextInt(0, 1000000)}",
                price = Random.nextDouble(1.0, 1000.0),
                category = categories[Random.nextInt(categories.size)],
                stockQuantity = Random.nextInt(0, 1000)
            )
        )
    }

    return products
}

/**
 * Creates a simulated order items table.
 */
private fun createOrderItemsTable(count: Int, orders: List<DbOrder>, products: List<DbProduct>): MutableList<DbOrderItem> {
    val orderItems = mutableListOf<DbOrderItem>()

    for (i in 1..count) {
        val orderId = if (orders.isNotEmpty()) orders[Random.nextInt(orders.size)].id else Random.nextInt(1, 1000)
        val productId = if (products.isNotEmpty()) products[Random.nextInt(products.size)].id else Random.nextInt(1, 1000)
        val product = products.find { it.id == productId }
        val price = product?.price ?: Random.nextDouble(1.0, 100.0)

        orderItems.add(
            DbOrderItem(
                id = i,
                orderId = orderId,
                productId = productId,
                quantity = Random.nextInt(1, 10),
                price = price
            )
        )
    }

    return orderItems
}

/**
 * Simulates a join operation between users and orders.
 */
private fun simulateJoinOperation(users: List<DbUser>, orders: List<DbOrder>): List<Pair<DbUser, List<DbOrder>>> {
    return users.map { user ->
        val userOrders = orders.filter { it.userId == user.id }
        Pair(user, userOrders)
    }
}

/**
 * Simulates a complex join operation across multiple tables.
 */
private fun simulateComplexJoin(
    users: List<DbUser>,
    orders: List<DbOrder>,
    orderItems: List<DbOrderItem>,
    products: List<DbProduct>
): List<Map<String, Any>> {
    val result = mutableListOf<Map<String, Any>>()

    // For each user, find their orders
    for (user in users.take(10)) { // Limit to 10 users for performance
        val userOrders = orders.filter { it.userId == user.id }

        // For each order, find the order items
        for (order in userOrders) {
            val items = orderItems.filter { it.orderId == order.id }

            // For each item, find the product details
            for (item in items) {
                val product = products.find { it.id == item.productId }

                if (product != null) {
                    result.add(
                        mapOf(
                            "user" to user,
                            "order" to order,
                            "item" to item,
                            "product" to product,
                            "totalPrice" to (item.quantity * item.price)
                        )
                    )
                }
            }
        }
    }

    return result
}

/**
 * Simulates aggregation operations.
 */
private fun simulateAggregation(
    users: List<DbUser>,
    orders: List<DbOrder>,
    orderItems: List<DbOrderItem>
): List<DbUserOrderSummary> {
    return users.map { user ->
        val userOrders = orders.filter { it.userId == user.id }
        val orderIds = userOrders.map { it.id }

        // Calculate total spent across all orders
        var totalSpent = 0.0
        for (orderId in orderIds) {
            val items = orderItems.filter { it.orderId == orderId }
            for (item in items) {
                totalSpent += item.price * item.quantity
            }
        }

        DbUserOrderSummary(
            userId = user.id,
            userName = user.name,
            totalOrders = userOrders.size,
            totalSpent = totalSpent
        )
    }
}

/**
 * Simulates a database transaction with multiple operations.
 */
private fun simulateTransaction(
    users: MutableList<DbUser>,
    orders: MutableList<DbOrder>,
    orderItems: MutableList<DbOrderItem>,
    products: MutableList<DbProduct>
) {
    // 1. Create a new user
    val newUser = DbUser(
        id = users.size + 1,
        name = "Transaction User ${Random.nextInt(1000)}",
        email = "transaction${Random.nextInt(1000)}@example.com",
        registrationDate = System.currentTimeMillis()
    )
    users.add(newUser)

    // 2. Create a new order for the user
    val newOrder = DbOrder(
        id = orders.size + 1,
        userId = newUser.id,
        totalAmount = 0.0, // Will be calculated based on items
        status = "pending",
        orderDate = System.currentTimeMillis()
    )
    orders.add(newOrder)

    // 3. Add items to the order
    var orderTotal = 0.0
    for (i in 1..3) {
        val product = products[Random.nextInt(products.size)]
        val quantity = Random.nextInt(1, 5)

        val orderItem = DbOrderItem(
            id = orderItems.size + i,
            orderId = newOrder.id,
            productId = product.id,
            quantity = quantity,
            price = product.price
        )
        orderItems.add(orderItem)

        // 4. Update product stock
        val updatedProduct = product.copy(stockQuantity = product.stockQuantity - quantity)
        products[products.indexOf(product)] = updatedProduct

        // Calculate item total
        orderTotal += orderItem.price * orderItem.quantity
    }

    // 5. Update the order with the calculated total
    val updatedOrder = newOrder.copy(totalAmount = orderTotal)
    orders[orders.indexOf(newOrder)] = updatedOrder
}

/**
 * Legacy function for backward compatibility.
 * Simulates database operations.
 */
private suspend fun simulateDatabaseOperations(count: Int, latencyMs: Long): List<DbRecord> {
    val records = mutableListOf<DbRecord>()

    for (i in 0 until count) {
        // Simulate database latency
        delay(latencyMs)

        // Create a record
        records.add(
            DbRecord(
                id = i,
                name = "Record-${Random.nextInt(0, 1000000)}",
                value = Random.nextDouble(0.0, 1000.0),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    return records
}

/**
 * Simulates basic network operations.
 */
private suspend fun simulateNetworkOperations(sizeKB: Int, latencyMs: Long): ByteArray {
    // Simulate network latency
    delay(latencyMs)

    // Create random data
    val sizeBytes = sizeKB * 1024
    return Random.nextBytes(sizeBytes)
}

/**
 * Simulates multiple parallel network requests.
 * Returns a list of pairs containing the response data and a boolean indicating success.
 */
private suspend fun simulateParallelRequests(
    requestCount: Int,
    sizeKB: Int,
    latencyMs: Long,
    errorRate: Int
): List<Pair<ByteArray, Boolean>> = coroutineScope {
    val requests = List(requestCount) { requestId ->
        async {
            try {
                // Simulate variable latency for each request
                val requestLatency = latencyMs + Random.nextLong(-latencyMs / 5, latencyMs / 5).coerceAtLeast(1)
                delay(requestLatency)

                // Simulate random errors based on error rate
                if (Random.nextInt(100) < errorRate) {
                    throw SimulatedNetworkException("Simulated network error for request $requestId")
                }

                // Create response data
                val responseData = Random.nextBytes(sizeKB * 1024)

                // Return successful response
                Pair(responseData, true)
            } catch (e: Exception) {
                // Return empty data with failure flag
                Pair(ByteArray(0), false)
            }
        }
    }

    requests.awaitAll()
}

/**
 * Simulates different network protocols.
 */
private suspend fun simulateDifferentProtocols(
    sizeKB: Int,
    latencyMs: Long,
    errorRate: Int
): Map<String, Any> {
    val protocols = listOf("HTTP", "HTTPS", "FTP", "SMTP", "WebSocket")
    val results = mutableMapOf<String, Any>()

    for (protocol in protocols) {
        try {
            // Different protocols have different latencies and overhead
            val protocolLatency = when (protocol) {
                "HTTP" -> latencyMs
                "HTTPS" -> latencyMs * 1.2 // SSL handshake adds overhead
                "FTP" -> latencyMs * 1.5 // Control connection + data connection
                "SMTP" -> latencyMs * 1.3 // SMTP handshake
                "WebSocket" -> latencyMs * 0.8 // After initial handshake, it's faster
                else -> latencyMs
            }

            // Simulate protocol-specific latency
            delay(protocolLatency.toLong())

            // Simulate random errors based on error rate
            if (Random.nextInt(100) < errorRate) {
                throw SimulatedNetworkException("Simulated $protocol error")
            }

            // Create response data
            val responseData = Random.nextBytes((sizeKB * 1024 * 0.8).toInt()) // Assume some protocol overhead

            results[protocol] = mapOf(
                "status" to "success",
                "latencyMs" to protocolLatency,
                "dataSize" to responseData.size,
                "dataHashCode" to responseData.hashCode()
            )
        } catch (e: Exception) {
            results[protocol] = mapOf(
                "status" to "error",
                "error" to e.message
            )
        }
    }

    return results
}

/**
 * Simulates network congestion and retries.
 */
private suspend fun simulateNetworkCongestion(
    sizeKB: Int,
    baseLatencyMs: Long,
    requestCount: Int
): Map<String, Any> {
    val results = mutableMapOf<String, Any>()
    var successfulRequests = 0
    var totalRetries = 0

    for (i in 1..requestCount) {
        var retries = 0
        var success = false

        while (retries < 3 && !success) {
            try {
                // Simulate increasing congestion with each request
                val congestionFactor = 1.0 + (i.toDouble() / requestCount)

                // Calculate latency with congestion and jitter
                val jitter = Random.nextLong(-baseLatencyMs / 10, baseLatencyMs / 5)
                val actualLatency = (baseLatencyMs * congestionFactor).toLong() + jitter

                // Simulate the request with congestion
                delay(actualLatency)

                // Simulate packet loss under congestion
                if (Random.nextInt(100) < (10 * congestionFactor).toInt()) {
                    throw SimulatedNetworkException("Packet loss due to congestion")
                }

                // Request succeeded
                success = true
                successfulRequests++

                // Record metrics for this request
                results["request_$i"] = mapOf(
                    "status" to "success",
                    "retries" to retries,
                    "congestionFactor" to congestionFactor,
                    "actualLatencyMs" to actualLatency
                )
            } catch (e: Exception) {
                retries++
                totalRetries++

                if (retries >= 3) {
                    // Request failed after max retries
                    results["request_$i"] = mapOf(
                        "status" to "failed",
                        "retries" to retries,
                        "error" to e.message
                    )
                }

                // Exponential backoff before retry
                if (retries < 3) {
                    delay(baseLatencyMs * (1 shl retries))
                }
            }
        }
    }

    return mapOf(
        "summary" to mapOf(
            "successfulRequests" to successfulRequests,
            "failedRequests" to (requestCount - successfulRequests),
            "totalRetries" to totalRetries
        ),
        "requests" to results
    )
}

/**
 * Simulates data streaming with chunks.
 */
private suspend fun simulateDataStreaming(
    sizeKB: Int,
    latencyMs: Long
): Map<String, Any> {
    val chunkSize = 64 * 1024 // 64KB chunks
    val totalBytes = sizeKB * 1024
    val chunkCount = (totalBytes + chunkSize - 1) / chunkSize // Ceiling division

    val chunks = mutableListOf<Map<String, Any>>()
    var bytesTransferred = 0
    var totalLatency = 0L

    for (i in 0 until chunkCount) {
        // Calculate the size of this chunk
        val currentChunkSize = minOf(chunkSize, totalBytes - bytesTransferred)

        // Generate chunk data
        val chunkData = Random.nextBytes(currentChunkSize)

        // Simulate variable latency for each chunk
        val chunkLatency = latencyMs / 2 + Random.nextLong(0, latencyMs)
        delay(chunkLatency)
        totalLatency += chunkLatency

        // Process the chunk
        bytesTransferred += currentChunkSize

        // Record metrics for this chunk
        chunks.add(
            mapOf(
                "chunkIndex" to i,
                "chunkSize" to currentChunkSize,
                "latencyMs" to chunkLatency,
                "bytesTransferred" to bytesTransferred,
                "progress" to (bytesTransferred.toDouble() / totalBytes * 100).toInt()
            )
        )
    }

    return mapOf(
        "totalSizeKB" to sizeKB,
        "chunkCount" to chunkCount,
        "totalLatencyMs" to totalLatency,
        "averageChunkLatencyMs" to (totalLatency.toDouble() / chunkCount).toLong(),
        "chunks" to chunks
    )
}

/**
 * Custom exception for simulated network errors.
 */
private class SimulatedNetworkException(message: String) : Exception(message)
