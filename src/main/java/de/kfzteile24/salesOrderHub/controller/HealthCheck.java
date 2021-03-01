package de.kfzteile24.salesOrderHub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class HealthCheck {

    @GetMapping("/healthCheck")
    public ResponseEntity<String> healthResponse() {
        return new ResponseEntity<>("OK", HttpStatus.OK);

    }
}
