.PHONY: help build start stop restart logs clean test

# Default target
help:
	@echo "=================================="
	@echo "Mail Processor Service - Makefile"
	@echo "=================================="
	@echo ""
	@echo "Available commands:"
	@echo "  make build       - Build Docker images"
	@echo "  make start       - Start all services"
	@echo "  make stop        - Stop all services"
	@echo "  make restart     - Restart all services"
	@echo "  make logs        - View logs"
	@echo "  make logs-app    - View application logs"
	@echo "  make clean       - Stop and remove all containers and volumes"
	@echo "  make test        - Run tests"
	@echo "  make db          - Connect to PostgreSQL"
	@echo "  make kafka-test  - Test Kafka connection"
	@echo "  make setup       - Initial setup (copy .env.example)"
	@echo ""

# Initial setup
setup:
	@if [ ! -f .env ]; then \
		echo "Creating .env file from .env.example..."; \
		cp .env.example .env; \
		echo "✓ .env file created"; \
		echo ""; \
		echo "⚠️  Please edit .env file with your credentials:"; \
		echo "   - MAIL_USERNAME"; \
		echo "   - MAIL_PASSWORD"; \
	else \
		echo "✓ .env file already exists"; \
	fi

# Build Docker images
build:
	@echo "Building Docker images..."
	docker-compose build

# Start all services
start: setup
	@echo "Starting services..."
	docker-compose up -d
	@echo ""
	@echo "✓ Services started!"
	@echo ""
	@echo "View logs: make logs"
	@echo "Stop services: make stop"

# Stop all services
stop:
	@echo "Stopping services..."
	docker-compose down

# Restart all services
restart:
	@echo "Restarting services..."
	docker-compose restart

# View all logs
logs:
	docker-compose logs -f

# View application logs only
logs-app:
	docker-compose logs -f mail-processor

# Clean everything
clean:
	@echo "⚠️  This will remove all containers, volumes, and data!"
	@echo "Press Ctrl+C to cancel, or Enter to continue..."
	@read line
	docker-compose down -v
	@echo "✓ Cleaned up"

# Run tests
test:
	@echo "Running tests..."
	./mvnw test

# Connect to PostgreSQL
db:
	docker exec -it postgres psql -U postgres -d mail_processor

# Test Kafka connection
kafka-test:
	@echo "Testing Kafka..."
	docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# View Kafka topics
kafka-topics:
	docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Create Kafka topic manually
kafka-create-topic:
	docker exec -it kafka kafka-topics --create --topic email-requests --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# View Kafka consumer groups
kafka-groups:
	docker exec -it kafka kafka-consumer-groups --list --bootstrap-server localhost:9092

# Package application
package:
	@echo "Packaging application..."
	./mvnw clean package -DskipTests

# Run application locally (without Docker)
run-local:
	@echo "Running application locally..."
	./mvnw spring-boot:run

# Check service health
health:
	@echo "Checking service health..."
	@curl -s http://localhost:8080/actuator/health | jq . || echo "Service not available or jq not installed"

# View service status
status:
	@echo "Service Status:"
	@docker-compose ps
