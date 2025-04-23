package com.performance.routes

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Extension function to register configurable routes.
 * These routes are designed to test performance with configurable response sizes and processing delays.
 */
fun Routing.configurableRoutes() {
    // Get configuration values
    val config = application.environment.config
    val defaultResponseSize = config.propertyOrNull("performance.defaultResponseSize")?.getString()?.toInt() ?: 1024
    val maxResponseSize = config.propertyOrNull("performance.maxResponseSize")?.getString()?.toInt() ?: 10485760 // 10MB
    val defaultDelay = config.propertyOrNull("performance.defaultDelay")?.getString()?.toLong() ?: 0
    val maxDelay = config.propertyOrNull("performance.maxDelay")?.getString()?.toLong() ?: 10000 // 10 seconds

    route("/configurable") {
        /**
         * Variable response size endpoint.
         * Returns a response of the specified size.
         * Query parameters:
         * - size: The size of the response in bytes (default: from config)
         */
        get("/size") {
            val size = call.parameters["size"]?.toIntOrNull()?.coerceIn(0, maxResponseSize) ?: defaultResponseSize

            // Generate random data of the specified size
            val data = generateRandomData(size)

            call.respond(mapOf(
                "size" to size,
                "data" to data
            ))
        }

        /**
         * Variable delay endpoint.
         * Responds after the specified delay.
         * Query parameters:
         * - delay: The delay in milliseconds (default: from config)
         */
        get("/delay") {
            val delayMs = call.parameters["delay"]?.toLongOrNull()?.coerceIn(0, maxDelay) ?: defaultDelay

            // Delay the response
            delay(delayMs)

            call.respond(mapOf(
                "delay" to delayMs,
                "timestamp" to System.currentTimeMillis()
            ))
        }

        /**
         * Combined endpoint with both variable size and delay.
         * Query parameters:
         * - size: The size of the response in bytes (default: from config)
         * - delay: The delay in milliseconds (default: from config)
         */
        get("/combined") {
            val size = call.parameters["size"]?.toIntOrNull()?.coerceIn(0, maxResponseSize) ?: defaultResponseSize
            val delayMs = call.parameters["delay"]?.toLongOrNull()?.coerceIn(0, maxDelay) ?: defaultDelay

            // Delay the response
            delay(delayMs)

            // Generate random data of the specified size
            val data = generateRandomData(size)

            call.respond(mapOf(
                "size" to size,
                "delay" to delayMs,
                "timestamp" to System.currentTimeMillis(),
                "data" to data
            ))
        }

        /**
         * Echo endpoint that returns the request parameters.
         * This is useful for testing request/response handling without much processing.
         */
        get("/echo") {
            // Convert query parameters to a map manually
            val paramsMap = call.request.queryParameters.entries().associate { it.key to it.value }

            call.respond(mapOf(
                "parameters" to paramsMap,
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
}

/**
 * Generates a random string of the specified size.
 */
private fun generateRandomData(size: Int): String {
    if (size <= 0) return ""

    // For very large sizes, use a more efficient approach
    if (size > 1000000) {
        // Return a placeholder with the size information
        return "Random data of size $size bytes (actual data not included in response to save memory)"
    }

    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val sb = StringBuilder(size)

    repeat(size) {
        sb.append(chars[Random.nextInt(chars.length)])
    }

    return sb.toString()
}
