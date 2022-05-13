package de.kfzteile24.salesOrderHub.services.splitter.decorator;


import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * This class splits up set items into single items
 */
@Service
@RequiredArgsConstructor
public class ItemSplitService extends AbstractSplitDecorator {

    @Override
    /**
     * This method splits up set items into single items
     */
    public void processOrderList(ArrayList<Order> orderList) {
        //TODO: implement in a different story
    }

}
