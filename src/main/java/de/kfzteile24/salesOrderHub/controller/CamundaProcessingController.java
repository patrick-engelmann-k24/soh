package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.dto.mapper.KeyValuePropertyMapper;
import de.kfzteile24.salesOrderHub.dto.property.PersistentProperty;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Api("CamundaProcessingController")
@RestController
@RequestMapping(value = "/camunda-processing", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CamundaProcessingController {

    private final KeyValuePropertyService keyValuePropertyService;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final KeyValuePropertyMapper keyValuePropertyMapper;

    @ApiOperation(value = "Retrieve all persistent properties")
    @ApiResponse(code = 200, message = "OK", response = PersistentProperty.class, responseContainer = "List")
    @GetMapping( "/property")
    public ResponseEntity<List<PersistentProperty>> getAllPersistentProperties() {
        var persistentProperties = keyValuePropertyService.getAllProperties().stream()
                .map(keyValuePropertyMapper::toPersistentProperty)
                .collect(Collectors.toUnmodifiableList());
        return ResponseEntity.ok(persistentProperties);
    }

    @ApiOperation("Get persistent properties based on property key")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = PersistentProperty.class),
            @ApiResponse(code = 404, message = "Property not found in the DB")
    })
    @GetMapping("/property/{key}")
    public ResponseEntity<PersistentProperty> getPersistentProperty(@PathVariable String key) {
        return keyValuePropertyService.getPropertyByKey(key)
                .map(property -> ResponseEntity.ok(keyValuePropertyMapper.toPersistentProperty(property)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiOperation("Store existing persistent properties in the DB")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = PersistentProperty.class),
            @ApiResponse(code = 404, message = "Property to be saved not found in the DB")
    })

    @PutMapping("/property")
    public ResponseEntity<PersistentProperty> storePersistentProperty(@RequestParam String key, @RequestParam String value) {
        return keyValuePropertyService.getPropertyByKey(key)
                .map(property -> {
                    property.setValue(value);
                    var savedProperty = keyValuePropertyService.save(property);
                    return ResponseEntity.ok(keyValuePropertyMapper.toPersistentProperty(savedProperty));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiOperation("Modify 'pauseDropshipmentProcessing' flag and continue all paused dropshipment order process instances")
    @ApiResponse(code = 200, message = "OK", response = PersistentProperty.class)
    @PutMapping("/pause/dropshipment/{pauseDropshipmentProcessing}")
    public ResponseEntity<PersistentProperty> handleProcessingDropshipmentState(@PathVariable Boolean pauseDropshipmentProcessing) {
        var keyValueProperty = dropshipmentOrderService.setPauseDropshipmentProcessing(pauseDropshipmentProcessing);
        return ResponseEntity.ok(keyValuePropertyMapper.toPersistentProperty(keyValueProperty));
    }
}
