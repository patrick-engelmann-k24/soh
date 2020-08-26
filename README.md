## Demo Camundo SOH project

This projects adds a very simple process.

To install the app, run ```mvn clean package```. This installs all needed dependencies. (Camunda runs with local H2 (memory) database)

Then run the application with ```mvn spring-boot:start``` 

Login to the App:

http://localhost:8080 with
 
User: demo
Password: demo

You can start a new process in the Cockpit: http://localhost:8080/camunda/app/tasklist/default/#/?processStart (Demo_SOH_process).

When you start the process, it will execute the Process (you'll see it in the log output).

 
