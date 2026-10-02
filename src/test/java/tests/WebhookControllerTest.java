package tests;

import com.encore.encoreapi.payment.WebhookController;
import com.encore.encoreapi.ticket.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock private OrderRepository orderRepository;

    @Test
    void rejectsPayloadWithInvalidSignature() {
        WebhookController controller = new WebhookController(orderRepository);
        ReflectionTestUtils.setField(controller, "webhookSecret", "whsec_test_secret");

        ResponseEntity<String> response = controller.handleWebhook(
                "{\"type\":\"payment_intent.succeeded\"}",
                "firma-invalida"
        );

        assertEquals(400, response.getStatusCode().value());
    }
}