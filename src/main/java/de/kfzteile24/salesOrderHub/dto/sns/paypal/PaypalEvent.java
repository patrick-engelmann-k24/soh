package de.kfzteile24.salesOrderHub.dto.sns.paypal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaypalEvent {

    @JsonProperty("OrderNumber")
    private String orderNumber;
    @JsonProperty("OrderGroupId")
    private String orderGroupId;
    @JsonProperty("PaypalAccountId")
    private String paypalAccountId;
    @JsonProperty("SaleId")
    private String saleId;
    @JsonProperty("CaptureId")
    private String captureId;
    @JsonProperty("Currency")
    private String currency;
    @JsonProperty("RefundAmount")
    private String refundAmount;
    @JsonProperty("TotalAmount")
    private String totalAmount;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("Reason")
    private String reason;
    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;
    @JsonProperty("RequestPayload")
    private PaypalRequestPayload paypalRequestPayload;
}
