#!/bin/bash

# shellcheck disable=SC2046
export $(grep -v '^#' infra/environment/.env | xargs)

echo "Starting dev server"

exec ./mvnw spring-boot:run
