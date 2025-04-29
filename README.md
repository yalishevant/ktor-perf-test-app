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
GET /cpu/recursive?depth=30
GET /cpu/parallel?iterations=1000000&threads=4
```

#### Memory-Intensive Endpoints

```
GET /memory/allocate?count=1000000
GET /memory/collections?size=1000000
GET /memory/strings?size=1000000
```

#### I/O-Intensive Endpoints

```
GET /io/write?size=1024&method=0
GET /io/read?size=1024&method=0
GET /io/db?records=1000&delay=10&complexity=3
GET /io/network?size=1024&delay=50&requests=5&complexity=3&errorRate=10
GET /io/combined?size=1024&delay=20&parallel=true
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
# Test the health endpoint with 10 threads and 100 connections for 10 seconds
wrk -t10 -c100 -d10s http://localhost:8080/health

# Test CPU-intensive endpoints
wrk -t10 -c500 -d30s 'http://localhost:8080/cpu/primes?limit=100000'
wrk -t10 -c500 -d30s 'http://localhost:8080/cpu/sort?size=1000'
wrk -t10 -c500 -d30s 'http://localhost:8080/cpu/matrix?size=200'
wrk -t10 -c500 -d30s 'http://localhost:8080/cpu/recursive?depth=25'
wrk -t10 -c500 -d30s 'http://localhost:8080/cpu/parallel?iterations=1000000&threads=4'

# Test memory-intensive endpoints
wrk -t10 -c100 -d30s --latency 'http://localhost:8080/memory/allocate?count=100000'
wrk -t10 -c200 -d5m --latency 'http://localhost:8080/memory/collections?size=10000'
wrk -t10 -c100 -d30s 'http://localhost:8080/memory/strings?size=1000000'

# Test I/O-intensive endpoints
wrk -t10 -c100 -d30s 'http://localhost:8080/io/write?size=1024&method=0'
wrk -t10 -c100 -d30s 'http://localhost:8080/io/read?size=1024&method=0'
wrk -t10 -c100 -d30s 'http://localhost:8080/io/db?records=1000&delay=10&complexity=3'
wrk -t10 -c100 -d30s 'http://localhost:8080/io/network?size=1024&delay=50&requests=5&complexity=3&errorRate=10'
wrk -t10 -c100 -d30s 'http://localhost:8080/io/combined?size=1024&delay=20&parallel=true'

# Test configurable endpoints
wrk -t10 -c100 -d30s 'http://localhost:8080/configurable/combined?size=1024&delay=10'
```

### Comparing JDK Performance

To compare the performance between JDKs:

1. Run the same test with both JDKs
2. Focus on CPU-intensive and I/O-intensive endpoints, as they show the most significant differences
3. Compare throughput (requests/second) and latency (response time)
4. The enhanced complexity of these endpoints will better highlight the performance differences between JDKs

Example comparison workflow:

```bash
# Run at machine with JDK-1
java -jar build/libs/ktor-performance-app.jar
# In another terminal
wrk -t10 -c100 -d30s 'http://localhost:8080/cpu/parallel?iterations=5000000&threads=8'

# Run at machine with JDK-2
/path/to/JDK-2/bin/java -jar build/libs/ktor-performance-app.jar
# In another terminal
wrk -t10 -c100 -d30s 'http://localhost:8080/cpu/parallel?iterations=5000000&threads=8'

# Compare the results
```

## Deployment to AWS EC2

To deploy the application to AWS EC2:

1. Build the fat JAR using `./gradlew shadowJar`
2. Upload the JAR to your EC2 instance
3. Run the JAR on the EC2 instance using `java -jar ktor-performance-app.jar`

## License

This project is licensed under the MIT License - see the LICENSE file for details.
