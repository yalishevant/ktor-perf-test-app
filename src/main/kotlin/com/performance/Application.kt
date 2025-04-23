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
    // Start the server with configuration from application.conf
    embeddedServer(
        Netty, 
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("com.performance.ApplicationKt")

            // Load configuration from application.conf
            config = ApplicationConfig("application.conf")

            // Configure the development mode
            developmentMode = config.property("ktor.development").getString().toBoolean()

            // Configure the module
            module {
                module()
            }

            // Configure the connector
            connector {
                port = config.property("ktor.deployment.port").getString().toInt()
                host = config.property("ktor.deployment.host").getString()
            }
        }
    ).start(wait = true)
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
