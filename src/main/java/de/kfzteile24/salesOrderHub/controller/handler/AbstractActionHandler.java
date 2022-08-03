package de.kfzteile24.salesOrderHub.controller.handler;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.dto.ErrorResponse;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public abstract class AbstractActionHandler {

    protected SnsPublishService snsPublishService;

    protected abstract Consumer<String> getAction();

    public abstract boolean supports(ActionType orderType);

    public List<ErrorResponse> applyAction(Collection<String> orderNumbers) {
        var errors = new ArrayList<ErrorResponse>();
        orderNumbers.forEach(orderNumber -> {
            try {
                getAction().accept(orderNumber);
            } catch (Exception e) {
                errors.add(ErrorResponse.builder()
                        .orderNumber(orderNumber)
                        .errorMessage(e.getLocalizedMessage())
                        .build());
            }
        });
        return errors;
    }

    @Autowired
    private void setSnsPublishService(SnsPublishService snsPublishService) {
        this.snsPublishService = snsPublishService;
    }
}
