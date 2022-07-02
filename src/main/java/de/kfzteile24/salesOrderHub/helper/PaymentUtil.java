package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.soh.order.dto.PaymentProviderData;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author samet
 */


@UtilityClass
public class PaymentUtil {
    private final List<String> MIGRATION_FIELDS = List.of(
            "externalId",
            "transactionAmount",
            "code",
            "promotionIdentifier",
            "remaingValue",
            "paymentProviderTransactionId",
            "paymentProviderPaymentId",
            "paymentProviderStatus",
            "paymentProviderOrderDescription",
            "paymentProviderCode",
            "pseudoCardNumber",
            "cardExpiryDate",
            "cardBrand",
            "merchantId",
            "paymentProviderXid",
            "orderDescription",
            "externalTransactionId",
            "senderFirstName",
            "senderLastName",
            "externalPaymentStatus",
            "billingAgreement",
            "senderHolder",
            "accountNumber",
            "bankCode",
            "bankName",
            "countryCode",
            "request",
            "response",
            "iban",
            "bic",
            "paymentProviderDescription",
            "matchingId",
            "accountBank",
            "accountOwner",
            "lastName",
            "firstName",
            "checkoutId",
            "orderNumber",
            "reservationId",
            "customerNummber",
            "expirationDate",
            "billingCountry",
            "bankAccount",
            "cashOnDeliveryCharge");

    public static Payments updatePaymentProvider(Payments source, Payments target) {
        var targetProvider = target.getPaymentProviderData();
        if (targetProvider == null) {
            targetProvider = PaymentProviderData.builder().build();
        }
        var sourceProvider = source.getPaymentProviderData();
        updateProviderDataFields(targetProvider, sourceProvider);
        updatePaymentTransactionId(source, target);
        target.setPaymentProviderData(targetProvider);
        return target;
    }

    private static void updatePaymentTransactionId(Payments source, Payments target) {
        if (target.getPaymentTransactionId() == null || target.getPaymentTransactionId().equals(new UUID(0, 0))) {
            target.setPaymentTransactionId(source.getPaymentTransactionId());
        }
    }

    private static void updateProviderDataFields(PaymentProviderData targetProvider, PaymentProviderData sourceProvider) {
        PropertyAccessor targetAccessor = PropertyAccessorFactory.forDirectFieldAccess(targetProvider);
        PropertyAccessor sourceAccessor = PropertyAccessorFactory.forDirectFieldAccess(sourceProvider);

        MIGRATION_FIELDS.forEach(field -> {
            var fieldType = targetAccessor.getPropertyType(field);

            if (String.class.equals(fieldType)) {
                updateString(targetAccessor, sourceAccessor, field);
            } else if (Boolean.class.equals(fieldType)) {
                updateBoolean(targetAccessor, sourceAccessor, field);
            } else if (BigDecimal.class.equals(fieldType)) {
                updateBigDecimal(targetAccessor, sourceAccessor, field);
            }
        });
    }

    private static void updateString(PropertyAccessor targetAccessor, PropertyAccessor sourceAccessor, String field) {
        String propertyValue = (String) targetAccessor.getPropertyValue(field);
        if (StringUtils.isBlank(propertyValue)) {
            targetAccessor.setPropertyValue(field, sourceAccessor.getPropertyValue(field));
        }
    }

    private static void updateBigDecimal(PropertyAccessor targetAccessor, PropertyAccessor sourceAccessor, String field) {
        BigDecimal propertyValue = (BigDecimal) targetAccessor.getPropertyValue(field);
        if (propertyValue == null || propertyValue.equals(BigDecimal.ZERO)) {
            targetAccessor.setPropertyValue(field, sourceAccessor.getPropertyValue(field));
        }
    }

    private static void updateBoolean(PropertyAccessor targetAccessor, PropertyAccessor sourceAccessor, String field) {
        Boolean propertyValue = (Boolean) targetAccessor.getPropertyValue(field);
        if (propertyValue == null) {
            targetAccessor.setPropertyValue(field, sourceAccessor.getPropertyValue(field));
        }
    }
}
