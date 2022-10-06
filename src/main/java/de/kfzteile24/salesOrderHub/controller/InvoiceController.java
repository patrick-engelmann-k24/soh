package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.dto.invoice.InvoiceDocument;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.REQUEST_ID_KEY;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TRACE_ID_NAME;

/**
 * Invoice Rest Controller Object
 *
 * <P>Rest-Endpoint for direct request to handle changes on an invoice</P>
 *
 * @author Mykhailo Skliar
 * @version 1.0
 */
@Slf4j
@Tag(name = "Invoice processing")
@RestController
@RequestMapping("/api/v1/invoice")
@RequiredArgsConstructor
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;

    @ModelAttribute
    public void setResponseHeader(HttpServletResponse response) {
        response.addHeader(REQUEST_ID_KEY, MDC.get(TRACE_ID_NAME));
    }

    /**
     * Get invoice document with Base64 encoded content by invoice number
     * @param invoiceNumber The invoice number of the invoice which should be returned
     * @return InvoiceDocument
     */
    @Operation(summary = "Get invoice document with Base64 encoded content by invoice number", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "invoiceNumber",
                    description = "The invoice document number")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Invoice Document with Base64 encoded content for invoice number", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = InvoiceDocument.class))}),
            @ApiResponse(responseCode  = "404", description  = "No invoice document found")
    })
    @GetMapping("/invoice-document/{invoiceNumber}")
    public ResponseEntity<InvoiceDocument> getInvoiceDocument(@PathVariable String invoiceNumber) {
        final InvoiceDocument invoiceDocument = invoiceService.getInvoiceDocument(invoiceNumber);
        return ResponseEntity.ok(invoiceDocument);
    }

    /**
     * Download invoice document by invoice number
     * @param invoiceNumber The invoice number of the invoice which should be downloaded
     * @return StreamingResponseBody
     */
    @Operation(summary = "Download invoice document by invoice number", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "invoiceNumber",
                    description = "The invoice document number")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode  = "200", description  = "Invoice Document File based on invoice number", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = byte[].class))}),
            @ApiResponse(responseCode  = "404", description  = "No invoice document found")
    })
    @GetMapping(value = "/download/{invoiceNumber}")
    public HttpEntity<byte[]> downloadInvoiceDocument(@PathVariable String invoiceNumber) throws IOException {
        var bytes = invoiceService.getInvoiceDocumentAsByteArray(invoiceNumber);
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        header.set(HttpHeaders.CONTENT_DISPOSITION,
                MessageFormat.format("attachment; filename=invoice-{0}.pdf", invoiceNumber));
        header.setContentLength(bytes.length);
        return new HttpEntity<byte[]>(bytes, header);
    }


}
