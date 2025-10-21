# trustworthy-product-reviews
A review platform where users rate products with stars and text. Users can follow trusted reviewers, sort products by overall or followed ratings, and find similar users through Jaccard distance. Reviews are ranked by similarity, top reviewers are highlighted, and trust is based on network closeness.

## Tech Stack: Spring Boot (Java 17) packaged as a WAR and deployed on Tomcat 10
## Deployment: GitHub Actions for CI/CD
## Authentication: Azure App Service Authentication (Google OAuth 2.0 provider)
## Hosting: Azure Web App
## Database: Azure SQL Database (Free Student Tier)