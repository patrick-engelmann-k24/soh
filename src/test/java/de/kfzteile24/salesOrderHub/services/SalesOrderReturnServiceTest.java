package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderReturnServiceTest {

    @Mock
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @InjectMocks
    private SalesOrderReturnService salesOrderReturnService;

    @Test
    @SneakyThrows
    public void testCreateCreditNotNumber() {

        when(salesOrderReturnService.findLatestCreditNoteNumber()).thenReturn(Optional.empty());
        assertThat(salesOrderReturnService.getNextCreditNoteCount()).isEqualTo("00001");

        when(salesOrderReturnService.findLatestCreditNoteNumber()).thenReturn(Optional.of("2022200001"));
        assertThat(salesOrderReturnService.getNextCreditNoteCount()).isEqualTo("00002");
    }
}
