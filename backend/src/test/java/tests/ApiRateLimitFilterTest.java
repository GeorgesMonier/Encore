package tests;

import com.encore.encoreapi.security.ApiRateLimitFilter;
import com.encore.encoreapi.security.ApiRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiRateLimitFilterTest {

    @Test
    void returns429AfterTenSupportRequestsFromSameAddress() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(new ApiRateLimitService());
        AtomicInteger handledRequests = new AtomicInteger();

        for (int requestNumber = 0; requestNumber < 11; requestNumber++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/support/ask");
            request.setRemoteAddr("203.0.113.10");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (servletRequest, servletResponse) -> handledRequests.incrementAndGet());

            if (requestNumber < 10) {
                assertEquals(200, response.getStatus());
            } else {
                assertEquals(429, response.getStatus());
                assertNotNull(response.getHeader("Retry-After"));
                assertEquals(10, handledRequests.get());
            }
        }
    }

    @Test
    void doesNotRateLimitStripeWebhook() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(new ApiRateLimitService());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments/webhook");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger handledRequests = new AtomicInteger();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> handledRequests.incrementAndGet());

        assertEquals(200, response.getStatus());
        assertEquals(1, handledRequests.get());
    }
}
