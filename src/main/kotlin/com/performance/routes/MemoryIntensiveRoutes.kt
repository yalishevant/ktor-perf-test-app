package com.performance.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Extension function to register memory-intensive routes.
 * These routes are designed to test memory performance by allocating objects and manipulating collections.
 */
fun Routing.memoryIntensiveRoutes() {
    route("/memory") {
        /**
         * Object allocation endpoint.
         * Creates a large number of objects to test memory allocation performance.
         * Query parameters:
         * - count: The number of objects to create (default: 1000000)
         */
        get("/allocate") {
            val count = call.parameters["count"]?.toIntOrNull()?.coerceAtMost(10000000) ?: 1000000
            
            // Use Dispatchers.Default for memory-intensive operations
            val result = withContext(Dispatchers.Default) {
                val startTime = System.nanoTime()
                val objects = allocateObjects(count)
                val endTime = System.nanoTime()
                
                mapOf(
                    "count" to count,
                    "allocationTimeNs" to (endTime - startTime),
                    "objectSize" to objects.first().estimatedSize()
                )
            }
            
            call.respond(result)
        }
        
        /**
         * Collection manipulation endpoint.
         * Creates and manipulates large collections to test memory performance.
         * Query parameters:
         * - size: The size of the collections to create (default: 1000000)
         */
        get("/collections") {
            val size = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10000000) ?: 1000000
            
            // Use Dispatchers.Default for memory-intensive operations
            val result = withContext(Dispatchers.Default) {
                val startTime = System.nanoTime()
                
                // Create a large list
                val list = List(size) { Random.nextInt(0, 1000000) }
                
                // Perform operations on the list
                val sorted = list.sorted()
                val filtered = list.filter { it % 2 == 0 }
                val mapped = list.map { it * 2 }
                
                val endTime = System.nanoTime()
                
                mapOf(
                    "size" to size,
                    "processingTimeNs" to (endTime - startTime),
                    "sortedSample" to sorted.take(5),
                    "filteredCount" to filtered.size,
                    "mappedSample" to mapped.take(5)
                )
            }
            
            call.respond(result)
        }
        
        /**
         * String manipulation endpoint.
         * Creates and manipulates large strings to test memory performance.
         * Query parameters:
         * - size: The size of the string to create (default: 1000000)
         */
        get("/strings") {
            val size = call.parameters["size"]?.toIntOrNull()?.coerceAtMost(10000000) ?: 1000000
            
            // Use Dispatchers.Default for memory-intensive operations
            val result = withContext(Dispatchers.Default) {
                val startTime = System.nanoTime()
                
                // Create a large string
                val sb = StringBuilder(size)
                for (i in 0 until size) {
                    sb.append(('a' + i % 26).toChar())
                }
                val str = sb.toString()
                
                // Perform operations on the string
                val reversed = str.reversed()
                val substring = str.substring(0, minOf(100, str.length))
                val replaced = str.replace('a', 'z')
                
                val endTime = System.nanoTime()
                
                mapOf(
                    "size" to size,
                    "processingTimeNs" to (endTime - startTime),
                    "substringPreview" to substring,
                    "reversedPreview" to reversed.take(100)
                )
            }
            
            call.respond(result)
        }
    }
}

/**
 * Data class representing a test object with random data.
 */
private data class TestObject(
    val id: Int,
    val name: String,
    val values: List<Double>
) {
    /**
     * Estimates the size of this object in bytes.
     * This is a rough estimate and not exact.
     */
    fun estimatedSize(): Int {
        // Rough estimate: 4 bytes for id, 2 bytes per char in name, 8 bytes per double in values
        return 4 + (name.length * 2) + (values.size * 8)
    }
}

/**
 * Allocates a specified number of test objects.
 */
private fun allocateObjects(count: Int): List<TestObject> {
    return List(count) { i ->
        TestObject(
            id = i,
            name = "Object-${Random.nextInt(0, 1000000)}",
            values = List(10) { Random.nextDouble(0.0, 1000.0) }
        )
    }
}