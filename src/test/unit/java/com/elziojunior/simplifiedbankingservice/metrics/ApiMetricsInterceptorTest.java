package com.elziojunior.simplifiedbankingservice.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import io.micrometer.core.instrument.Timer;

class ApiMetricsInterceptorTest {

    /** Proves annotated handlers are timed and classified with the final response status. */
    @Test
    void shouldObserveAnnotatedHandler() throws Exception {
        ApiMetrics apiMetrics = mock(ApiMetrics.class);
        Timer.Sample sample = mock(Timer.Sample.class);
        when(apiMetrics.start()).thenReturn(sample);
        ApiMetricsInterceptor interceptor = new ApiMetricsInterceptor(apiMetrics);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);

        assertThat(interceptor.preHandle(request, response, handlerMethod("observed"))).isTrue();
        assertThat(ApiMetricsInterceptor.operation(request)).contains(ApiOperation.TRANSFER_CREATE);
        interceptor.afterCompletion(request, response, handlerMethod("observed"), null);

        verify(apiMetrics).recordOutcome(ApiOperation.TRANSFER_CREATE, 400, sample);
    }

    /** Proves non-controller and unannotated handlers do not create unbounded operation metrics. */
    @Test
    void shouldIgnoreHandlersWithoutObservedOperation() throws Exception {
        ApiMetrics apiMetrics = mock(ApiMetrics.class);
        ApiMetricsInterceptor interceptor = new ApiMetricsInterceptor(apiMetrics);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, handlerMethod("unobserved"))).isTrue();
        assertThat(ApiMetricsInterceptor.operation(request)).isEmpty();
        interceptor.afterCompletion(request, response, handlerMethod("unobserved"), null);

        verifyNoInteractions(apiMetrics);
    }

    /** Proves an unresolved exception is failed even before the servlet container assigns status 500. */
    @Test
    void shouldClassifyUnresolvedExceptionAsFailure() throws Exception {
        ApiMetrics apiMetrics = mock(ApiMetrics.class);
        Timer.Sample sample = mock(Timer.Sample.class);
        when(apiMetrics.start()).thenReturn(sample);
        ApiMetricsInterceptor interceptor = new ApiMetricsInterceptor(apiMetrics);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new IllegalStateException("unexpected");

        interceptor.preHandle(request, response, handlerMethod("observed"));
        interceptor.afterCompletion(request, response, handlerMethod("observed"), failure);

        verify(apiMetrics).recordOutcome(ApiOperation.TRANSFER_CREATE, 500, sample);
    }

    private HandlerMethod handlerMethod(String name) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(name);
        return new HandlerMethod(new TestController(), method);
    }

    private static final class TestController {

        @ObservedApiOperation(ApiOperation.TRANSFER_CREATE)
        void observed() {
        }

        void unobserved() {
        }
    }
}
