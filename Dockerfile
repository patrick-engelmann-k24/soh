FROM maven:3.6.3-adoptopenjdk-11 as MAVEN
RUN mvn clean install -DskipTests

FROM openjdk:12-jdk-alpine

COPY --from=MAVEN target/de.kfzteile24.sales-order-hub-0.0.1.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]