package com.encore.encoreapi.ticket;

import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import com.encore.encoreapi.payment.PaymentMode;
import com.encore.encoreapi.payment.StripeRefundService;
import com.encore.encoreapi.payment.StripeCurrency;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final PaymentMode paymentMode;
    private final TicketPurchaseLimits purchaseLimits;
    private final StripeRefundService stripeRefundService;

    public OrderService(OrderRepository orderRepository,
                        TicketTypeRepository ticketTypeRepository,
                        UserRepository userRepository,
                        PaymentMode paymentMode,
                        TicketPurchaseLimits purchaseLimits,
                        StripeRefundService stripeRefundService) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.userRepository = userRepository;
        this.paymentMode = paymentMode;
        this.purchaseLimits = purchaseLimits;
        this.stripeRefundService = stripeRefundService;
    }

    @Transactional
    public Order createOrder(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos una entrada");
        }
        if (request.getIdempotencyKey() == null) {
            throw new IllegalArgumentException("La solicitud de compra debe incluir una clave de idempotencia");
        }

        Map<UUID, Long> requestedQuantities = new LinkedHashMap<>();
        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getTicketTypeId() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Cada entrada debe tener un tipo válido y una cantidad mayor que 0");
            }
            requestedQuantities.merge(itemReq.getTicketTypeId(), (long) itemReq.getQuantity(), Long::sum);
        }

        Optional<Order> previousAttempt = orderRepository.findByUserIdAndIdempotencyKey(
                userId, request.getIdempotencyKey());
        if (previousAttempt.isPresent()) {
            Order existingOrder = previousAttempt.get();
            if (!hasSameItems(existingOrder, requestedQuantities)) {
                throw new IllegalArgumentException("La clave de compra ya está asociada a otra selección de entradas");
            }
            if (existingOrder.getStatus() != OrderStatus.PENDING || existingOrder.isExpired()) {
                throw new IllegalArgumentException("La orden asociada a esta solicitud ya no está pendiente");
            }
            return existingOrder;
        }

        Map<PurchaseLimitKey, Long> previousPurchases = getPreviousPurchases(userId);
        Order order = new Order(user, BigDecimal.ZERO);
        order.setIdempotencyKey(request.getIdempotencyKey());
        BigDecimal total = BigDecimal.ZERO;
        String currency = null;

        for (Map.Entry<UUID, Long> requested : requestedQuantities.entrySet()) {
            TicketType ticketType = ticketTypeRepository.findById(requested.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de ticket no encontrado"));
            if (!paymentMode.isDemoMode() && ticketType.isDemoTicket()) {
                throw new IllegalArgumentException("Las entradas de demostración no se pueden comprar con Stripe");
            }
            long quantity = requested.getValue();

            PurchaseLimitKey limitKey = new PurchaseLimitKey(
                    ticketType.getEvent().getId(), ticketType.getCategory());
            long limit = purchaseLimits.forCategory(limitKey.category());
            long alreadyPurchased = previousPurchases.getOrDefault(limitKey, 0L);
            if (alreadyPurchased + quantity > limit) {
                String type = limitKey.category() == TicketCategory.VIP ? "VIP" : "NORMAL";
                throw new IllegalArgumentException(
                        "El límite es de " + limit + " entradas " + type + " por usuario y concierto");
            }

            if (currency != null && !currency.equals(ticketType.getCurrency())) {
                throw new IllegalArgumentException("Todas las entradas de una orden deben usar la misma moneda");
            }
            currency = ticketType.getCurrency();
            ticketType.reserve(Math.toIntExact(quantity));
            try {
                ticketTypeRepository.saveAndFlush(ticketType);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new IllegalArgumentException(
                        "Las entradas para " + ticketType.getName() + " se agotaron mientras comprabas. Inténtalo de nuevo.");
            }

            BigDecimal subtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            OrderItem orderItem = new OrderItem(ticketType, (int) quantity, ticketType.getPrice());
            order.addItem(orderItem);
        }

        order.setTotalAmount(total);
        order.setCurrency(currency);
        StripeCurrency.toMinorUnits(total, currency);
        return orderRepository.save(order);
    }

    private boolean hasSameItems(Order order, Map<UUID, Long> requestedQuantities) {
        Map<UUID, Long> existingQuantities = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            existingQuantities.merge(item.getTicketType().getId(), (long) item.getQuantity(), Long::sum);
        }
        return existingQuantities.equals(requestedQuantities);
    }

    private Map<PurchaseLimitKey, Long> getPreviousPurchases(UUID userId) {
        Map<PurchaseLimitKey, Long> purchases = new HashMap<>();
        for (Order existingOrder : orderRepository.findByUserId(userId)) {
            if (existingOrder.getStatus() == OrderStatus.CANCELLED
                    || existingOrder.getStatus() == OrderStatus.EXPIRED
                    || (existingOrder.getStatus() == OrderStatus.PENDING && existingOrder.isExpired())) {
                continue;
            }
            for (OrderItem item : existingOrder.getItems()) {
                PurchaseLimitKey key = new PurchaseLimitKey(
                        item.getTicketType().getEvent().getId(), item.getTicketType().getCategory());
                purchases.merge(key, (long) item.getQuantity(), Long::sum);
            }
        }
        return purchases;
    }

    private record PurchaseLimitKey(UUID eventId, TicketCategory category) {}

    @Transactional
    public OrderResponse confirmDemoPayment(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes permiso para confirmar esta orden");
        }
        if (order.getStatus() == OrderStatus.DEMO) {
            return OrderResponse.from(order);
        }
        if (order.getStatus() != OrderStatus.PENDING || order.isExpired()) {
            throw new IllegalArgumentException("La reserva ya no está pendiente o ha expirado");
        }
        for (OrderItem item : order.getItems()) {
            TicketType ticketType = item.getTicketType();
            ticketType.confirmReservation(item.getQuantity());
            ticketTypeRepository.save(ticketType);
        }
        order.setStatus(OrderStatus.DEMO);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public Order cancelOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.DEMO) {
            throw new IllegalArgumentException("Solo se pueden cancelar órdenes PENDING, PAID o DEMO");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            if (order.getStripePaymentIntentId() == null) {
                throw new IllegalStateException("No se puede cancelar una orden pagada sin referencia de Stripe");
            }
            stripeRefundService.refundPaymentIntent(order.getStripePaymentIntentId());
        }

        for (OrderItem item : order.getItems()) {
            TicketType ticketType = item.getTicketType();
            if (order.getStatus() == OrderStatus.PENDING) {
                ticketType.releaseReservation(item.getQuantity());
            } else {
                ticketType.releaseSold(item.getQuantity());
            }
            ticketTypeRepository.save(ticketType);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}