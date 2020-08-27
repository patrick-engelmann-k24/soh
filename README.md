## Sales-Order-Hub

### Consumed & Published Events 

A list of Events that this projects publishes or consumes are [here](https://kfzteile24.atlassian.net/wiki/x/NgB9Y).

### Camunda & BPMN

The Sales order Hub uses Camunda to run the BPMN processes.

The processes are located in src/main/resources/processes

## Start the project

### Database

1. Create a database and edit the following files:
    a. src/main/resources/application.yml -> replace jdbcURL with your database settings
    b. make a copy of .env.localhost.dist to .env.localhost -> replace FLYWAY_URL with your settings
    
2. run source .env.localhost
3. run ./migrate.sh -> check output. Flyway should no go through all the files needed to setup the database

### Application

To install the app, run ```mvn clean package```. This installs all needed dependencies. (Camunda runs with local H2 (memory) database for test and Postgres for other configs)

Then run the application with ```mvn spring-boot:start``` 

Login to the App:

http://localhost:8080 with
 
User: demo

Password: demo

(these settings are defined in src/main/resources/application-default.yml)

