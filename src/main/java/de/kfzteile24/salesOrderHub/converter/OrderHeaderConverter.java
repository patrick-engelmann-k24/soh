package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.Header;
import de.kfzteile24.salesOrderHub.dto.order.header.Discount;
import de.kfzteile24.salesOrderHub.dto.order.header.Payment;
import de.kfzteile24.soh.order.dto.Address;
import de.kfzteile24.soh.order.dto.Customer;
import de.kfzteile24.soh.order.dto.Discounts;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.PaymentProviderData;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class OrderHeaderConverter implements Converter<OrderJSON, OrderHeader> {
    public static final String FULFILLED_BY_K24 = "K24";

    @NonNull
    private final CustomerTypeConverter customerTypeConverter;

    @NonNull
    private final TotalTaxesConverter totalTaxesConverter;

    @NonNull
    private final SurchargesConverter surchargesConverter;

/* Activate or delete this, depending on how to finally implement order[Number/Id/NumberWhm]
    private static final ThreadLocal<DateTimeFormatter> DATE_FORMATTER =
            ThreadLocal.withInitial(() ->
                    DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Europe/Berlin"))
            );
*/

    @Override
    public OrderHeader convert(OrderJSON source) {
        final var header = source.getOrderHeader();
//        final var orderDateTime = OffsetDateTime.parse(header.getOrderDatetime());

        return OrderHeader.builder()
                .salesChannel(header.getOrigin().getSalesChannel())
                .locale(header.getOrigin().getLocale())
                .orderDateTime(header.getOrderDatetime())
                .orderTimezone(header.getOrderTimezone())
                .orderCurrency(header.getOrderCurrency())
                //TODO: Finalize orderNumber/orderId/orderNumberWhm conversion, when it is clear how it should be implemented
                .orderId(header.getOrderId())
//                .orderNumber(MessageFormat.format("{0}-{1}",
//                        DATE_FORMATTER.get().format(orderDateTime), randomNumeric(8)))
                .orderNumber(header.getOrderNumber())
                .orderNumberWhm(header.getOrderNumber())
                .orderGroupId(null)
                .offerId(toUUIDOrNull(header.getOfferId()))
                .offerReferenceNumber(header.getOfferReferenceNumber())
                .orderFulfillment(FULFILLED_BY_K24)
                .customerFulfillmentPartialPreference(false)
                .customer(convertCustomer(header))
                .billingAddress(convertAddress(header.getBillingAddress()))
                .shippingAddresses(header.getShippingAddresses().stream()
                        .map(this::convertAddress)
                        .collect(toList()))
                .totals(convertTotals(source))
                .payments(header.getPayments().stream().map(this::convertPayment).collect(toList()))
                .discounts(header.getDiscounts().stream().map(this::convertDiscount).collect(toList()))
                .build();
    }

    private Customer convertCustomer(Header sourceHeader) {
        final var customer = sourceHeader.getCustomer();

        return Customer.builder()
                .customerId(toUUIDOrNull(customer.getCustomerId()))
                .contactId(toUUIDOrNull(customer.getContactId()))
                .customerType(customerTypeConverter.convert(customer.getCustomerType()))
                .customerEmail(customer.getCustomerEmail())
                .customerNumber(customer.getCustomerNumber())
                .vatTaxId(sourceHeader.getBillingAddress().getTaxNumber())
                .customerSegment(emptyList())
                .build();
    }

    private UUID toUUIDOrNull(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }

    private Address convertAddress(de.kfzteile24.salesOrderHub.dto.order.customer.Address source) {
        return Address.builder()
                .addressKey(parseInt(source.getAddressKey()))
                .addressFormat(source.getAddressFormat())
                .addressType(source.getAddressType())
                .company(source.getCompany())
                .salutation(source.getSalutation())
                .firstName(source.getFirstName())
                .lastName(source.getLastName())
                .phoneNumber(source.getPhoneNumber())
                .street1(source.getStreet1())
                .street2(source.getStreet2())
                .street3(source.getStreet3())
                .street4(source.getStreet4())
                .city(source.getCity())
                .zipCode(source.getZipCode())
                .countryRegionCode(source.getCountryRegionCode())
                .countryCode(source.getCountryCode())
                .build();
    }

    private Totals convertTotals(OrderJSON source) {
        final var totals = source.getOrderHeader().getTotals();

        return Totals.builder()
                .goodsTotalGross(totals.getGoodsTotalGross())
                .goodsTotalNet(totals.getGoodsTotalNet())
                .shippingCostGross(totals.getShippingTotalsGross().getTotal())
                .shippingCostNet(totals.getShippingTotalsNet().getTotal())
                .surcharges(surchargesConverter.convert(source.getOrderRows()))
                .totalDiscountGross(totals.getTotalDiscountGross())
                .totalDiscountNet(totals.getTotalDiscountNet())
                .grandTotalGross(totals.getGrandtotalGross())
                .grandTotalNet(totals.getGoodsTotalNet())
                .grandTotalTaxes(Arrays.stream(totals.getGrandtotalTaxes())
                        .map(totalTaxesConverter::convert)
                        .collect(toList()))
                .paymentTotal(totals.getPaymentTotal())
                .build();
    }

    private Payments convertPayment(Payment payment) {
        return Payments.builder()
                .type(payment.getType())
                .value(payment.getValue())
                .paymentTransactionId(toUUIDOrNull(payment.getPaymentTransactionId()))
                .paymentProviderData(convertPaymentProviderData(payment.getPaymentProviderData()))
                .build();
    }

    private PaymentProviderData convertPaymentProviderData(
            de.kfzteile24.salesOrderHub.dto.order.header.PaymentProviderData paymentProviderData) {
        return PaymentProviderData.builder()
                .externalId(paymentProviderData.getExternalId())
                .transactionAmount(paymentProviderData.getTransactionAmount())
                .code(paymentProviderData.getCode())
                .promotionIdentifier(paymentProviderData.getPromotionIdentifier())
                .paymentProviderTransactionId(paymentProviderData.getPaymentProviderTransactionId())
                .paymentProviderCode(paymentProviderData.getPaymentProviderCode())
                .paymentProviderPaymentId(paymentProviderData.getPaymentProviderPaymentId())
                .paymentProviderStatus(paymentProviderData.getPaymentProviderStatus())
                .cardBrand(paymentProviderData.getCardBrand())
                .merchantId(paymentProviderData.getMerchantId())
                .cardExpiryDate(paymentProviderData.getCardExpiryDate())
                .orderDescription(paymentProviderData.getOrderDescription())
                .pseudoCardNumber(paymentProviderData.getPseudoCardNumber())
                .paymentProviderOrderDescription(paymentProviderData.getPaymentProviderOrderDescription())
                .senderFirstName(paymentProviderData.getSenderFirstName())
                .senderLastName(paymentProviderData.getSenderLastName())
                .externalTransactionId(paymentProviderData.getExternalTransactionId())
                .externalPaymentStatus(paymentProviderData.getExternalPaymentStatus())
                .billingAgreement(paymentProviderData.getBillingAgreement())
                .iban(paymentProviderData.getIban())
                .bic(paymentProviderData.getBic())
                .senderHolder(paymentProviderData.getSenderHolder())
                .accountNumber(paymentProviderData.getAccountNumber())
                .bankCode(paymentProviderData.getBankCode())
                .bankName(paymentProviderData.getBankName())
                .countryCode(paymentProviderData.getCountryCode())
                .request(paymentProviderData.getRequest())
                .response(paymentProviderData.getResponse())
                .paymentProviderDescription(paymentProviderData.getPaymentProviderDescription())
                .matchingId(paymentProviderData.getMatchingId())
                .accountBank(paymentProviderData.getAccountBank())
                .accountOwner(paymentProviderData.getAccountOwner())
                .lastName(paymentProviderData.getSenderLastName())
                .firstName(paymentProviderData.getSenderFirstName())
                .orderNumber(null)
                .build();
    }

    private Discounts convertDiscount(Discount discount) {
        return Discounts.builder()
                .discountId(discount.getDiscountType())
                .discountValueGross(discount.getDiscountValueGross())
                .promotionIdentifier(discount.getPromotionIdentifier())
                .build();
    }
}
