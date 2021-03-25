package de.kfzteile24.salesOrderHub.dto.order.header;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentProviderData {
    private String code;
    private String promotionIdentifier;
    private String externalId;
    private BigDecimal transactionAmount;
    private String billingAgreement;
    private String paymentProviderTransactionId;
    private String paymentProviderPaymentId;
    private String paymentProviderStatus;
    private String paymentProviderOrderDescription;
    private String paymentProviderDescription;
    private String paymentProviderCode;
    private String pseudoCardNumber;
    private String cardExpiryDate;
    private String cardBrand;
    private String orderDescription;
    private String senderHolder;
    private String accountNumber;
    private String bankCode;
    private String bankName;
    private String bic;
    private String countryCode;
    private String request;
    private String response;
    private String matchingId;
}
