package tests;

import com.encore.encoreapi.payment.StripeWebhookService;
import com.encore.encoreapi.payment.StripeRefundService;
import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderItem;
import com.encore.encoreapi.ticket.OrderRepository;
import com.encore.encoreapi.ticket.OrderStatus;
import com.encore.encoreapi.ticket.TicketType;
import com.encore.encoreapi.ticket.TicketTypeRepository;
import com.encore.encoreapi.user.User;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private StripeRefundService stripeRefundService;

    @Test
    void confirmsAValidPaymentOnceWhenStripeDeliversDuplicateEvents() {
        UUID orderId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setStripeCustomerId("cus_test");
        Order order = new Order(user, new BigDecimal("45.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setStripePaymentIntentId("pi_test");

        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test");
        intent.setStatus("succeeded");
        intent.setAmount(4500L);
        intent.setCurrency("eur");
        intent.setCustomer("cus_test");
        intent.setMetadata(Map.of("orderId", orderId.toString()));

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        StripeWebhookService service = new StripeWebhookService(
                orderRepository, ticketTypeRepository, stripeRefundService);

        service.confirmSuccessfulPayment(intent);
        service.confirmSuccessfulPayment(intent);

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void doesNotConfirmPaymentWithTheWrongAmount() {
        UUID orderId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setStripeCustomerId("cus_test");
        Order order = new Order(user, new BigDecimal("45.00"));
        ReflectionTestUtils.setField(order, "id", orderId);
        order.setStripePaymentIntentId("pi_test");

        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test");
        intent.setStatus("succeeded");
        intent.setAmount(4501L);
        intent.setCurrency("eur");
        intent.setCustomer("cus_test");
        intent.setMetadata(Map.of("orderId", orderId.toString()));

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        StripeWebhookService service = new StripeWebhookService(
                orderRepository, ticketTypeRepository, stripeRefundService);

        service.confirmSuccessfulPayment(intent);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(stripeRefundService);
    }

    @Test
    void refundsASecondSuccessfulPaymentForAnAlreadyPaidOrder() {
        UUID orderId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setStripeCustomerId("cus_test");
        Order order = new Order(user, new BigDecimal("45.00"));
        order.setStatus(OrderStatus.PAID);
        order.setStripePaymentIntentId("pi_original");

        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_duplicate");
        intent.setStatus("succeeded");
        intent.setAmount(4500L);
        intent.setCurrency("eur");
        intent.setCustomer("cus_test");
        intent.setMetadata(Map.of("orderId", orderId.toString()));

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        StripeWebhookService service = new StripeWebhookService(
                orderRepository, ticketTypeRepository, stripeRefundService);

        service.confirmSuccessfulPayment(intent);

        verify(stripeRefundService).refundPaymentIntent(intent);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void refundsAValidPaymentForAnExpiredOrder() {
        UUID orderId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setStripeCustomerId("cus_test");
        Order order = new Order(user, new BigDecimal("45.00"));
        order.setStripePaymentIntentId("pi_test");
        ReflectionTestUtils.setField(order, "expiresAt", java.time.LocalDateTime.now().minusMinutes(1));

        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test");
        intent.setStatus("succeeded");
        intent.setAmount(4500L);
        intent.setCurrency("eur");
        intent.setCustomer("cus_test");
        intent.setMetadata(Map.of("orderId", orderId.toString()));

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        StripeWebhookService service = new StripeWebhookService(
                orderRepository, ticketTypeRepository, stripeRefundService);

        service.confirmSuccessfulPayment(intent);

        verify(stripeRefundService).refundPaymentIntent(intent);
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void convertsReservedInventoryToSoldAfterVerifiedPayment() {
        UUID orderId = UUID.randomUUID();
        User user = new User("buyer@test.com", "hashed", "Buyer");
        user.setStripeCustomerId("cus_test");
        Order order = new Order(user, new BigDecimal("45.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setStripePaymentIntentId("pi_test");
        TicketType ticketType = new TicketType(
                new com.encore.encoreapi.event.Event("concert-1", "Concert", null, null, null, null, null, null),
                "NORMAL", new BigDecimal("45.00"), 10);
        ticketType.reserve(1);
        order.addItem(new OrderItem(ticketType, 1, new BigDecimal("45.00")));

        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test");
        intent.setStatus("succeeded");
        intent.setAmount(4500L);
        intent.setCurrency("eur");
        intent.setCustomer("cus_test");
        intent.setMetadata(Map.of("orderId", orderId.toString()));

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(ticketTypeRepository.save(ticketType)).thenReturn(ticketType);
        when(orderRepository.save(order)).thenReturn(order);

        new StripeWebhookService(orderRepository, ticketTypeRepository, stripeRefundService)
                .confirmSuccessfulPayment(intent);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(0, ticketType.getReservedQuantity());
        assertEquals(1, ticketType.getSoldQuantity());
        assertEquals(9, ticketType.getAvailableQuantity());
        verify(ticketTypeRepository).save(ticketType);
    }
}
