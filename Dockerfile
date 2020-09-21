FROM maven:3.6.3-adoptopenjdk-11 as MAVEN
WORKDIR .
COPY . .
RUN mvn clean install -DskipTests

FROM openjdk:12-jdk-alpine

COPY --from=MAVEN target/de.kfzteile24.sales-order-hub-0.0.1.jar app.jar
COPY --from=MAVEN docker-flyway.conf docker-flyway.conf
COPY --from=MAVEN entrypoint.sh entrypoint.sh

COPY https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/6.5.6/flyway-commandline-6.5.6-linux-x64.tar.gz /opt

RUN tar -zxf /opt/flyway-commandline-6.5.6-linux-x64.tar.gz -C /opt &&\
                           rm /opt/flyway-commandline-6.5.6-linux-x64.tar.gz && \
                           ln -s /opt/flyway-6.5.6 /opt/flyway && \
                           mv docker-flyway.conf /opt/flyway/conf/flyway.conf && \
                           chmod +x /opt/flyway/flyway

RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]