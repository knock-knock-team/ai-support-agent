.PHONY: help build up down restart logs clean test

help:
	@echo "Operator Service - Команды управления"
	@echo ""
	@echo "  make build       - Собрать все Docker образы"
	@echo "  make up          - Запустить все сервисы"
	@echo "  make down        - Остановить все сервисы"
	@echo "  make restart     - Перезапустить все сервисы"
	@echo "  make logs        - Показать логи всех сервисов"
	@echo "  make clean       - Удалить все контейнеры и volumes"
	@echo "  make test        - Протестировать API"
	@echo "  make backend     - Запустить только backend"
	@echo "  make frontend    - Запустить только frontend"
	@echo "  make db          - Запустить только базу данных"

build:
	docker-compose build

up:
	docker-compose up -d
	@echo "Сервисы запущены!"
	@echo "Frontend: http://localhost:3000"
	@echo "Backend:  http://localhost:8080"
	@echo ""
	@echo "Учетные данные администратора:"
	@echo "  Логин:  admin"
	@echo "  Пароль: Admin123!"

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f

clean:
	docker-compose down -v
	@echo "Удалены все контейнеры и volumes"

test:
	@echo "Тестирование API..."
	@curl -X POST http://localhost:8080/api/auth/login \
		-H "Content-Type: application/json" \
		-d '{"username": "admin", "password": "Admin123!"}' \
		| jq '.'

backend:
	docker-compose up -d operator-db operator-backend

frontend:
	docker-compose up -d operator-frontend

db:
	docker-compose up -d operator-db

backend-logs:
	docker-compose logs -f operator-backend

frontend-logs:
	docker-compose logs -f operator-frontend

db-logs:
	docker-compose logs -f operator-db
