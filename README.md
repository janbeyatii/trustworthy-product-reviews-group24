# Trustworthy Product Reviews

A Spring Boot application for managing trustworthy product reviews with user authentication and social features.

## Current Status

This is a **simplified foundation** with basic user authentication and a clean landing page. Features will be added step by step.

### What's Currently Implemented

- **Landing Page**: Clean, modern landing page explaining the platform's purpose
- **User Authentication**: Complete user CRUD operations using Supabase
- **Login/Signup Flow**: Users can create accounts and log in
- **Basic Dashboard**: Simple homepage after login with user profile management
- **Product Reviews**: Users can submit and view reviews with 1-5 star ratings on each product page
- **User Profile System**: 
  - View who you follow and who follows you
  - Search for users by email
  - View other users' profiles and follow/unfollow them
- **Social Features**: Follow/unfollow users to build your trust network
- **Responsive Design**: Modern, mobile-friendly UI
- **Circuit Breaker Protection**: Hystrix circuit breakers for database operations and external API calls
  - Automatic failure detection and fallback handling
  - Circuit breaker debug dashboard at `/debug.html` for monitoring and testing
- **Comprehensive Testing**: Unit tests and circuit breaker test scenarios

### Coming Soon

The following features will be added incrementally:

- Browse products by category and rating
- Discover similar users using Jaccard distance
- View trust network analytics
- See community leaders and most followed users

## Tech Stack

- **Backend**: Spring Boot 3.3.4, Java 17
- **Database**: PostgreSQL (Supabase)
- **Authentication**: Supabase Auth
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **Build Tool**: Maven
- **Circuit Breaker**: Hystrix (for fault tolerance and resilience)
- **Testing**: JUnit 5, Mockito

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Supabase account and project

### Configuration

1. Clone the repository
2. Set up your environment variables:
   - create a `.env` file
   - Add Supabase credentials to the `.env` file
3. Run the application: `mvn spring-boot:run`
4. Visit `http://localhost:8080` to see the landing page

### Environment Variables

The application uses the following environment variables (set them in your `.env` file):

```bash
# Supabase Configuration
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key
SUPABASE_SERVICE_ROLE=your_supabase_service_role_key
SUPABASE_JWT_SECRET=your_supabase_jwt_secret

# Database Configuration
SUPABASE_DATABASE_URL=your_database_url
SUPABASE_DATABASE_USERNAME=your_database_username
SUPABASE_DATABASE_PASSWORD=your_database_password
```

## Testing

The project includes comprehensive unit tests and circuit breaker tests. Here's how to run them:

### Running All Tests

```bash
mvn test
```

### Running Specific Test Suites

**Run all Hystrix circuit breaker tests:**
```bash
mvn test -Dtest="*Hystrix*Test,*CircuitBreaker*Test"
```

**Run controller tests:**
```bash
mvn test -Dtest="*ControllerTest"
```

**Run specific test classes:**
```bash
# Product controller tests
mvn test -Dtest=ProductControllerTest

# User controller tests
mvn test -Dtest=UserControllerTest

# Hystrix user service tests
mvn test -Dtest=HystrixUserServiceTest

# Hystrix product service tests
mvn test -Dtest=HystrixProductServiceTest

# Circuit breaker scenario tests
mvn test -Dtest=CircuitBreakerScenarioTest
```

**Run a specific test method:**
```bash
mvn test -Dtest=HystrixUserServiceTest#testSearchUsers_Success
```

### Available Test Suites

- **Controller Tests**: `ProductControllerTest`, `UserControllerTest`
- **Hystrix Circuit Breaker Tests**: 
  - `HystrixUserServiceTest` - Tests for user service circuit breakers (10 scenarios)
  - `HystrixProductServiceTest` - Tests for product service circuit breakers (10 scenarios)
  - `CircuitBreakerScenarioTest` - Advanced scenarios (concurrency, mixed failures, etc.)
  - `CircuitBreakerIntegrationTest` - Lightweight integration tests

### Test Coverage

The tests cover:
- Normal operation scenarios
- Failure handling and fallback behavior
- Circuit breaker opening/closing
- Timeout scenarios
- Concurrent request handling
- Service isolation

## Using the Profile Feature

Once logged in, you can:

1. **View Who You Follow**: Click the "Following" tab in the Profile section
2. **View Who Follows You**: Click the "Followers" tab
3. **Search for Users**: 
   - Click the "Search" tab
   - Start typing an email
   - Matching users will appear
4. **View User Profiles**: Click on any user to see their profile and follow/unfollow them

## Data Base Schema 

<img width="803" height="771" alt="Screenshot 2025-11-17 132024" src="https://github.com/user-attachments/assets/69bb4a9f-866f-4c33-ad56-b4ae6c867a4c" />

## UML diagram

<img width="3200" height="6080" alt="ProjectUml" src="https://github.com/user-attachments/assets/ecd6361d-82ea-4abc-b3bd-c371e77f9d2c" />
