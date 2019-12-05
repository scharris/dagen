# Database setup (Postgres)


```
# psql -U postgres
create user drugs with password 'drugs';
create database drugs owner drugs;
```

```
# psql -U drugs
create schema drugs authorization drugs;
\i example/db/ddl/create-schema-objects.sql
\i example/db/ddl/create-test-data-pg.sql
```
