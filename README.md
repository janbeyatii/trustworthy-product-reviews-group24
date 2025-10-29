# Trustworthy Product Reviews

A Spring Boot application for managing trustworthy product reviews with user authentication and social features.

## Current Status

This is a **simplified foundation** with basic user authentication and a clean landing page. Features will be added step by step.

### What's Currently Implemented

- ✅ **Landing Page**: Clean, modern landing page explaining the platform's purpose
- ✅ **User Authentication**: Complete user CRUD operations using Supabase
- ✅ **Login/Signup Flow**: Users can create accounts and log in
- ✅ **Basic Dashboard**: Simple homepage after login with user profile management
- ✅ **Responsive Design**: Modern, mobile-friendly UI

### Coming Soon

The following features will be added incrementally:

- 📝 Write and manage product reviews
- 👥 Follow trusted reviewers  
- 🔍 Browse products by category and rating
- 🎯 Discover similar users using Jaccard distance
- 📊 View trust network analytics
- 🏆 See community leaders and most followed users

## Tech Stack

- **Backend**: Spring Boot 3.3.4, Java 17
- **Database**: PostgreSQL (Supabase)
- **Authentication**: Supabase Auth
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **Build Tool**: Maven

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Supabase account and project

### Configuration

1. Clone the repository
2. Set up your environment variables:
   - Copy `.env.example` to `.env` (if it exists) or create a `.env` file
   - Add your Supabase credentials to the `.env` file
3. Ensure `src/main/resources/application.properties` exists (it reads values from environment variables; no secrets are committed).
4. Run the application: `mvn spring-boot:run`
5. Visit `http://localhost:8080` to see the landing page

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

## Current API Endpoints

- `GET /api/whoami` - Get current user information (requires authentication)

## Frontend

The application includes a modern, responsive frontend:

- **Landing Page** (`/`): Explains the platform and provides login/signup
- **Dashboard** (`/app.html`): User homepage with profile management
- **Authentication**: Complete login/signup/password reset flow

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/trustworthyreviews/
│   │   ├── Application.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   └── SupabaseConfig.java
│   │   ├── controller/
│   │   │   └── UserController.java
│   │   ├── security/
│   │   │   ├── SupabaseJwtFilter.java
│   │   │   ├── SupabaseJwtService.java
│   │   │   └── SupabaseUser.java
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       ├── application.properties
│       └── static/
│           ├── index.html (landing page)
│           ├── app.html (dashboard)
│           ├── css/index.css
│           └── js/
│               ├── auth.js
│               └── app.js
```

## Next Steps

This foundation is ready for incremental feature development. Each new feature will be added as a separate step to maintain code quality and testability.

## License

This project is licensed under the MIT License.