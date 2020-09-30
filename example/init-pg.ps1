$dbInit = "$PSScriptRoot/db/init"

docker run -d --name pg --rm `
  -p 5432:5432 --shm-size=256MB `
  -v $dbInit/create-schema-pg.sql:/docker-entrypoint-initdb.d/01.sql `
  -v $dbInit/create-schema-objects.sql:/docker-entrypoint-initdb.d/02.sql `
  -v $dbInit/create-test-data-pg.sql:/docker-entrypoint-initdb.d/03.sql `
  -e POSTGRES_USER=drugs -e POSTGRES_PASSWORD=drugs -e POSTGRES_DB=drugs `
  postgres:12

# Connect via container embedded psql client via:
#    docker exec -it pg psql -U drugs
# or from the host
#  with jdbc via url:
#    jdbc:postgresql://localhost:5432/drugs
#  or with connection parameters:
#    host: localhost
#    port: 5432
#    user: drugs
#    database: drugs
#    (password is ignored)
