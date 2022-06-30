package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.constants.Payment;
import de.kfzteile24.soh.order.dto.PaymentProviderData;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author samet
 */


@UtilityClass
public class PaymentUtil {
    private static final String EXTERNAL_ID = "externalId";
    private static final String PAYMENT_PROVIDER_TRANSACTION_ID = "paymentProviderTransactionId";
    private static final String PAYMENT_PROVIDER_PAYMENT_ID = "paymentProviderPaymentId";
    private static final String PAYMENT_PROVIDER_STATUS = "paymentProviderStatus";
    private static final String PAYMENT_PROVIDER_ORDER_DESCRIPTION = "paymentProviderOrderDescription";
    private static final String PAYMENT_PROVIDER_CODE = "paymentProviderCode";
    private static final String PSEUDO_CARD_NUMBER = "pseudoCardNumber";
    private static final String CARD_EXPIRY_DATE = "cardExpiryDate";
    private static final String CARD_BRAND = "cardBrand";
    private static final String ORDER_DESCRIPTION = "orderDescription";
    private static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";
    private static final String SENDER_FIRST_NAME = "senderFirstName";
    private static final String SENDER_LAST_NAME = "senderLastName";
    private static final String EXTERNAL_PAYMENT_STATUS = "externalPaymentStatus";
    private static final String BILLING_AGREEMENT = "billingAgreement";
    private static final String SENDER_HOLDER = "senderHolder";
    private static final String ACCOUNT_NUMBER = "accountNumber";
    private static final String BANK_CODE = "bankCode";
    private static final String BANK_NAME = "bankName";
    private static final String COUNTRY_CODE = "countryCode";
    private static final String IBAN = "iban";
    private static final String BIC = "bic";
    private static final String PAYMENT_PROVIDER_DESCRIPTION = "paymentProviderDescription";
    private static final String MATCHING_ID = "matchingId";

    public static Payments updatePaymentProvider(Payments source, Payments target) {
        var targetProvider = target.getPaymentProviderData();
        if (targetProvider == null) {
            targetProvider = PaymentProviderData.builder().build();
        }
        var sourceProvider = source.getPaymentProviderData();
        switch (Payment.fromString(target.getType())) {
            case CREDIT_CARD:
            case STORED_CREDIT_CARD:
                updateProviderDataStringFields(targetProvider, sourceProvider,
                        EXTERNAL_ID,
                        PAYMENT_PROVIDER_TRANSACTION_ID,
                        PAYMENT_PROVIDER_PAYMENT_ID,
                        PAYMENT_PROVIDER_STATUS,
                        PAYMENT_PROVIDER_ORDER_DESCRIPTION,
                        PAYMENT_PROVIDER_CODE,
                        PSEUDO_CARD_NUMBER,
                        CARD_EXPIRY_DATE,
                        CARD_BRAND,
                        ORDER_DESCRIPTION);
                updateProviderDataTransactionAmount(targetProvider, sourceProvider);
                break;
            case PAYPAL:
                updateProviderDataStringFields(targetProvider, sourceProvider,
                        EXTERNAL_ID,
                        SENDER_FIRST_NAME,
                        SENDER_LAST_NAME,
                        EXTERNAL_TRANSACTION_ID,
                        EXTERNAL_PAYMENT_STATUS,
                        BILLING_AGREEMENT);
                updateProviderDataTransactionAmount(targetProvider, sourceProvider);
                break;
            case SOFORTUBERWEISUNG:
                updateProviderDataStringFields(targetProvider, sourceProvider,
                        EXTERNAL_ID,
                        PAYMENT_PROVIDER_TRANSACTION_ID,
                        SENDER_HOLDER,
                        ACCOUNT_NUMBER,
                        BANK_CODE,
                        BANK_NAME,
                        COUNTRY_CODE,
                        IBAN,
                        BIC);
                updateProviderDataTransactionAmount(targetProvider, sourceProvider);
                break;
            case IDEAL:
                updateProviderDataStringFields(targetProvider, sourceProvider,
                        EXTERNAL_ID,
                        EXTERNAL_TRANSACTION_ID,
                        PAYMENT_PROVIDER_TRANSACTION_ID,
                        PAYMENT_PROVIDER_PAYMENT_ID,
                        PAYMENT_PROVIDER_STATUS,
                        PAYMENT_PROVIDER_DESCRIPTION,
                        MATCHING_ID);
                updateProviderDataTransactionAmount(targetProvider, sourceProvider);
                break;
            default:
                break;
        }
        if (target.getPaymentTransactionId() == null || target.getPaymentTransactionId().equals(new UUID(0, 0))) {
            target.setPaymentTransactionId(source.getPaymentTransactionId());
        }
        target.setPaymentProviderData(targetProvider);
        return target;
    }

    private void updateProviderDataStringFields(PaymentProviderData targetProvider, PaymentProviderData sourceProvider, String... propertyList) {
        PropertyAccessor targetAccessor = PropertyAccessorFactory.forDirectFieldAccess(targetProvider);
        PropertyAccessor sourceAccessor = PropertyAccessorFactory.forDirectFieldAccess(sourceProvider);

        for (String property :propertyList) {
            if (StringUtils.isEmpty((String) targetAccessor.getPropertyValue(property))) {
                targetAccessor.setPropertyValue(property, sourceAccessor.getPropertyValue(property));
            }
        }
    }

    private void updateProviderDataTransactionAmount(PaymentProviderData targetProvider, PaymentProviderData sourceProvider) {
        PropertyAccessor targetAccessor = PropertyAccessorFactory.forDirectFieldAccess(targetProvider);
        PropertyAccessor sourceAccessor = PropertyAccessorFactory.forDirectFieldAccess(sourceProvider);
        String propertyName = "transactionAmount";

        BigDecimal propertyValue = (BigDecimal) targetAccessor.getPropertyValue(propertyName);
        if (propertyValue == null || propertyValue.equals(BigDecimal.ZERO)) {
            targetAccessor.setPropertyValue(propertyName, sourceAccessor.getPropertyValue(propertyName));
        }
    }
}
