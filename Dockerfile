FROM maven:3.6.3-adoptopenjdk-14 as MAVEN
WORKDIR .
COPY . .
RUN mkdir ~/.m2 && echo "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\"><localRepository/><interactiveMode/><offline/><pluginGroups/><servers><server><id>k24-github</id><username>k24-boe-deployment</username><password>dfe22ba0513a589cf6fb69e72cca6c5165ac51d6</password></server></servers><mirrors/><proxies/><profiles><profile><id>k24-github</id><repositories><repository><id>k24-github</id><name>GitHub kfzteile24 Apache Maven Packages</name><url>https://maven.pkg.github.com/kfzteile24/json-schema-java-bundle</url></repository></repositories></profile></profiles><activeProfiles><activeProfile>k24-github</activeProfile></activeProfiles></settings>" > ~/.m2/settings.xml
RUN mvn clean install -DskipTests --batch-mode --no-transfer-progress

FROM openjdk:14-slim-buster

COPY --from=MAVEN target/sales-order-hub-bpmn-0.0.1.jar app.jar
COPY --from=MAVEN docker-flyway.conf /docker-flyway.conf
COPY --from=MAVEN entrypoint.sh /entrypoint.sh
COPY --from=MAVEN src/main/resources/db/migration/* /migrations/

ADD https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/6.5.6/flyway-commandline-6.5.6-linux-x64.tar.gz /opt

RUN tar -zxf /opt/flyway-commandline-6.5.6-linux-x64.tar.gz -C /opt &&\
                           rm /opt/flyway-commandline-6.5.6-linux-x64.tar.gz && \
                           ln -s /opt/flyway-6.5.6 /opt/flyway && \
                           mv /docker-flyway.conf /opt/flyway/conf/flyway.conf && \
                           chmod +x /opt/flyway/flyway && mv /migrations/* /opt/flyway/sql/

RUN chmod +x /entrypoint.sh
CMD [""]
ENTRYPOINT ["/entrypoint.sh"]
