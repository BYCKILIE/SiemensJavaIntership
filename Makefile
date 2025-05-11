setup-migration:
	./infra/scripts/flyway_migrate.sh

# Use this to start with when in memory db instead of on disk
start-app:
	./infra/scripts/start_app.sh

start-app-with-migration: setup-migration start-app

