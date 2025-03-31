#!/bin/sh

echo "executing e2e tests..."

docker compose -f ./docker-compose-e2e.yaml up
docker compose -f ./docker-compose-e2e.yaml down
