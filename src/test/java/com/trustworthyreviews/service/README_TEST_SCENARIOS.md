# Hystrix Circuit Breaker Test Scenarios

This document describes the test scenarios for Hystrix circuit breaker functionality.

## Test Files

1. **HystrixUserServiceTest.java** - Unit tests for user service circuit breakers
2. **HystrixProductServiceTest.java** - Unit tests for product service circuit breakers
3. **CircuitBreakerScenarioTest.java** - Advanced scenarios (concurrency, mixed failures, etc.)
4. **CircuitBreakerIntegrationTest.java** - Integration tests (requires database)

## Test Scenarios Overview

### Basic Functionality Tests

#### ✅ Normal Operation
- **Scenario 1: Successful database query**
  - Service returns expected data
  - Circuit breaker allows request through
  - No fallback triggered

#### ❌ Failure Handling
- **Scenario 2: Database connection failure**
  - Service throws exception
  - Circuit breaker catches exception
  - Returns fallback (empty list or null)
  - No exception propagated to caller

#### ⏱️ Timeout Scenarios
- **Scenario 3: Slow database response**
  - Request exceeds 3-second timeout
  - Circuit breaker triggers timeout
  - Returns fallback immediately
  - Prevents thread pool exhaustion

### Advanced Scenarios

#### 🔄 Circuit Opening
- **Scenario 4: Multiple failures trigger circuit**
  - After 10 requests with 50% failure rate
  - Circuit opens (stops calling service)
  - All subsequent requests return fallback immediately
  - Prevents cascading failures

#### 🔁 Circuit Recovery
- **Scenario 5: Circuit half-open state**
  - After 5 seconds, circuit tries one request
  - If successful, circuit closes (normal operation resumes)
  - If failure, circuit stays open for another 5 seconds

#### 🔀 Mixed Success/Failure
- **Scenario 6: Alternating success and failure**
  - Some requests succeed, some fail
  - Circuit breaker tracks failure percentage
  - Opens when threshold exceeded

#### 🚀 Concurrent Requests
- **Scenario 7: High traffic handling**
  - Multiple threads making simultaneous requests
  - Circuit breaker handles concurrency correctly
  - No race conditions or deadlocks

#### 🔌 Service Isolation
- **Scenario 8: Failure in one service doesn't affect others**
  - UserService failure doesn't affect ProductService
  - Each service has independent circuit breaker
  - Isolation prevents cascading failures

## Running the Tests

### Run All Circuit Breaker Tests
```bash
mvn test -Dtest="*Hystrix*Test,*CircuitBreaker*Test"
```

### Run Specific Test Class
```bash
mvn test -Dtest=HystrixUserServiceTest
mvn test -Dtest=HystrixProductServiceTest
mvn test -Dtest=CircuitBreakerScenarioTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=HystrixUserServiceTest#testSearchUsers_Success
```

### Run with Verbose Output
```bash
mvn test -Dtest=HystrixUserServiceTest -X
```

## Expected Test Results

### ✅ Success Criteria
- All normal operation tests pass
- Failure scenarios return fallback (not exceptions)
- Circuit breaker opens after threshold
- Circuit breaker recovers after sleep window
- Concurrent requests handled correctly
- Services isolated from each other

### ⚠️ Known Limitations
- Integration tests require database connection
- Circuit breaker timing is approximate (may vary by a few milliseconds)
- Some tests may need adjustment based on actual Hystrix behavior

## Test Coverage

| Component | Test Coverage |
|-----------|--------------|
| HystrixUserService | ✅ 10 scenarios |
| HystrixProductService | ✅ 10 scenarios |
| Circuit Breaker Logic | ✅ 6 advanced scenarios |
| Integration | ✅ 2 scenarios |

## Troubleshooting

### Tests Failing Due to Timeouts
- Increase timeout in test if needed
- Check if database is slow or unavailable
- Verify Hystrix configuration

### Circuit Breaker Not Opening
- Verify failure threshold is met (10 requests, 50% failure)
- Check Hystrix configuration in `HystrixConfig.java`
- Ensure exceptions are being thrown correctly

### Integration Tests Failing
- Ensure database is configured for tests
- Check `application.properties` or test properties
- Consider using Testcontainers for isolated database testing

## Next Steps

1. **Add Performance Tests**: Measure circuit breaker overhead
2. **Add Metrics Tests**: Verify Hystrix metrics are collected
3. **Add End-to-End Tests**: Test circuit breaker in full request flow
4. **Add Chaos Engineering**: Simulate network partitions, slow responses

