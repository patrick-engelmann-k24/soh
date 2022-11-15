package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolationException;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CustomValidatorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomValidator customValidator;

    @Test
    void testCustomValidator() {

        var message = getObjectByResource("ecpOrderMessage.json", Order.class);

        assertDoesNotThrow(() -> customValidator.validate(message));
    }

    @Test
    void testCustomValidatorInvalidOrderJson() {

        var message = getObjectByResource("invalidJsonMessage.json", Order.class);

        assertThatThrownBy(() -> customValidator.validate(message)).isInstanceOf(ConstraintViolationException.class);
    }
}
