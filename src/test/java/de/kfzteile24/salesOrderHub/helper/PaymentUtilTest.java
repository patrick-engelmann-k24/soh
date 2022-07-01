package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.PaymentProviderData;
import de.kfzteile24.soh.order.dto.Payments;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

class PaymentUtilTest {
    
    private final static String ANY_EXTERNALID = RandomStringUtils.randomAlphabetic(5);
    private final static String ANOTHER_EXTERNALID = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PAYMENTPROVIDERTRANSACTIONID = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PAYMENTPROVIDERPAYMENTID = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PAYMENTPROVIDERSTATUS = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PAYMENTPROVIDERORDERDESCRIPTION = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PAYMENTPROVIDERCODE = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PSEUDOCARDNUMBER = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_CARDEXPIRYDATE = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_CARDBRAND = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_ORDERDESCRIPTION = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_EXTERNALTRANSACTIONID = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_SENDERFIRSTNAME = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_SENDERLASTNAME = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_EXTERNALPAYMENTSTATUS = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_SENDERHOLDER = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_ACCOUNTNUMBER = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_BANKCODE = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_BANKNAME = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_COUNTRYCODE = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_IBAN = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_BIC = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_PAYMENTPROVIDERDESCRIPTION = RandomStringUtils.randomAlphabetic(5);
    private final static String ANY_MATCHINGID = RandomStringUtils.randomAlphabetic(5);
    private final static UUID ANY_TRANSACTIONALID  = UUID.randomUUID();
    private final static UUID ANOTHER_TRANSACTIONALID  = UUID.randomUUID();
    private final static BigDecimal ANY_TRANSACTIONAMOUNT  = BigDecimal.ONE;
    private final static BigDecimal ANOTHER_TRANSACTIONAMOUNT  = BigDecimal.ONE;

    private final static Payments SOURCE = Payments.builder()
            .paymentProviderData(PaymentProviderData.builder()
                    .externalId(ANY_EXTERNALID)
                    .paymentProviderTransactionId(ANY_PAYMENTPROVIDERTRANSACTIONID)
                    .paymentProviderPaymentId(ANY_PAYMENTPROVIDERPAYMENTID)
                    .paymentProviderStatus(ANY_PAYMENTPROVIDERSTATUS)
                    .paymentProviderOrderDescription(ANY_PAYMENTPROVIDERORDERDESCRIPTION)
                    .paymentProviderCode(ANY_PAYMENTPROVIDERCODE)
                    .pseudoCardNumber(ANY_PSEUDOCARDNUMBER)
                    .cardExpiryDate(ANY_CARDEXPIRYDATE)
                    .cardBrand(ANY_CARDBRAND)
                    .orderDescription(ANY_ORDERDESCRIPTION)
                    .externalTransactionId(ANY_EXTERNALTRANSACTIONID)
                    .senderFirstName(ANY_SENDERFIRSTNAME)
                    .senderLastName(ANY_SENDERLASTNAME)
                    .externalPaymentStatus(ANY_EXTERNALPAYMENTSTATUS)
                    .billingAgreement(true)
                    .senderHolder(ANY_SENDERHOLDER)
                    .accountNumber(ANY_ACCOUNTNUMBER)
                    .bankCode(ANY_BANKCODE)
                    .bankName(ANY_BANKNAME)
                    .countryCode(ANY_COUNTRYCODE)
                    .iban(ANY_IBAN)
                    .bic(ANY_BIC)
                    .paymentProviderDescription(ANY_PAYMENTPROVIDERDESCRIPTION)
                    .matchingId(ANY_MATCHINGID)
                    .transactionAmount(ANY_TRANSACTIONAMOUNT)
                    .build())
            .paymentTransactionId(ANY_TRANSACTIONALID)
            .build();

    @Test
    void updatePaymentProvider() {

        Payments target = Payments.builder()
                .build();

        PaymentUtil.updatePaymentProvider(SOURCE, target);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(target.getPaymentTransactionId()).isEqualTo(ANY_TRANSACTIONALID);
            var targetPaymentProviderData = target.getPaymentProviderData();
            softly.assertThat(targetPaymentProviderData.getExternalId()).isEqualTo(ANY_EXTERNALID);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderTransactionId()).isEqualTo(ANY_PAYMENTPROVIDERTRANSACTIONID);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderPaymentId()).isEqualTo(ANY_PAYMENTPROVIDERPAYMENTID);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderStatus()).isEqualTo(ANY_PAYMENTPROVIDERSTATUS);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderOrderDescription()).isEqualTo(ANY_PAYMENTPROVIDERORDERDESCRIPTION);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderCode()).isEqualTo(ANY_PAYMENTPROVIDERCODE);
            softly.assertThat(targetPaymentProviderData.getPseudoCardNumber()).isEqualTo(ANY_PSEUDOCARDNUMBER);
            softly.assertThat(targetPaymentProviderData.getCardExpiryDate()).isEqualTo(ANY_CARDEXPIRYDATE);
            softly.assertThat(targetPaymentProviderData.getCardBrand()).isEqualTo(ANY_CARDBRAND);
            softly.assertThat(targetPaymentProviderData.getOrderDescription()).isEqualTo(ANY_ORDERDESCRIPTION);
            softly.assertThat(targetPaymentProviderData.getExternalTransactionId()).isEqualTo(ANY_EXTERNALTRANSACTIONID);
            softly.assertThat(targetPaymentProviderData.getSenderFirstName()).isEqualTo(ANY_SENDERFIRSTNAME);
            softly.assertThat(targetPaymentProviderData.getSenderLastName()).isEqualTo(ANY_SENDERLASTNAME);
            softly.assertThat(targetPaymentProviderData.getExternalPaymentStatus()).isEqualTo(ANY_EXTERNALPAYMENTSTATUS);
            softly.assertThat(targetPaymentProviderData.getBillingAgreement()).isEqualTo(true);
            softly.assertThat(targetPaymentProviderData.getSenderHolder()).isEqualTo(ANY_SENDERHOLDER);
            softly.assertThat(targetPaymentProviderData.getAccountNumber()).isEqualTo(ANY_ACCOUNTNUMBER);
            softly.assertThat(targetPaymentProviderData.getBankCode()).isEqualTo(ANY_BANKCODE);
            softly.assertThat(targetPaymentProviderData.getBankName()).isEqualTo(ANY_BANKNAME);
            softly.assertThat(targetPaymentProviderData.getCountryCode()).isEqualTo(ANY_COUNTRYCODE);
            softly.assertThat(targetPaymentProviderData.getIban()).isEqualTo(ANY_IBAN);
            softly.assertThat(targetPaymentProviderData.getBic()).isEqualTo(ANY_BIC);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderDescription()).isEqualTo(ANY_PAYMENTPROVIDERDESCRIPTION);
            softly.assertThat(targetPaymentProviderData.getMatchingId()).isEqualTo(ANY_MATCHINGID);
            softly.assertThat(targetPaymentProviderData.getTransactionAmount()).isEqualTo(ANY_TRANSACTIONAMOUNT);
        }
    }

    @Test
    void updatePaymentProviderWithExistingTargetValues() {

        Payments target = Payments.builder()
                .paymentProviderData(PaymentProviderData.builder()
                        .externalId(ANOTHER_EXTERNALID)
                        .transactionAmount(ANOTHER_TRANSACTIONAMOUNT)
                        .billingAgreement(false)
                        .build())
                .paymentTransactionId(ANOTHER_TRANSACTIONALID)
                .build();

        PaymentUtil.updatePaymentProvider(SOURCE, target);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(target.getPaymentTransactionId()).isEqualTo(ANOTHER_TRANSACTIONALID);
            var targetPaymentProviderData = target.getPaymentProviderData();
            softly.assertThat(targetPaymentProviderData.getExternalId()).isEqualTo(ANOTHER_EXTERNALID);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderTransactionId()).isEqualTo(ANY_PAYMENTPROVIDERTRANSACTIONID);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderPaymentId()).isEqualTo(ANY_PAYMENTPROVIDERPAYMENTID);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderStatus()).isEqualTo(ANY_PAYMENTPROVIDERSTATUS);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderOrderDescription()).isEqualTo(ANY_PAYMENTPROVIDERORDERDESCRIPTION);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderCode()).isEqualTo(ANY_PAYMENTPROVIDERCODE);
            softly.assertThat(targetPaymentProviderData.getPseudoCardNumber()).isEqualTo(ANY_PSEUDOCARDNUMBER);
            softly.assertThat(targetPaymentProviderData.getCardExpiryDate()).isEqualTo(ANY_CARDEXPIRYDATE);
            softly.assertThat(targetPaymentProviderData.getCardBrand()).isEqualTo(ANY_CARDBRAND);
            softly.assertThat(targetPaymentProviderData.getOrderDescription()).isEqualTo(ANY_ORDERDESCRIPTION);
            softly.assertThat(targetPaymentProviderData.getExternalTransactionId()).isEqualTo(ANY_EXTERNALTRANSACTIONID);
            softly.assertThat(targetPaymentProviderData.getSenderFirstName()).isEqualTo(ANY_SENDERFIRSTNAME);
            softly.assertThat(targetPaymentProviderData.getSenderLastName()).isEqualTo(ANY_SENDERLASTNAME);
            softly.assertThat(targetPaymentProviderData.getExternalPaymentStatus()).isEqualTo(ANY_EXTERNALPAYMENTSTATUS);
            softly.assertThat(targetPaymentProviderData.getBillingAgreement()).isEqualTo(false);
            softly.assertThat(targetPaymentProviderData.getSenderHolder()).isEqualTo(ANY_SENDERHOLDER);
            softly.assertThat(targetPaymentProviderData.getAccountNumber()).isEqualTo(ANY_ACCOUNTNUMBER);
            softly.assertThat(targetPaymentProviderData.getBankCode()).isEqualTo(ANY_BANKCODE);
            softly.assertThat(targetPaymentProviderData.getBankName()).isEqualTo(ANY_BANKNAME);
            softly.assertThat(targetPaymentProviderData.getCountryCode()).isEqualTo(ANY_COUNTRYCODE);
            softly.assertThat(targetPaymentProviderData.getIban()).isEqualTo(ANY_IBAN);
            softly.assertThat(targetPaymentProviderData.getBic()).isEqualTo(ANY_BIC);
            softly.assertThat(targetPaymentProviderData.getPaymentProviderDescription()).isEqualTo(ANY_PAYMENTPROVIDERDESCRIPTION);
            softly.assertThat(targetPaymentProviderData.getMatchingId()).isEqualTo(ANY_MATCHINGID);
            softly.assertThat(targetPaymentProviderData.getTransactionAmount()).isEqualTo(ANOTHER_TRANSACTIONAMOUNT);
        }
    }
}