package tests;

import com.encore.encoreapi.ticket.*;
import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @InjectMocks
    private OrderService orderService;

    private User user;
    private TicketType ticketType;
    private UUID userId;
    private UUID ticketTypeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();

        user = new User("buyer@test.com", "hashed", "Buyer");
        ticketType = new TicketType(null, "General", new BigDecimal("40.00"), 10);
    }

    @Test
    void reservesStockAndComputesTotal() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
        when(ticketTypeRepository.saveAndFlush(any(TicketType.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setTicketTypeId(ticketTypeId);
        item.setQuantity(3);
        request.setItems(List.of(item));

        Order order = orderService.createOrder(userId, request);

        assertEquals(new BigDecimal("120.00"), order.getTotalAmount());
        assertEquals(7, ticketType.getAvailableQuantity());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }
}