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
    //TODO: implement in a different story
    public void processOrderList(ArrayList<Order> orderList) {

    }

}
