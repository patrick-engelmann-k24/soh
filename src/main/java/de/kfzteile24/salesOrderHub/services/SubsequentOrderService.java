package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubsequentOrderService {

    @NonNull
    private final CamundaHelper camundaHelper;

    public void publishDropshipmentSubsequentOrderCreated(SalesOrder subsequentOrder) {
        camundaHelper.startDropshipmentSubsequentOrderCreatedProcess(subsequentOrder);
    }
}
