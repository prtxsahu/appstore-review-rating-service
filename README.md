# App Store Review & Rating Service

A Spring Boot application for managing app store reviews, ratings, and comments.

## Quick Start

### Prerequisites
- Java 20+
- Docker and Docker Compose
- Maven

### Setup Commands

1. **Start infrastructure services:**
   ```bash
   docker compose up -d
   ```

2. **Run the Spring Boot application:**
   ```bash
   mvn spring-boot:run
   ```

The application will be available at `http://localhost:8080`

## API Testing

A Postman collection is included for testing the APIs:
- `app store.postman_collection.json`

Import this collection into Postman to test all available endpoints.
