setup-migration:
	./infra/scripts/flyway_migrate.sh

start-app:
	./infra/scripts/start_app.sh

start-app-with-migration: setup-migration start-app

