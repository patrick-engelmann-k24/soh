package de.kfzteile24.salesOrderHub.converter;

import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.Header;
import de.kfzteile24.salesOrderHub.dto.order.header.Discount;
import de.kfzteile24.salesOrderHub.dto.order.header.Payment;
import de.kfzteile24.soh.order.dto.Customer;
import de.kfzteile24.soh.order.dto.Discounts;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.Surcharges;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readOrderJson;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static de.kfzteile24.soh.order.dto.Platform.ECP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderHeaderConverterTest {
    @Spy
    private CustomerTypeConverter customerTypeConverter;

    @Mock
    private SurchargesConverter surchargesConverter;

    @Mock
    private TotalTaxesConverter totalTaxesConverter;

    @InjectMocks
    private OrderHeaderConverter orderHeaderConverter;

    @Test
    public void theOrderHeaderIsConvertedCorrectly() {
        final var expectedSurcharges = Surcharges.builder()
                .bulkyGoodsGross(BigDecimal.ONE)
                .riskyGoodsGross(BigDecimal.TEN)
                .build();
        when(surchargesConverter.convert(any())).thenReturn(expectedSurcharges);

        final var expectedTaxes = GrandTotalTaxes.builder()
                .value(BigDecimal.TEN)
                .rate(BigDecimal.ONE)
                .build();
        when(totalTaxesConverter.convert(any())).thenReturn(expectedTaxes);

        var orderJSON = readOrderJson("examples/completeOrder.json");
        orderJSON.getOrderHeader().getCustomer().setContactId(UUID.randomUUID().toString());

        final var convertedHeader = orderHeaderConverter.convert(orderJSON);
        final var originalHeader = orderJSON.getOrderHeader();

        assertThat(convertedHeader).isNotNull();
        assertThat(convertedHeader.getSalesChannel()).isEqualTo(originalHeader.getOrigin().getSalesChannel());
        assertThat(convertedHeader.getLocale()).isEqualTo(originalHeader.getOrigin().getLocale());
        assertThat(convertedHeader.getOrderDateTime()).isEqualTo(originalHeader.getOrderDatetime());
        assertThat(convertedHeader.getOrderTimezone()).isEqualTo(originalHeader.getOrderTimezone());
        assertThat(convertedHeader.getOrderCurrency()).isEqualTo(originalHeader.getOrderCurrency());
        assertThat(convertedHeader.getOrderId()).isEqualTo(originalHeader.getOrderId());
        assertThat(convertedHeader.getOrderNumber()).isEqualTo(originalHeader.getOrderNumber());
        assertThat(convertedHeader.getOrderNumberCore()).isEqualTo(originalHeader.getOrderNumber());
        assertThat(convertedHeader.getOrderNumberExternal()).isNull();
        assertThat(convertedHeader.getOrderGroupId()).isNull();
        assertThat(convertedHeader.getOfferId()).isEqualTo(UUID.fromString(originalHeader.getOfferId()));
        assertThat(convertedHeader.getOfferReferenceNumber()).isEqualTo(originalHeader.getOfferReferenceNumber());
        assertThat(convertedHeader.getOrderFulfillment()).isEqualTo(OrderHeaderConverter.FULFILLED_BY_K24);
        assertThat(convertedHeader.getCustomerFulfillmentPartialPreference()).isFalse();
        assertThat(convertedHeader.getPlatform()).isEqualTo(ECP);

        validateCustomer(orderJSON.getOrderHeader(), convertedHeader.getCustomer());

        validateBillingAddress(originalHeader.getBillingAddress(), convertedHeader.getBillingAddress());
        originalHeader.getShippingAddresses().forEach(originalAddress -> {
            final var convertedAddressOpt = convertedHeader.getShippingAddresses().stream()
                    .filter(a -> a.getAddressKey().equals(Integer.parseInt(originalAddress.getAddressKey())))
                    .findAny();

            assertThat(convertedAddressOpt.isPresent()).isTrue();
            assertThat(convertedAddressOpt.get().getRelayPhoneNumberConsent()).isFalse();
            validateShippingAddress(originalAddress, convertedAddressOpt.get());
        });

        validateTotals(originalHeader.getTotals(), convertedHeader.getTotals(),
                expectedSurcharges, List.of(expectedTaxes));

        assertThat(originalHeader.getPayments().size()).isEqualTo(convertedHeader.getPayments().size());
        for (int i = 0; i < originalHeader.getPayments().size(); i++) {
            validatePayment(originalHeader.getPayments().get(i), convertedHeader.getPayments().get(i));
        }

        assertThat(originalHeader.getDiscounts().size()).isEqualTo(convertedHeader.getDiscounts().size());
        for (int i = 0; i < originalHeader.getDiscounts().size(); i++) {
            validateDiscount(originalHeader.getDiscounts().get(i), convertedHeader.getDiscounts().get(i));
        }

        // Verify mocks were called as expected
        verify(customerTypeConverter).convert(originalHeader.getCustomer().getCustomerType());
        verify(surchargesConverter).convert(eq(orderJSON.getOrderRows()));
        Arrays.stream(originalHeader.getTotals().getGrandtotalTaxes())
                .forEach(totals -> verify(totalTaxesConverter).convert(totals));
    }

    @Test
    public void nullValuesAreHandledCorrectly() {
        var orderJSON = (OrderJSON) getSalesOrder(readResource("examples/ecpOrderMessage.json")).getOriginalOrder();
        final var convertedHeader = orderHeaderConverter.convert(orderJSON);

        assertThat(convertedHeader).isNotNull();
        assertThat(convertedHeader.getCustomer().getContactId()).isNull();
    }

    private void validateCustomer(Header originalHeader, Customer convertedCustomer) {
        final var originalCustomer = originalHeader.getCustomer();

        assertThat(convertedCustomer.getCustomerId()).isEqualTo(UUID.fromString(originalCustomer.getCustomerId()));
        assertThat(convertedCustomer.getContactId()).isEqualTo(UUID.fromString(originalCustomer.getContactId()));
        assertThat(convertedCustomer.getCustomerType().getType()).isEqualTo(originalCustomer.getCustomerType());
        assertThat(convertedCustomer.getCustomerNumber()).isEqualTo(originalCustomer.getCustomerNumber());
        assertThat(convertedCustomer.getCustomerNumberCore()).isNull();
        assertThat(convertedCustomer.getVatTaxId())
                .isEqualTo(originalHeader.getBillingAddress().getTaxNumber());
        assertThat(convertedCustomer.getCustomerSegment().isEmpty()).isTrue();
        assertThat(convertedCustomer.getCustomerEmail()).isEqualTo(originalCustomer.getCustomerEmail());
    }

    private void validateAddress(de.kfzteile24.salesOrderHub.dto.order.customer.Address originalAddress,
                                 de.kfzteile24.soh.order.dto.ShippingAddress convertedAddress) {
        assertThat(convertedAddress.getAddressKey()).isEqualTo(Integer.parseInt(originalAddress.getAddressKey()));
        assertThat(convertedAddress.getAddressFormat()).isEqualTo(originalAddress.getAddressFormat());
        assertThat(convertedAddress.getAddressType()).isEqualTo(originalAddress.getAddressType());
        assertThat(convertedAddress.getCompany()).isEqualTo(originalAddress.getCompany());
        assertThat(convertedAddress.getSalutation()).isEqualTo(originalAddress.getSalutation());
        assertThat(convertedAddress.getFirstName()).isEqualTo(originalAddress.getFirstName());
        assertThat(convertedAddress.getLastName()).isEqualTo(originalAddress.getLastName());
        assertThat(convertedAddress.getPhoneNumber()).isEqualTo(originalAddress.getPhoneNumber());
        assertThat(convertedAddress.getStreet1()).isEqualTo(originalAddress.getStreet1());
        assertThat(convertedAddress.getStreet2()).isEqualTo(originalAddress.getStreet2());
        assertThat(convertedAddress.getStreet3()).isEqualTo(originalAddress.getStreet3());
        assertThat(convertedAddress.getStreet4()).isEqualTo(originalAddress.getStreet4());
        assertThat(convertedAddress.getCity()).isEqualTo(originalAddress.getCity());
        assertThat(convertedAddress.getZipCode()).isEqualTo(originalAddress.getZipCode());
        assertThat(convertedAddress.getCountryRegionCode()).isEqualTo(originalAddress.getCountryRegionCode());
        assertThat(convertedAddress.getCountryCode()).isEqualTo(originalAddress.getCountryCode());
    }

    private void validateBillingAddress(de.kfzteile24.salesOrderHub.dto.order.customer.Address originalAddress,
                                 de.kfzteile24.soh.order.dto.BillingAddress convertedAddress) {
        assertThat(convertedAddress.getAddressKey()).isEqualTo(Integer.parseInt(originalAddress.getAddressKey()));
        assertThat(convertedAddress.getAddressFormat()).isEqualTo(originalAddress.getAddressFormat());
        assertThat(convertedAddress.getAddressType()).isEqualTo(originalAddress.getAddressType());
        assertThat(convertedAddress.getCompany()).isEqualTo(originalAddress.getCompany());
        assertThat(convertedAddress.getSalutation()).isEqualTo(originalAddress.getSalutation());
        assertThat(convertedAddress.getFirstName()).isEqualTo(originalAddress.getFirstName());
        assertThat(convertedAddress.getLastName()).isEqualTo(originalAddress.getLastName());
        assertThat(convertedAddress.getPhoneNumber()).isEqualTo(originalAddress.getPhoneNumber());
        assertThat(convertedAddress.getStreet1()).isEqualTo(originalAddress.getStreet1());
        assertThat(convertedAddress.getStreet2()).isEqualTo(originalAddress.getStreet2());
        assertThat(convertedAddress.getStreet3()).isEqualTo(originalAddress.getStreet3());
        assertThat(convertedAddress.getStreet4()).isEqualTo(originalAddress.getStreet4());
        assertThat(convertedAddress.getCity()).isEqualTo(originalAddress.getCity());
        assertThat(convertedAddress.getZipCode()).isEqualTo(originalAddress.getZipCode());
        assertThat(convertedAddress.getCountryRegionCode()).isEqualTo(originalAddress.getCountryRegionCode());
        assertThat(convertedAddress.getCountryCode()).isEqualTo(originalAddress.getCountryCode());
    }

    private void validateShippingAddress(de.kfzteile24.salesOrderHub.dto.order.customer.Address originalAddress,
                                        de.kfzteile24.soh.order.dto.ShippingAddress convertedAddress) {
        assertThat(convertedAddress.getAddressKey()).isEqualTo(Integer.parseInt(originalAddress.getAddressKey()));
        assertThat(convertedAddress.getAddressFormat()).isEqualTo(originalAddress.getAddressFormat());
        assertThat(convertedAddress.getAddressType()).isEqualTo(originalAddress.getAddressType());
        assertThat(convertedAddress.getCompany()).isEqualTo(originalAddress.getCompany());
        assertThat(convertedAddress.getSalutation()).isEqualTo(originalAddress.getSalutation());
        assertThat(convertedAddress.getFirstName()).isEqualTo(originalAddress.getFirstName());
        assertThat(convertedAddress.getLastName()).isEqualTo(originalAddress.getLastName());
        assertThat(convertedAddress.getPhoneNumber()).isEqualTo(originalAddress.getPhoneNumber());
        assertThat(convertedAddress.getStreet1()).isEqualTo(originalAddress.getStreet1());
        assertThat(convertedAddress.getStreet2()).isEqualTo(originalAddress.getStreet2());
        assertThat(convertedAddress.getStreet3()).isEqualTo(originalAddress.getStreet3());
        assertThat(convertedAddress.getStreet4()).isEqualTo(originalAddress.getStreet4());
        assertThat(convertedAddress.getCity()).isEqualTo(originalAddress.getCity());
        assertThat(convertedAddress.getZipCode()).isEqualTo(originalAddress.getZipCode());
        assertThat(convertedAddress.getCountryRegionCode()).isEqualTo(originalAddress.getCountryRegionCode());
        assertThat(convertedAddress.getCountryCode()).isEqualTo(originalAddress.getCountryCode());
    }


    private void validateTotals(de.kfzteile24.salesOrderHub.dto.order.header.Totals originalTotals,
                                de.kfzteile24.soh.order.dto.Totals convertedTotals,
                                Surcharges expectedSurcharges,
                                List<GrandTotalTaxes> expectedTaxes) {
        assertThat(convertedTotals.getGoodsTotalGross()).isEqualTo(originalTotals.getGoodsTotalGross());
        assertThat(convertedTotals.getGoodsTotalNet()).isEqualTo(originalTotals.getGoodsTotalNet());
        assertThat(convertedTotals.getShippingCostGross())
                .isEqualTo(originalTotals.getShippingTotalsGross().getTotal());
        assertThat(convertedTotals.getShippingCostNet()).isEqualTo(originalTotals.getShippingTotalsNet().getTotal());
        assertThat(convertedTotals.getSurcharges()).isEqualTo(expectedSurcharges);
        assertThat(convertedTotals.getTotalDiscountGross()).isEqualTo(originalTotals.getTotalDiscountGross());
        assertThat(convertedTotals.getTotalDiscountNet()).isEqualTo(originalTotals.getTotalDiscountNet());
        assertThat(convertedTotals.getGrandTotalGross()).isEqualTo(originalTotals.getGoodsTotalGross());
        assertThat(convertedTotals.getGrandTotalNet()).isEqualTo(originalTotals.getGoodsTotalNet());
        assertThat(convertedTotals.getGrandTotalTaxes()).isEqualTo(expectedTaxes);
        assertThat(convertedTotals.getPaymentTotal()).isEqualTo(originalTotals.getPaymentTotal());
    }

    private void validatePayment(Payment originalPayment, Payments convertedPayment) {
        assertThat(convertedPayment.getType()).isEqualTo(originalPayment.getType());
        assertThat(convertedPayment.getValue()).isEqualTo(originalPayment.getValue());
        assertThat(convertedPayment.getPaymentTransactionId())
                .isEqualTo(UUID.fromString(originalPayment.getPaymentTransactionId()));

        validatePaymentProviderData(originalPayment.getPaymentProviderData(),
                convertedPayment.getPaymentProviderData());
    }

    private void validatePaymentProviderData(
            de.kfzteile24.salesOrderHub.dto.order.header.PaymentProviderData originalPPD,
            de.kfzteile24.soh.order.dto.PaymentProviderData convertedPPD) {
        assertThat(convertedPPD.getExternalId()).isEqualTo(originalPPD.getExternalId());
        assertThat(convertedPPD.getTransactionAmount()).isEqualTo(originalPPD.getTransactionAmount());
        assertThat(convertedPPD.getCode()).isEqualTo(originalPPD.getCode());
        assertThat(convertedPPD.getPromotionIdentifier()).isEqualTo(originalPPD.getPromotionIdentifier());
        assertThat(convertedPPD.getPaymentProviderTransactionId())
                .isEqualTo(originalPPD.getPaymentProviderTransactionId());
        assertThat(convertedPPD.getPaymentProviderCode()).isEqualTo(originalPPD.getPaymentProviderCode());
        assertThat(convertedPPD.getPaymentProviderPaymentId()).isEqualTo(originalPPD.getPaymentProviderPaymentId());
        assertThat(convertedPPD.getPaymentProviderStatus()).isEqualTo(originalPPD.getPaymentProviderStatus());
        assertThat(convertedPPD.getCardBrand()).isEqualTo(originalPPD.getCardBrand());
        assertThat(convertedPPD.getMerchantId()).isEqualTo(originalPPD.getMerchantId());
        assertThat(convertedPPD.getCardExpiryDate()).isEqualTo(originalPPD.getCardExpiryDate());
        assertThat(convertedPPD.getOrderDescription()).isEqualTo(originalPPD.getOrderDescription());
        assertThat(convertedPPD.getPseudoCardNumber()).isEqualTo(originalPPD.getPseudoCardNumber());
        assertThat(convertedPPD.getPaymentProviderOrderDescription())
                .isEqualTo(originalPPD.getPaymentProviderOrderDescription());
        assertThat(convertedPPD.getSenderFirstName()).isEqualTo(originalPPD.getSenderFirstName());
        assertThat(convertedPPD.getSenderLastName()).isEqualTo(originalPPD.getSenderLastName());
        assertThat(convertedPPD.getExternalTransactionId()).isEqualTo(originalPPD.getExternalTransactionId());
        assertThat(convertedPPD.getExternalPaymentStatus()).isEqualTo(originalPPD.getExternalPaymentStatus());
        assertThat(convertedPPD.getBillingAgreement()).isEqualTo(originalPPD.getBillingAgreement());
        assertThat(convertedPPD.getIban()).isEqualTo(originalPPD.getIban());
        assertThat(convertedPPD.getBic()).isEqualTo(originalPPD.getBic());
        assertThat(convertedPPD.getSenderHolder()).isEqualTo(originalPPD.getSenderHolder());
        assertThat(convertedPPD.getAccountNumber()).isEqualTo(originalPPD.getAccountNumber());
        assertThat(convertedPPD.getBankCode()).isEqualTo(originalPPD.getBankCode());
        assertThat(convertedPPD.getBankName()).isEqualTo(originalPPD.getBankName());
        assertThat(convertedPPD.getCountryCode()).isEqualTo(originalPPD.getCountryCode());
        assertThat(convertedPPD.getRequest()).isEqualTo(originalPPD.getRequest());
        assertThat(convertedPPD.getResponse()).isEqualTo(originalPPD.getResponse());
        assertThat(convertedPPD.getPaymentProviderDescription()).isEqualTo(originalPPD.getPaymentProviderDescription());
        assertThat(convertedPPD.getMatchingId()).isEqualTo(originalPPD.getMatchingId());
        assertThat(convertedPPD.getAccountBank()).isEqualTo(originalPPD.getAccountBank());
        assertThat(convertedPPD.getAccountOwner()).isEqualTo(originalPPD.getAccountOwner());
        assertThat(convertedPPD.getLastName()).isEqualTo(originalPPD.getSenderLastName());
        assertThat(convertedPPD.getFirstName()).isEqualTo(originalPPD.getSenderFirstName());
        assertThat(convertedPPD.getCheckoutId()).isNull();
        assertThat(convertedPPD.getOrderNumber()).isNull();
        assertThat(convertedPPD.getReservationId()).isNull();
        assertThat(convertedPPD.getCustomerNumber()).isNull();
        assertThat(convertedPPD.getExpirationDate()).isNull();
        assertThat(convertedPPD.getBillingCountry()).isNull();
        assertThat(convertedPPD.getBankAccount()).isNull();
    }

    private void validateDiscount(Discount originalDiscount, Discounts convertedDiscount) {
        assertThat(convertedDiscount.getDiscountId()).isEqualTo(originalDiscount.getDiscountType());
        assertThat(convertedDiscount.getDiscountValueGross()).isEqualTo(originalDiscount.getDiscountValueGross());
        assertThat(convertedDiscount.getPromotionIdentifier()).isEqualTo(originalDiscount.getPromotionIdentifier());
    }
}