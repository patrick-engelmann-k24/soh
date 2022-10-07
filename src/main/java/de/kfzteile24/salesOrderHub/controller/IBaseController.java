package de.kfzteile24.salesOrderHub.controller;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletResponse;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.REQUEST_ID_KEY;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TRACE_ID_NAME;

public interface IBaseController {

    @ModelAttribute
    default void setResponseHeader(HttpServletResponse response) {
        response.addHeader(REQUEST_ID_KEY, MDC.get(TRACE_ID_NAME));
    }
}
