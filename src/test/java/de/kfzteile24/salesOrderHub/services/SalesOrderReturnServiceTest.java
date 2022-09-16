package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderReturnServiceTest {

    @Mock
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Mock
    private CreditNoteNumberCounterService creditNoteNumberCounterService;

    @InjectMocks
    private SalesOrderReturnService salesOrderReturnService;

    @Test
    @SneakyThrows
    @DisplayName(("Create Credit Note Number When Latest Credit Note Number Is Empty"))
    public void testCreateCreditNoteNumberWhenLatestCreditNoteNumberIsEmpty() {
        var currentYear = 2022;
        when(creditNoteNumberCounterService.getNextCounter(currentYear)).thenReturn(1L);
        assertThat(salesOrderReturnService.createCreditNoteNumber(currentYear)).isEqualTo("2022200001");
    }

    @Test
    @SneakyThrows
    @DisplayName(("Create Credit Note Number When Latest Credit Note Number Is Not Empty"))
    public void testCreateCreditNoteNumberWhenLatestCreditNoteNumberIsNotEmpty() {
        var currentYear = 2022;
        when(creditNoteNumberCounterService.getNextCounter(currentYear)).thenReturn(2L);
        assertThat(salesOrderReturnService.createCreditNoteNumber(currentYear)).isEqualTo("2022200002");
    }
}
