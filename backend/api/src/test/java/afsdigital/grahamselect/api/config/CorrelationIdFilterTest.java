package afsdigital.grahamselect.api.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldPropagateValidIncomingCorrelationIdAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/founders/slots");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "req-abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturedMdc = new AtomicReference<>();

        FilterChain chain = (req, res) -> capturedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);

        assertThat(capturedMdc.get()).isEqualTo("req-abc-123");
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("req-abc-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissingOrUnsafe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/founders/slots");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "bad\nheader");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String generated = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generated).isNotBlank().matches("^[A-Za-z0-9._-]{1,64}$");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
