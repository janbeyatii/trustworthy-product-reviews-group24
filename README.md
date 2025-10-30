# Trustworthy Product Reviews

A Spring Boot application for managing trustworthy product reviews with user authentication and social features.

## Current Status

This is a **simplified foundation** with basic user authentication and a clean landing page. Features will be added step by step.

### What's Currently Implemented

- **Landing Page**: Clean, modern landing page explaining the platform's purpose
- **User Authentication**: Complete user CRUD operations using Supabase
- **Login/Signup Flow**: Users can create accounts and log in
- **Basic Dashboard**: Simple homepage after login with user profile management
- **User Profile System**: 
  - View who you follow and who follows you
  - Search for users by username or email (real-time autocomplete)
  - View other users' profiles and follow/unfollow them
- **Social Features**: Follow/unfollow users to build your trust network
- **Responsive Design**: Modern, mobile-friendly UI

### Coming Soon

The following features will be added incrementally:

- Write and manage product reviews
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

## Using the Profile Feature

Once logged in, you can:

1. **View Who You Follow**: Click the "Following" tab in the Profile section
2. **View Who Follows You**: Click the "Followers" tab
3. **Search for Users**: 
   - Click the "Search" tab
   - Start typing an email
   - Matching users will appear
4. **View User Profiles**: Click on any user to see their profile and follow/unfollow them
