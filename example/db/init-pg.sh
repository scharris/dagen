#!/bin/sh
DDL_DIR=$(cd $(dirname $0) && pwd)/ddl

docker run -d --rm  -p 5432:5432 --shm-size=256MB \
  -e POSTGRES_USER=drugs -e POSTGRES_PASSWORD=drugs -e POSTGRES_DB=drugs \
  -v $DDL_DIR/create-schema-pg.sql:/docker-entrypoint-initdb.d/01.sql \
  -v $DDL_DIR/create-schema-objects.sql:/docker-entrypoint-initdb.d/02.sql \
  -v $DDL_DIR/create-test-data-pg.sql:/docker-entrypoint-initdb.d/03.sql \
  postgres:12

# Connect via host psql via:
#    psql -h 127.0.0.1 -U drugs
