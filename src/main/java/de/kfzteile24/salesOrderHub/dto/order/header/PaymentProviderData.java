package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class PaymentProviderData {
    String code;
    String promotionIdentifier;
    String externalId;
    Number transactionAmount;
    String billingAgreement;
    String paymentProviderTransactionId;
    String paymentProviderPaymentId;
    String paymentProviderStatus;
    String paymentProviderOrderDescription;
    String paymentProviderDescription;
    String paymentProviderCode;
    String pseudoCardNumber;
    String cardExpiryDate;
    String cardBrand;
    String orderDescription;
    String senderHolder;
    String accountNumber;
    String bankCode;
    String bankName;
    String bic;
    String countryCode;
    String request;
    String response;
    String matchingId;


}
