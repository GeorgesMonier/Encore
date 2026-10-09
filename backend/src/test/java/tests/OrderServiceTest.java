package tests;

import com.encore.encoreapi.ticket.*;
import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.payment.PaymentMode;
import com.encore.encoreapi.payment.StripeRefundService;
import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentMode paymentMode;
    @Mock private TicketPurchaseLimits purchaseLimits;
    @Mock private StripeRefundService stripeRefundService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private TicketType ticketType;
    private Event event;
    private UUID userId;
    private UUID ticketTypeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        event = new Event("concert-1", "Concert", null, null, null, null, null, null);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());

        user = new User("buyer@test.com", "hashed", "Buyer");
        ReflectionTestUtils.setField(user, "id", userId);
        ticketType = new TicketType(event, "General", new BigDecimal("40.00"), 10);
        ReflectionTestUtils.setField(ticketType, "id", ticketTypeId);
        lenient().when(purchaseLimits.forCategory(TicketCategory.NORMAL)).thenReturn(10);
        lenient().when(purchaseLimits.forCategory(TicketCategory.VIP)).thenReturn(5);
    }

    @Test
    void reservesStockAndComputesTotal() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(paymentMode.isDemoMode()).thenReturn(false);
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(ticketTypeRepository.saveAndFlush(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID());
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setTicketTypeId(ticketTypeId);
        item.setQuantity(3);
        request.setItems(List.of(item));

        Order order = orderService.createOrder(userId, request);

        assertEquals(new BigDecimal("120.00"), order.getTotalAmount());
        assertEquals(7, ticketType.getAvailableQuantity());
        assertEquals(3, ticketType.getReservedQuantity());
        assertEquals(0, ticketType.getSoldQuantity());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertTrue(order.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
    }

    @Test
    void confirmsDemoOrdersAsNonRevenue() {
        UUID orderId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        Order order = new Order(user, new BigDecimal("45.00"));
        ReflectionTestUtils.setField(order, "id", orderId);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.confirmDemoPayment(userId, orderId);

        assertEquals(OrderStatus.DEMO, response.status());
        assertEquals(new BigDecimal("45.00"), response.totalAmount());
        verify(orderRepository).save(order);
    }

    @Test
    void rejectsAnOrderContainingDifferentCurrencies() {
        TicketType usdTicket = new TicketType(
                event, "General USD", TicketCategory.NORMAL, "USD", new BigDecimal("40.00"), 10);
        UUID usdTicketTypeId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(ticketTypeRepository.findById(usdTicketTypeId)).thenReturn(Optional.of(usdTicket));
        when(paymentMode.isDemoMode()).thenReturn(false);
        when(ticketTypeRepository.saveAndFlush(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(UUID.randomUUID());
        request.setItems(List.of(orderItem(ticketTypeId, 1), orderItem(usdTicketTypeId, 1)));
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(userId, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void returnsTheExistingPendingOrderForTheSameIdempotencyKeyAndCart() {
        UUID requestKey = UUID.randomUUID();
        Order existingOrder = new Order(user, new BigDecimal("80.00"));
        existingOrder.setIdempotencyKey(requestKey);
        existingOrder.addItem(new OrderItem(ticketType, 2, ticketType.getPrice()));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(userId, requestKey))
                .thenReturn(Optional.of(existingOrder));

        Order retry = orderService.createOrder(userId, request(requestKey, ticketTypeId, 2));

        assertSame(existingOrder, retry);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(ticketTypeRepository);
    }

    @Test
    void rejectsReusingAnIdempotencyKeyWithDifferentTicketQuantities() {
        UUID requestKey = UUID.randomUUID();
        Order existingOrder = new Order(user, new BigDecimal("40.00"));
        existingOrder.setIdempotencyKey(requestKey);
        existingOrder.addItem(new OrderItem(ticketType, 1, ticketType.getPrice()));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(userId, requestKey))
                .thenReturn(Optional.of(existingOrder));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(userId, request(requestKey, ticketTypeId, 2)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void refundsPaidOrderBeforeReleasingItsSoldInventory() {
        Order order = new Order(user, new BigDecimal("40.00"));
        order.setStatus(OrderStatus.PAID);
        order.setStripePaymentIntentId("pi_paid");
        ticketType.reserve(1);
        ticketType.confirmReservation(1);
        order.addItem(new OrderItem(ticketType, 1, ticketType.getPrice()));
        when(orderRepository.findByIdForUpdate(any())).thenReturn(Optional.of(order));
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order cancelled = orderService.cancelOrder(UUID.randomUUID());

        verify(stripeRefundService).refundPaymentIntent("pi_paid");
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(10, ticketType.getAvailableQuantity());
        assertEquals(0, ticketType.getSoldQuantity());
    }

    @Test
    void enforcesGeneralTicketLimitAcrossOrders() {
        Order previousOrder = previousOrder(OrderStatus.PAID, "General", 8);
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of(previousOrder));
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(paymentMode.isDemoMode()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(userId, request(3)));
        verify(ticketTypeRepository, never()).saveAndFlush(any(TicketType.class));
    }

    @Test
    void aggregatesDuplicateLinesBeforeApplyingGeneralTicketLimit() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(paymentMode.isDemoMode()).thenReturn(false);

        CreateOrderRequest request = request(6, 5);
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(userId, request));
        verify(ticketTypeRepository, never()).saveAndFlush(any(TicketType.class));
    }

    @Test
    void enforcesVipLimitPerEvent() {
        TicketType vipTicket = new TicketType(event, "VIP", new BigDecimal("90.00"), 10);
        UUID vipTicketTypeId = UUID.randomUUID();
        ReflectionTestUtils.setField(vipTicket, "id", vipTicketTypeId);
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(ticketTypeRepository.findById(vipTicketTypeId)).thenReturn(Optional.of(vipTicket));
        when(paymentMode.isDemoMode()).thenReturn(false);
        when(ticketTypeRepository.saveAndFlush(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(userId, request(vipTicketTypeId, 5));

        assertEquals(5, order.getItems().get(0).getQuantity());
        assertEquals(5, vipTicket.getAvailableQuantity());
    }

    @Test
    void appliesConfiguredNormalLimitWhenReserving() {
        when(purchaseLimits.forCategory(TicketCategory.NORMAL)).thenReturn(2);
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(paymentMode.isDemoMode()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(userId, request(3)));
        verify(ticketTypeRepository, never()).saveAndFlush(any(TicketType.class));
    }

    @Test
    void reportsAnOptimisticInventoryConflictInsteadOfOverselling() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(paymentMode.isDemoMode()).thenReturn(false);
        when(ticketTypeRepository.saveAndFlush(ticketType)).thenThrow(
                new org.springframework.orm.ObjectOptimisticLockingFailureException(TicketType.class, ticketTypeId));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(userId, request(1)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsDemoInventoryWhenStripeIsActive() {
        TicketType demoTicket = new TicketType(event, "General (demo)", new BigDecimal("45.00"), 10);
        UUID demoTicketTypeId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(ticketTypeRepository.findById(demoTicketTypeId)).thenReturn(Optional.of(demoTicket));
        when(paymentMode.isDemoMode()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(userId, request(demoTicketTypeId, 1)));
        verify(ticketTypeRepository, never()).saveAndFlush(any(TicketType.class));
    }

    private Order previousOrder(OrderStatus status, String typeName, int quantity) {
        Order existingOrder = new Order(user, new BigDecimal("40.00").multiply(BigDecimal.valueOf(quantity)));
        existingOrder.setStatus(status);
        existingOrder.addItem(new OrderItem(ticketTypeFor(typeName), quantity, new BigDecimal("40.00")));
        return existingOrder;
    }

    private TicketType ticketTypeFor(String name) {
        return new TicketType(event, name, new BigDecimal("40.00"), 50);
    }

    private CreateOrderRequest request(int... quantities) {
        return request(ticketTypeId, quantities);
    }

    private CreateOrderRequest request(UUID typeId, int... quantities) {
        return request(UUID.randomUUID(), typeId, quantities);
    }

    private CreateOrderRequest request(UUID requestKey, UUID typeId, int... quantities) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(requestKey);
        request.setItems(java.util.Arrays.stream(quantities)
                .mapToObj(quantity -> orderItem(typeId, quantity))
                .toList());
        return request;
    }

    private CreateOrderRequest.OrderItemRequest orderItem(UUID typeId, int quantity) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setTicketTypeId(typeId);
        item.setQuantity(quantity);
        return item;
    }
}