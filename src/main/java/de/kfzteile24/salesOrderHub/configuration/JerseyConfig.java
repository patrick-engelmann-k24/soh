package de.kfzteile24.salesOrderHub.configuration;

import org.springframework.stereotype.Component;

import org.camunda.bpm.spring.boot.starter.rest.CamundaJerseyResourceConfig;

import javax.ws.rs.ApplicationPath;


@Component
@ApplicationPath("/engine-rest")
public class JerseyConfig extends CamundaJerseyResourceConfig {

    @Override
    protected void registerAdditionalResources() {
        //NOTE: Modify the configuration or register new resources here.
    }
}
