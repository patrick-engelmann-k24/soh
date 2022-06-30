package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.dto.mapper.KeyValuePropertyMapper;
import de.kfzteile24.salesOrderHub.dto.property.PersistentProperty;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Camunda processing")
@RestController
@RequestMapping(value = "/camunda-processing", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CamundaProcessingController {

    private final KeyValuePropertyService keyValuePropertyService;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final KeyValuePropertyMapper keyValuePropertyMapper;

    @Operation(summary = "Retrieve all persistent properties")
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "List of all persistent properties", content = {
                    @Content(mediaType = "application/json", array =
                    @ArraySchema(schema = @Schema(implementation = PersistentProperty.class)))})
    })
    @GetMapping( "/property")
    public ResponseEntity<List<PersistentProperty>> getAllPersistentProperties() {
        var persistentProperties = keyValuePropertyService.getAllProperties().stream()
                .map(keyValuePropertyMapper::toPersistentProperty)
                .collect(Collectors.toUnmodifiableList());
        return ResponseEntity.ok(persistentProperties);
    }

    @Operation(summary = "Get persistent properties based on property key", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "key", description = "Persistent property key", example = "pauseDropshipmentProcessing")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Persistent property", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = PersistentProperty.class))}),
            @ApiResponse(responseCode  = "404", description  = "Property not found in the DB")
    })
    @GetMapping("/property/{key}")
    public ResponseEntity<PersistentProperty> getPersistentProperty(@PathVariable String key) {
        return keyValuePropertyService.getPropertyByKey(key)
                .map(property -> ResponseEntity.ok(keyValuePropertyMapper.toPersistentProperty(property)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Store existing persistent properties in the DB", parameters = {
            @Parameter(name = "key", description = "Persistent property key", example = "pauseDropshipmentProcessing"),
            @Parameter(name = "value", description = "Persistent property value", example = "true")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Persistent property", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = PersistentProperty.class))}),
            @ApiResponse(responseCode  = "404", description  = "Property not found in the DB")
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

    @Operation(summary = "Modify 'pauseDropshipmentProcessing' flag and continue all paused dropshipment order process instances",
            parameters = {
                @Parameter(in = ParameterIn.PATH, name = "pauseDropshipmentProcessing",
                        description = "'pauseDropshipmentProcessing' property value", example = "true")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Pause processing dropshipment set successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = PersistentProperty.class))})
    })
    @PutMapping("/pause/dropshipment/{pauseDropshipmentProcessing}")
    public ResponseEntity<PersistentProperty> handleProcessingDropshipmentState(@PathVariable Boolean pauseDropshipmentProcessing) {
        var keyValueProperty = dropshipmentOrderService.setPauseDropshipmentProcessing(pauseDropshipmentProcessing);
        return ResponseEntity.ok(keyValuePropertyMapper.toPersistentProperty(keyValueProperty));
    }
}
