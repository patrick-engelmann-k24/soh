package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SalesOrderReturnService {

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    public SalesOrderReturn getByOrderNumber(String orderNumber) {
        return salesOrderReturnRepository.findByOrderNumber(orderNumber);
    }

    public SalesOrderReturn save(SalesOrderReturn salesOrderReturn) {
        return salesOrderReturnRepository.save(salesOrderReturn);
    }

    public void updateUrl(String orderNumber, String url) {
        salesOrderReturnRepository.updateUrl(orderNumber, url);
    }
}
