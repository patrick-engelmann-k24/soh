package de.kfzteile24.salesOrderHub.dto.sns.paypal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaypalRequestPayload {

    @JsonProperty("AmountCustomerPayment")
    private BigDecimal amountCustomerPayment;
    @JsonProperty("CoreVoucherNumber")
    private String coreVoucherNumber;
    @JsonProperty("CreditNoteNumber")
    private String creditNoteNumber;
    @JsonProperty("Currency")
    private String currency;
    @JsonProperty("CustAccount")
    private String custAccount;
    @JsonProperty("CustName")
    private String custName;
    @JsonProperty("DataAreaId")
    private String dataAreaId;
    @JsonProperty("OrderGroupId")
    private String orderGroupId;
    @JsonProperty("OriginialTransactionNumber")
    private String originalTransactionNumber;
    @JsonProperty("PayableAmount")
    private BigDecimal payableAmount;
    @JsonProperty("PaymentMethod")
    private String paymentMethod;

}
