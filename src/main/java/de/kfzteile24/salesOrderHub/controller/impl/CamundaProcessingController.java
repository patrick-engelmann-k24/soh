package de.kfzteile24.salesOrderHub.controller.impl;

import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
import de.kfzteile24.salesOrderHub.controller.IBaseController;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.mapper.KeyValuePropertyMapper;
import de.kfzteile24.salesOrderHub.dto.property.PersistentProperty;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.processmigration.ProcessMigrationService;
import de.kfzteile24.salesOrderHub.services.processmigration.mapper.MigrationMapper;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import io.swagger.v3.oas.annotations.Hidden;
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
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Slf4j
@Tag(name = "Camunda processing")
@RestController
@RequestMapping(value = "/camunda-processing", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CamundaProcessingController implements IBaseController {

    private final KeyValuePropertyService keyValuePropertyService;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final KeyValuePropertyMapper keyValuePropertyMapper;
    private final CamundaHelper camundaHelper;
    private final ProcessMigrationService processMigrationService;
    private final MigrationMapper migrationMapper;
    private final SalesOrderService salesOrderService;
    private final ProcessEngine processEngine;


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

    @Operation(summary = "Modify 'preventDropshipmentOrderReturnConfirmed' flag and continue to receive dropshipment order return confirmed messages",
            parameters = {
                @Parameter(in = ParameterIn.PATH, name = "preventDropshipmentOrderReturnConfirmed",
                        description = "'preventDropshipmentOrderReturnConfirmed' property value", example = "true")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Prevent Dropshipment Order Return Confirmed set successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = PersistentProperty.class))})
    })
    @PutMapping("/prevent/dropshipmentReturn/{preventDropshipmentOrderReturnConfirmed}")
    public ResponseEntity<PersistentProperty> handleDropshipmentOrderReturnConfirmedState(@PathVariable Boolean preventDropshipmentOrderReturnConfirmed) {
        var keyValueProperty = dropshipmentOrderService.setPreventDropshipmentOrderReturnConfirmed(preventDropshipmentOrderReturnConfirmed);
        return ResponseEntity.ok(keyValuePropertyMapper.toPersistentProperty(keyValueProperty));
    }

    @Operation(summary = "Release individual dropshipment order by order-number",
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "orderNumber",
                            description = "'orderNumber", example = "514045253")
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Dropshipment order released successfully"),
            @ApiResponse(responseCode  = "400", description  = "No dropshipment order found waiting to be releasing based on order number")
    })
    @PutMapping("/dropshipment/release/{orderNumber}")
    public ResponseEntity<Void> continueDropshipmentProcessing(@PathVariable String orderNumber) {
        camundaHelper.sendSignal(Signals.CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS,
                org.camunda.bpm.engine.variable.Variables.putValue(ORDER_NUMBER.getName(), orderNumber));
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Migrate Camunda process from specific version to the latest version of process definition",
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "processDefinition",
                            description = "Camunda process definition enumeration", example = "SALES_ORDER_PROCESS"),
                    @Parameter(in = ParameterIn.PATH, name = "version",
                            description = "'Camunda process version", example = "1"),
                    @Parameter(in = ParameterIn.QUERY, name = "migrateParent",
                            description = "Flag indicating whether also to migrate the parent Camunda process, if any",
                            example = "false")
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Migration started successfully")
    })
    @PostMapping("/process/{processDefinition}/{version}/migrate")
    public ResponseEntity<Void> migrationProcess(@PathVariable ProcessDefinition processDefinition,
                                                 @PathVariable int version,
                                                 @RequestParam boolean migrateParent) {
        processMigrationService.executeMigration(migrationMapper.map(processDefinition, version), migrateParent);
        return ResponseEntity.ok().build();
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleExceptions(Exception ex) {
        return Map.of("Error", ex.getLocalizedMessage());
    }
}
