package de.kfzteile24.salesOrderHub.helper;


import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@RequiredArgsConstructor
public class CustomValidator {

    private final Validator validator;

    public void validate(Object target) {

        Set<ConstraintViolation<Object>> violations = validator.validate(target);

        if (!violations.isEmpty()) {
            String orderNumber = getOrderNumber(target);

            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<Object> constraintViolation : violations) {
                sb.append(constraintViolation.getPropertyPath()).append(" ");
                sb.append(constraintViolation.getMessage());
            }

            if (orderNumber != null) {
                sb.append(" for orderNumber ").append(orderNumber);
            }

            throw new ConstraintViolationException("Constraint error occurred: " + sb, violations);
        }
    }

    private String getOrderNumber(Object target) {

        try {
            return ((Order) target).getOrderHeader().getOrderNumber();
        } catch (Exception ignored) {
            return null;
        }
    }
}