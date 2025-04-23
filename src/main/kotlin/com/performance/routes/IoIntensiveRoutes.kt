package com.performance.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random

/**
 * Extension function to register I/O-intensive routes.
 * These routes are designed to test I/O performance by performing file operations and simulating database operations.
 */
fun Routing.ioIntensiveRoutes() {
    route("/io") {
        /**
         * File write endpoint.
         * Writes a file of the specified size to test file I/O performance.
         * Query parameters:
         * - size: The size of the file to write in KB (default: 1024)
         */
        get("/write") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val sizeBytes = sizeKB * 1024
            
            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val tempFile = createTempFile("ktor-perf-test", ".tmp")
                tempFile.deleteOnExit()
                
                val startTime = System.nanoTime()
                writeRandomDataToFile(tempFile, sizeBytes)
                val endTime = System.nanoTime()
                
                val fileSize = tempFile.length()
                
                mapOf(
                    "requestedSizeKB" to sizeKB,
                    "actualSizeBytes" to fileSize,
                    "writeTimeNs" to (endTime - startTime),
                    "filePath" to tempFile.absolutePath
                )
            }
            
            call.respond(result)
        }
        
        /**
         * File read endpoint.
         * Reads a file of the specified size to test file I/O performance.
         * Query parameters:
         * - size: The size of the file to read in KB (default: 1024)
         */
        get("/read") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val sizeBytes = sizeKB * 1024
            
            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val tempFile = createTempFile("ktor-perf-test", ".tmp")
                tempFile.deleteOnExit()
                
                // First write the file
                writeRandomDataToFile(tempFile, sizeBytes)
                
                // Then read it
                val startTime = System.nanoTime()
                val content = tempFile.readBytes()
                val endTime = System.nanoTime()
                
                mapOf(
                    "sizeKB" to sizeKB,
                    "bytesRead" to content.size,
                    "readTimeNs" to (endTime - startTime),
                    "filePath" to tempFile.absolutePath
                )
            }
            
            call.respond(result)
        }
        
        /**
         * Database simulation endpoint.
         * Simulates database operations to test I/O performance.
         * Query parameters:
         * - records: The number of records to process (default: 1000)
         * - delay: The simulated database latency in ms (default: 10)
         */
        get("/db") {
            val records = call.parameters["records"]?.toIntOrNull()?.coerceAtMost(10000) ?: 1000
            val dbLatencyMs = call.parameters["delay"]?.toLongOrNull()?.coerceAtMost(1000) ?: 10
            
            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()
                
                // Simulate database operations
                val dbRecords = simulateDatabaseOperations(records, dbLatencyMs)
                
                val endTime = System.nanoTime()
                
                mapOf(
                    "records" to records,
                    "simulatedDbLatencyMs" to dbLatencyMs,
                    "totalTimeNs" to (endTime - startTime),
                    "sampleRecords" to dbRecords.take(5)
                )
            }
            
            call.respond(result)
        }
        
        /**
         * Network simulation endpoint.
         * Simulates network operations to test I/O performance.
         * Query parameters:
         * - size: The size of the data to transfer in KB (default: 1024)
         * - delay: The simulated network latency in ms (default: 50)
         */
        get("/network") {
            val sizeKB = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10240) ?: 1024
            val networkLatencyMs = call.parameters["delay"]?.toLongOrNull()?.coerceAtMost(1000) ?: 50
            
            // Use Dispatchers.IO for I/O-bound operations
            val result = withContext(Dispatchers.IO) {
                val startTime = System.nanoTime()
                
                // Simulate network operations
                val data = simulateNetworkOperations(sizeKB, networkLatencyMs)
                
                val endTime = System.nanoTime()
                
                mapOf(
                    "sizeKB" to sizeKB,
                    "simulatedNetworkLatencyMs" to networkLatencyMs,
                    "totalTimeNs" to (endTime - startTime),
                    "dataHashCode" to data.hashCode()
                )
            }
            
            call.respond(result)
        }
    }
}

/**
 * Writes random data to a file.
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
 * Data class representing a simulated database record.
 */
private data class DbRecord(
    val id: Int,
    val name: String,
    val value: Double,
    val timestamp: Long
)

/**
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
 * Simulates network operations.
 */
private suspend fun simulateNetworkOperations(sizeKB: Int, latencyMs: Long): ByteArray {
    // Simulate network latency
    delay(latencyMs)
    
    // Create random data
    val sizeBytes = sizeKB * 1024
    return Random.nextBytes(sizeBytes)
}