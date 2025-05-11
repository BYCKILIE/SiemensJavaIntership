#!/bin/bash

# shellcheck disable=SC2046
export $(grep -v '^#' infra/environment/.env | xargs)

echo "Starting flyway migration"

./mvnw flyway:migrate \
  -Dflyway.url=jdbc:h2:mem:$DB_URL \
  -Dflyway.user=$DB_USERNAME \
  -Dflyway.password=$DB_PASSWORD

echo "Finished flyway migration"