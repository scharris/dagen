#!/bin/sh
SCRIPT_DIR=$(cd $(dirname $0) && pwd)/db/init
cd "$SCRIPT_DIR"

docker run -d --name pg --rm  -p 5432:5432 --shm-size=256MB \
  -e POSTGRES_USER=drugs -e POSTGRES_PASSWORD=drugs -e POSTGRES_DB=drugs \
  -v ./create-schema-pg.sql:/docker-entrypoint-initdb.d/01.sql \
  -v ./create-schema-objects.sql:/docker-entrypoint-initdb.d/02.sql \
  -v ./create-test-data-pg.sql:/docker-entrypoint-initdb.d/03.sql \
  postgres:12

# Connect via container embedded psql client via:
#    docker exec -it pg psql -U drugs
# or via host psql if postgresql client is installed, via:
#    psql -h 127.0.0.1 -U drugs
