package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentProviderData {
    private String code;
    private String promotionIdentifier;
    private String externalId;
    private String externalTransactionId;
    private String externalPaymentStatus;
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
    private String senderFirstName;
    private String senderLastName;
    private String accountNumber;
    private String accountBank;
    private String accountOwner;
    private String bankCode;
    private String bankName;
    private String iban;
    private String bic;
    private String countryCode;
    private String request;
    private String response;
    private String matchingId;
    private String paymentAmountAuth;
    private String paymentAmountCred;
    private String paymentAmountCap;
    private String merchantId;
}
