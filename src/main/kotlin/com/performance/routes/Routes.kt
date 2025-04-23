package com.performance.routes

import io.ktor.server.routing.*

/**
 * This file re-exports all route extension functions for easier import in the main application.
 */

/**
 * Extension function to register all routes.
 * This is a convenience function that registers all route extension functions.
 */
fun Routing.registerAllRoutes() {
    cpuIntensiveRoutes()
    memoryIntensiveRoutes()
    ioIntensiveRoutes()
    configurableRoutes()
}