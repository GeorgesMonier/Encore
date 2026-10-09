package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderRepository;
import com.encore.encoreapi.ticket.OrderStatus;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private final OrderRepository orderRepository;
    private final StripeRefundService stripeRefundService;

    public StripeWebhookService(
            OrderRepository orderRepository,
            StripeRefundService stripeRefundService) {
        this.orderRepository = orderRepository;
        this.stripeRefundService = stripeRefundService;
    }

    @Transactional
    public void confirmSuccessfulPayment(PaymentIntent paymentIntent) {
        String orderIdValue = paymentIntent.getMetadata() == null
                ? null : paymentIntent.getMetadata().get("orderId");
        UUID orderId;
        try {
            orderId = UUID.fromString(orderIdValue);
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.warn("Stripe succeeded event has no valid Encore order reference");
            return;
        }

        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            log.warn("Stripe succeeded event references an unknown Encore order");
            return;
        }

        long expectedAmount = order.getTotalAmount().movePointRight(2).longValueExact();
        String customerId = order.getUser().getStripeCustomerId();
        if (!"succeeded".equals(paymentIntent.getStatus())
                || paymentIntent.getAmount() == null
                || paymentIntent.getAmount() != expectedAmount
                || !"eur".equals(paymentIntent.getCurrency())
                || customerId == null
                || !customerId.equals(paymentIntent.getCustomer())) {
            log.warn("Stripe succeeded event did not match Encore order payment details");
            return;
        }

        String storedPaymentIntentId = order.getStripePaymentIntentId();
        if (order.getStatus() == OrderStatus.PAID) {
            if (Objects.equals(paymentIntent.getId(), storedPaymentIntentId)) {
                log.info("Duplicate Stripe payment event ignored for an already paid order");
            } else if (storedPaymentIntentId != null) {
                log.error("A second Stripe payment succeeded for a paid Encore order; refunding the duplicate");
                stripeRefundService.refundPaymentIntent(paymentIntent);
            } else {
                log.warn("Paid Encore order has no stored PaymentIntent; duplicate charge could not be verified");
            }
            return;
        }

        if (storedPaymentIntentId != null
                && !Objects.equals(paymentIntent.getId(), storedPaymentIntentId)) {
            log.error("Unexpected Stripe payment succeeded for an Encore order; refunding the unmatched payment");
            stripeRefundService.refundPaymentIntent(paymentIntent);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING || order.isExpired()) {
            log.error("Stripe payment succeeded after its Encore order was no longer payable; refunding it");
            stripeRefundService.refundPaymentIntent(paymentIntent);
            return;
        }

        if (storedPaymentIntentId == null) {
            order.setStripePaymentIntentId(paymentIntent.getId());
        }
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        log.info("Stripe webhook confirmed an Encore order");
    }
}
