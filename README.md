# Expense Tracker API

A Spring Boot REST API for managing personal expenses with AI-powered insights.

## Features

- User authentication and management
- Expense tracking and categorization
- AI-powered expense analysis
- Docker containerization
- MySQL database integration

## Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Maven 3.9+ (for local development)

## Quick Start with Docker

1. **Clone the repository**
   ```bash
   git clone https://github.com/riteshsmalviya/expense-tracker.git
   cd expense-tracker-api
   ```

2. **Set up environment variables**
   ```bash
   # Copy the template files
   cp .env.template .env
   cp src/main/resources/application-docker.properties.template src/main/resources/application-docker.properties
   ```

3. **Update configuration files**
   - Edit `.env` with your database credentials and API keys
   - Edit `application-docker.properties` with your specific configuration

4. **Start the application**
   ```bash
   docker-compose up --build -d
   ```

5. **Verify the application is running**
   ```bash
   docker ps
   docker logs expense-tracker-app
   ```

The API will be available at `http://localhost:8080`

## Configuration

### Environment Variables (.env)
- `MYSQL_ROOT_PASSWORD`: Root password for MySQL
- `MYSQL_DATABASE`: Database name
- `SPRING_DATASOURCE_*`: Database connection details
- `DEEPSEEK_API_KEY`: API key for AI features

### Application Profiles
- `default`: Local development
- `dev`: Development environment
- `docker`: Docker containerized environment

## API Endpoints

- `GET /api/auth` - Authentication endpoints
- `GET /api/expenses` - Expense management
- `GET /api/ai` - AI-powered insights

## Development

For local development without Docker:

1. Start MySQL locally
2. Update `application-dev.properties` with local database settings
3. Run with dev profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Security Notes

- Never commit actual configuration files with credentials
- Use environment variables for sensitive data
- Template files are provided for reference