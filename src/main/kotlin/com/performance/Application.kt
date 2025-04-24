package com.performance

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import com.performance.routes.*
import java.net.ServerSocket
import java.net.SocketException

/**
 * Main application entry point.
 * This application is designed for performance testing of JDKs.
 * It provides various endpoints to test different aspects of performance:
 * - CPU-intensive operations
 * - Memory-intensive operations
 * - I/O-intensive operations
 * - Configurable response size and delay
 */
fun main() {
    val defaultPort = 8080
    val maxPortAttempts = 10
    val logger = LoggerFactory.getLogger("Application")

    // Find an available port starting from the default port
    val port = findAvailablePort(defaultPort, maxPortAttempts)

    // Log the port that will be used
    if (port != defaultPort) {
        logger.info("Default port $defaultPort was not available. Using port $port instead.")
    } else {
        logger.info("Starting server on port $port")
    }

    // Start the server with configuration from application.conf
    embeddedServer(
        Netty, 
        port = port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

/**
 * Finds an available port starting from the specified port.
 * If the specified port is not available, it will try the next ports up to maxAttempts.
 *
 * @param startPort The port to start checking from
 * @param maxAttempts Maximum number of ports to check
 * @return An available port or the original port if no available port is found
 */
private fun findAvailablePort(startPort: Int, maxAttempts: Int): Int {
    val logger = LoggerFactory.getLogger("PortFinder")

    for (portOffset in 0 until maxAttempts) {
        val port = startPort + portOffset
        try {
            ServerSocket(port).use {
                logger.info("Port $port is available")
                return port
            }
        } catch (e: java.net.BindException) {
            logger.info("Port $port is already in use, trying next port")
        } catch (e: SocketException) {
            logger.info("Socket error on port $port: ${e.message}, trying next port")
        } catch (e: Exception) {
            logger.warn("Unexpected error checking port $port: ${e.message}, trying next port")
        }
    }

    logger.warn("Could not find an available port after $maxAttempts attempts, using default port $startPort")
    return startPort
}

/**
 * Application module configuration.
 * This function configures all the plugins and routes for the application.
 */
fun Application.module() {
    // Create Prometheus registry for metrics
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // Install plugins
    install(CallLogging) {
        level = Level.INFO
        // Log all requests
        filter { _ -> true }
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        // Configure metrics
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .maximumExpectedValue(TimeUnit.SECONDS.toNanos(10).toDouble())
            .build()
    }

    // Configure routing
    routing {
        // Health check endpoint
        get("/health") {
            call.respondText("OK")
        }

        // Metrics endpoint
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }

        // Register all routes
        registerAllRoutes()
    }

    // Log application startup
    log.info("Application started")
}
