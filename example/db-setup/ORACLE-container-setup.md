# Oracle Docker Setup and Usage

Login to via docker account at `hub.docker.com` on the web and then "checkout" from:
    `https://hub.docker.com/_/oracle-database-enterprise-edition`

## Start the container

In terminal:
```
  docker login
  
  # Make a start script and start the db.
  echo 'docker run -d -it --name ora --rm -p1521:1521 -v OracleDBData:/ORCL store/oracle/database-enterprise:12.2.0.1-slim' > start_ora.sh
  chmod +x start_ora.sh
  ./start_ora.sh

  docker ps
  # Status should show healthy after about a minute.
```

## Connect via JDBC
Connection properties:
```
# as sys/sysdba
host: localhost
port: 1521
sid: ORCLCDB
user: sys as sysdba
password: Oradoc_db1
```

Once on as sys/sysdba, we can create the app/test user in the pluggable database (as opposed to common container db):
```
select * from v$services; -- find pdb full name for info to connect directly to pdb
alter session set container = ORCLPDB1;

create user drugs identified by drugs;
grant connect to drugs;
grant create session to drugs;
grant unlimited tablespace to drugs;
grant create table to drugs;
grant create view to drugs;
grant create procedure to drugs;
```

Now we can create a connection as the app user with the following connection properties:
```
host: localhost
port: 1521
service: orclpdb1.localdomain
# ^ note this is service not sid
user: drugs
password: drugs
```

Changes in this pluggable db will persist across restarts because of our volume mount in the start script.

