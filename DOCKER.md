# Docker Setup for Expense Tracker API

This document explains how to run the Expense Tracker API using Docker.

## Prerequisites

- Docker and Docker Compose installed
- Git (to clone the repository)

## Quick Start

1. **Clone the repository** (if not already done):
   ```bash
   git clone <your-repo-url>
   cd expense-tracker-api
   ```

2. **Set up environment variables**:
   ```bash
   cp .env.example .env
   # Edit .env file with your actual API keys
   ```

3. **Build and run with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

4. **Access the application**:
   - API: http://localhost:8080
   - Health check: http://localhost:8080/actuator/health

## Docker Commands

### Build the application
```bash
docker build -t expense-tracker-api .
```

### Run with Docker Compose (recommended)
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This will delete your data)
docker-compose down -v
```

### Run standalone container
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/expense_tracker \
  -e SPRING_DATASOURCE_USERNAME=your_username \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e DEEPSEEK_API_KEY=your_api_key \
  expense-tracker-api
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database connection URL | - |
| `SPRING_DATASOURCE_USERNAME` | Database username | - |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - |
| `DEEPSEEK_API_KEY` | AI API key for insights | - |
| `DEEPSEEK_API_URL` | AI API endpoint | https://openrouter.ai/api/v1/chat/completions |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | docker |

## Docker Compose Services

- **mysql-db**: MySQL 8.0 database server
- **expense-tracker-api**: Spring Boot application

## Volumes and Data Persistence

- Database data is persisted in the `mysql_data` Docker volume
- Application logs are mounted to `./logs` directory

## Health Checks

Both services include health checks:
- MySQL: Uses `mysqladmin ping`
- Spring Boot: Uses Spring Actuator health endpoint

## Development

For development with hot reload:
```bash
# Run only the database
docker-compose up mysql-db -d

# Run the application locally with your IDE
# Configure your IDE to use the database at localhost:3306
```

## Troubleshooting

1. **Port conflicts**: If port 8080 or 3306 is already in use, modify the ports in `docker-compose.yml`

2. **Database connection issues**: Ensure the MySQL container is healthy before starting the application

3. **Memory issues**: Adjust JVM memory settings in the Dockerfile `JAVA_OPTS` environment variable

4. **API key issues**: Make sure your `.env` file contains the correct `DEEPSEEK_API_KEY`

## Production Considerations

- Use Docker secrets for sensitive data
- Set up proper logging and monitoring
- Use a reverse proxy (nginx) for SSL termination
- Consider using managed database services
- Implement proper backup strategies