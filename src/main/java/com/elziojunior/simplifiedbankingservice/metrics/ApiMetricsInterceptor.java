package com.elziojunior.simplifiedbankingservice.metrics;

import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Observes API requests before Spring resolves controller arguments and validation. */
@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final String CONTEXT_ATTRIBUTE = ApiMetricsInterceptor.class.getName() + ".context";

    private final ApiMetrics apiMetrics;

    public ApiMetricsInterceptor(ApiMetrics apiMetrics) {
        this.apiMetrics = apiMetrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            ObservedApiOperation observed = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), ObservedApiOperation.class);
            if (observed != null) {
                request.setAttribute(CONTEXT_ATTRIBUTE, new RequestContext(observed.value(), apiMetrics.start()));
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        Object context = request.getAttribute(CONTEXT_ATTRIBUTE);
        if (context instanceof RequestContext requestContext) {
            int status = exception == null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            apiMetrics.recordOutcome(requestContext.operation(), status, requestContext.sample());
        }
    }

    /** Returns the bounded operation selected before controller argument resolution. */
    public static Optional<ApiOperation> operation(HttpServletRequest request) {
        Object context = request.getAttribute(CONTEXT_ATTRIBUTE);
        return context instanceof RequestContext requestContext
                ? Optional.of(requestContext.operation())
                : Optional.empty();
    }

    private record RequestContext(ApiOperation operation, Timer.Sample sample) {
    }
}
