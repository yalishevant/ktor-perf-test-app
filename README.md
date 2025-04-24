# Ktor Performance Testing Application

A Ktor-based web application designed for performance testing of JDKs. This application provides various endpoints to test different aspects of performance, including CPU-intensive operations, memory-intensive operations, I/O-intensive operations, and configurable response sizes and delays.

## Features

- **CPU-intensive endpoints**: Prime number calculation, sorting, matrix multiplication
- **Memory-intensive endpoints**: Object allocation, collection manipulation, string manipulation
- **I/O-intensive endpoints**: File operations, simulated database operations, simulated network operations
- **Configurable endpoints**: Variable response sizes, variable delays
- **Metrics**: Prometheus metrics for monitoring application performance
- **Optimized for performance**: Uses Kotlin coroutines for handling high concurrency

## Requirements

- JDK 11 or later
- Gradle 7.6 or later

## Building the Application

### Building with Gradle

```bash
./gradlew build
```

### Creating a Fat JAR

```bash
./gradlew shadowJar
```

The fat JAR will be created in `build/libs/ktor-performance-app.jar`.

## Running the Application

### Running with Gradle

```bash
./gradlew run
```

### Running the Fat JAR

```bash
java -jar build/libs/ktor-performance-app.jar
```

### Configuration

The application is configured using the `application.conf` file in the `src/main/resources` directory. You can override configuration values by setting environment variables or system properties.

## Testing the Application

### Available Endpoints

#### Health Check

```
GET /health
```

Returns "OK" if the application is running.

#### Metrics

```
GET /metrics
```

Returns Prometheus metrics for monitoring application performance.

#### CPU-Intensive Endpoints

```
GET /cpu/primes?limit=100000
GET /cpu/sort?size=100000
GET /cpu/matrix?size=200
```

#### Memory-Intensive Endpoints

```
GET /memory/allocate?count=1000000
GET /memory/collections?size=1000000
GET /memory/strings?size=1000000
```

#### I/O-Intensive Endpoints

```
GET /io/write?size=1024
GET /io/read?size=1024
GET /io/db?records=1000&delay=10
GET /io/network?size=1024&delay=50
```

#### Configurable Endpoints

```
GET /configurable/size?size=1024
GET /configurable/delay?delay=100
GET /configurable/combined?size=1024&delay=100
GET /configurable/echo?param1=value1&param2=value2
```

### Performance Testing with wrk

You can use the [wrk](https://github.com/wg/wrk) tool to test the performance of the application:

```bash
# Test the health endpoint with 10 threads and 100 connections for 30 seconds
wrk -t10 -c100 -d60s http://35.180.91.184:8080/health

# Test a CPU-intensive endpoint
wrk -t10 -c100 -d60s 'http://35.180.91.184:8080/cpu/primes?limit=10000'

# Test a memory-intensive endpoint
wrk -t10 -c100 -d60s 'http://35.180.91.184:8080/memory/allocate?count=10000'

# Test an I/O-intensive endpoint
wrk -t10 -c100 -d60s 'http://35.180.91.184:8080/io/db?records=100&delay=5'

# Test a configurable endpoint
wrk -t10 -c100 -d60s 'http://35.180.91.184:8080/configurable/combined?size=1024&delay=10'
```

## Deployment to AWS EC2

To deploy the application to AWS EC2:

1. Build the fat JAR using `./gradlew shadowJar`
2. Upload the JAR to your EC2 instance
3. Run the JAR on the EC2 instance using `java -jar ktor-performance-app.jar`

## License

This project is licensed under the MIT License - see the LICENSE file for details.
