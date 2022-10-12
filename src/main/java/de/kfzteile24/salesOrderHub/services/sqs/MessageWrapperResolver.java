package de.kfzteile24.salesOrderHub.services.sqs;

import lombok.NonNull;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

@Configuration
public class MessageWrapperResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ClassUtils.isAssignable(parameter.getParameterType(), MessageWrapper.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, @NonNull Message<?> message) throws Exception {
        return MessageWrapper.fromMessage(message);
    }
}
