package tests;

import com.encore.encoreapi.security.ApiRateLimitService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRateLimitServiceTest {

    @Test
    void blocksRequestsAfterLimitIsReached() {
        ApiRateLimitService service = new ApiRateLimitService();

        assertTrue(service.tryAcquire("client", 2).allowed());
        assertTrue(service.tryAcquire("client", 2).allowed());

        ApiRateLimitService.Decision blocked = service.tryAcquire("client", 2);
        assertFalse(blocked.allowed());
        assertTrue(blocked.retryAfterSeconds() > 0);
    }

    @Test
    void tracksClientsIndependently() {
        ApiRateLimitService service = new ApiRateLimitService();

        assertTrue(service.tryAcquire("client-a", 1).allowed());
        assertTrue(service.tryAcquire("client-b", 1).allowed());
    }
}
