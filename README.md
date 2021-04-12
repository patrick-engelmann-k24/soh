## Sales-Order-Hub

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/27d09ee13f4240c196b26c0525954c45)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=kfzteile24/soh-business-processing-engine&amp;utm_campaign=Badge_Grade)

### Consumed & Published Events 

A list of Events that this projects publishes or consumes are [here](https://kfzteile24.atlassian.net/wiki/x/NgB9Y).

### Camunda & BPMN

The Sales order Hub uses Camunda to run the BPMN processes.

The processes are located in src/main/resources/processes

## Github Package Repo configuration
To reach out the maven github for kfzteile24 you must add  
the following part to the **profiles** sections in .m2/settings.xml file in your home folder:
```
<profile>
   <id>github</id>
   <repositories>
       <repository>
           <id>github</id>
           <name>GitHub kfzteile24 Apache Maven Packages</name>
           <url>https://maven.pkg.github.com/kfzteile24/json-schema-java-bundle</url>
       </repository>
   </repositories>
</profile>
```

Further you must add this part under **settings** tag directly:
```
<activeProfiles>
  <activeProfile>github</activeProfile>
</activeProfiles>
```

Finally you have to provide your [github credentials](https://github.com/settings/tokens):  
```
<servers>
	<server>
            <id>github</id>
            <username>your-username</username>
            <password>your-password</password>
          </server>
</servers>
```

## Start the project
### Prerequisites
These command line tools must be in your path:
* curl
* postgres-client
* docker
* docker-compose  
* aws

Stop and remove all eventually existing localstack and postgres docker container.

#### Additional configuration on Windows
* Export the DOCKER_HOST environment variable in your Ubuntu subsystem with the correct
URL to the exposed docker daemon on Windows, e.g.  
```export DOCKER_HOST=tcp://localhost:2375```
  
### Start script
The script ```start.sh``` can be used to start and stop the dependencies and the service.
Additionally it can exectute the Flyway database migration.

The script reads the .env.localhost file to configure the database parameter.  
The script has the following usage:

```start.sh COMMAND```

These are the available commands:

|Command |Description|
|--------|-----------|
|dependencies|Start localstack, postgres and execute the flyway migration|
|service|Start the soh-business-processing-engine|
|all|Start dependencies and the soh-business-processing-engine|
|migration|Execute the flyway migration only (needs the postgres container running)|
|shutdown|Stop all containers|

For example, this command starts the dependencies:  
```./start.sh dependencies```

If you want to source in the environment variables from ```.env.localhost.dist``` into your current shell,
use this command:  
```source .env.localhost.dist```   
This is also necessary, if you want to run Maven directly in your shell, e.g. ```mvn clean package```.

### Application

Make a copy of application-local.yml.dist to application-local.yml and replace the dummy values starting with "your-"
with your configuration values.

The AWS ```cloud.aws.credentials``` can have any values for localstack and therefore don't have to be changed.

Source in the environment variables:   
```source .env.localhost.dist```

To install the app, run ```mvn clean package```. This installs all needed dependencies.

Then run the application with ```mvn spring-boot:run``` 

Login to the App:

http://localhost:8080 with
 
User: demo

Password: demo

### Run from IDE
To run this service, or the tests locally from Idea select ```Edit Configurations...``` and add
this environment variable:
SPRING_PROFILES_ACTIVE=local

### Repo for prepare sns topics in aws with terraform

[SNS-Topics](https://github.com/kfzteile24/soh-sns-topics)

## Documentation

### Architecture

![Architecture_pic](doc/img/Architecture_pic_soh_business_process_engine.png)

Link to [miro board](https://miro.com/app/board/o9J_lRPP23M=/) 

[Confluence](https://kfzteile24.atlassian.net/wiki/spaces/IT/pages/574554350/Sales+Order+Hub)

## Swagger RestApi Documentation
```
http(s)://<host>:<port>/swagger-ui.html
```