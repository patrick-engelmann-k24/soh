FROM openjdk:12-jdk-alpine

COPY target/de.kfzteile24.sales-order-hub-0.0.1.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]