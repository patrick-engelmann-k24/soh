## Sales-Order-Hub

### Consumed & Published Events 

A list of Events that this projects publishes or consumes are [here](https://kfzteile24.atlassian.net/wiki/x/NgB9Y).

### Camunda & BPMN

The Sales order Hub uses Camunda to run the BPMN processes.

The processes are located in src/main/resources/processes

## Start the project

### Database

1. Create a database and edit the following files:
    * src/main/resources/application.yml -> replace jdbcURL with your database settings
    * make a copy of .env.localhost.dist to .env.localhost -> replace FLYWAY_URL with your settings
    
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

### Repo for prepare sns topics in aws with terraform

[SNS-Topics](https://github.com/kfzteile24/soh-sns-topics)

## Documentation


## Localstack for local AWS using/testing
To work and test sns and sqs local on dev computer use `docker-compose.yml` in folder `aws_localstack`.  
For mac users use command:  
<code>TMPDIR=/private$TMPDIR DATADIR=/tmp/localstack/data docker-compose up</code>

For all other user:  
<code>DATADIR=/tmp/localstack/data docker-compose up</code>

Run this command inside the folder.
Adapt the DATADIR for your local behavior.

Currently there are only SNS and SQS service configured.

Adapt also your `application-local.yml` file. Add the following

```
cloud:
  aws:
    endpoint:
      url: "http://localhost:4566"
```

Further instructions you can find [here](aws_localstack/README.md)
