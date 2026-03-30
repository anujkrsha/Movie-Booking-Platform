.PHONY: up down logs reset ps kafka-topics

# Load .env if present so variables are available to sub-commands
-include .env
export

COMPOSE := docker compose
PROJECT  := booking-platform

# ──────────────────────────────────────────────
# up — start all infrastructure services
# ──────────────────────────────────────────────
up:
	@echo "Starting $(PROJECT) infrastructure..."
	$(COMPOSE) up -d --build
	@echo ""
	@echo "Services ready:"
	@echo "  PostgreSQL  → localhost:$(or $(POSTGRES_PORT),5432)"
	@echo "  Redis       → localhost:$(or $(REDIS_PORT),6379)"
	@echo "  Kafka       → localhost:$(or $(KAFKA_PORT),9092)"
	@echo "  Elasticsearch → http://localhost:$(or $(ES_PORT),9200)"
	@echo "  MailHog UI  → http://localhost:$(or $(MAILHOG_UI_PORT),8025)"

# ──────────────────────────────────────────────
# down — stop and remove containers (keeps volumes)
# ──────────────────────────────────────────────
down:
	@echo "Stopping $(PROJECT) infrastructure..."
	$(COMPOSE) down

# ──────────────────────────────────────────────
# logs — tail logs for all services (or pass service=<name>)
# ──────────────────────────────────────────────
logs:
ifdef service
	$(COMPOSE) logs -f $(service)
else
	$(COMPOSE) logs -f
endif

# ──────────────────────────────────────────────
# ps — show container status and health
# ──────────────────────────────────────────────
ps:
	$(COMPOSE) ps

# ──────────────────────────────────────────────
# reset — tear down everything including volumes and restart fresh
# ──────────────────────────────────────────────
reset:
	@echo "WARNING: This will delete all local data volumes. Ctrl-C to abort."
	@sleep 3
	$(COMPOSE) down -v --remove-orphans
	$(COMPOSE) up -d --build
	@echo "Environment reset complete."

# ──────────────────────────────────────────────
# kafka-topics — list all Kafka topics (convenience helper)
# ──────────────────────────────────────────────
kafka-topics:
	$(COMPOSE) exec kafka kafka-topics \
		--bootstrap-server localhost:9092 \
		--list
