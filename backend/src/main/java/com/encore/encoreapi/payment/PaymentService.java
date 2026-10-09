package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderStatus;
import com.encore.encoreapi.ticket.OrderRepository;
import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

@Service
public class PaymentService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public PaymentService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PaymentIntent createPaymentIntent(Order order, User user) throws StripeException {
        Order lockedOrder = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
        if (lockedOrder.getStatus() != OrderStatus.PENDING || lockedOrder.isExpired()) {
            throw new IllegalArgumentException("La reserva ya no está pendiente o ha expirado");
        }
        String customerId = getOrCreateCustomer(user);
        long amountInMinorUnits = StripeCurrency.toMinorUnits(
                lockedOrder.getTotalAmount(), lockedOrder.getCurrency());

        if (lockedOrder.getStripePaymentIntentId() != null) {
            PaymentIntent existingIntent = PaymentIntent.retrieve(lockedOrder.getStripePaymentIntentId());
            if (!isPaymentIntentForOrder(existingIntent, lockedOrder, customerId, amountInMinorUnits)
                    || "canceled".equals(existingIntent.getStatus())) {
                throw new IllegalArgumentException("El intento de pago anterior no se puede reutilizar");
            }
            return existingIntent;
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInMinorUnits)
                .setCurrency(lockedOrder.getCurrency().toLowerCase(Locale.ROOT))
                .setCustomer(customerId)
                .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                .putMetadata("orderId", lockedOrder.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params, RequestOptions.builder()
                .setIdempotencyKey("encore-order-" + lockedOrder.getId())
                .build());
        lockedOrder.setStripePaymentIntentId(intent.getId());
        orderRepository.save(lockedOrder);
        return intent;
    }

    public void verifyPaymentIntent(Order order, User user, String paymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        long expectedAmount = StripeCurrency.toMinorUnits(order.getTotalAmount(), order.getCurrency());
        String customerId = user.getStripeCustomerId();
        var metadata = intent.getMetadata();

        if (!"succeeded".equals(intent.getStatus())
                || !Objects.equals(paymentIntentId, order.getStripePaymentIntentId())
                || metadata == null
                || !order.getId().toString().equals(metadata.get("orderId"))
                || customerId == null || customerId.isBlank()
                || !Objects.equals(customerId, intent.getCustomer())
                || intent.getCurrency() == null
                || !Objects.equals(order.getCurrency(), intent.getCurrency().toUpperCase(Locale.ROOT))
                || !Objects.equals(expectedAmount, intent.getAmount())) {
            throw new IllegalArgumentException("Stripe no confirmó un pago válido para esta reserva");
        }
    }

    private boolean isPaymentIntentForOrder(PaymentIntent intent, Order order, String customerId,
                                            long amountInMinorUnits) {
        return order.getId().toString().equals(
                    intent.getMetadata() == null ? null : intent.getMetadata().get("orderId"))
                && Objects.equals(customerId, intent.getCustomer())
                && Objects.equals(amountInMinorUnits, intent.getAmount())
                && intent.getCurrency() != null
                && Objects.equals(order.getCurrency(), intent.getCurrency().toUpperCase(Locale.ROOT));
    }

    @Transactional
    public SetupIntent createPaymentMethodSetup(User user) throws StripeException {
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(getOrCreateCustomer(user))
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .addPaymentMethodType("card")
                .build();
        return SetupIntent.create(params);
    }

    public SavedPaymentMethod getDefaultPaymentMethod(User user) throws StripeException {
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
            return SavedPaymentMethod.none();
        }

        Customer customer = Customer.retrieve(user.getStripeCustomerId());
        if (customer.getInvoiceSettings() == null
                || customer.getInvoiceSettings().getDefaultPaymentMethod() == null) {
            return SavedPaymentMethod.none();
        }

        PaymentMethod paymentMethod = PaymentMethod.retrieve(
                customer.getInvoiceSettings().getDefaultPaymentMethod());
        PaymentMethod.Card card = paymentMethod.getCard();
        if (card == null) {
            return SavedPaymentMethod.none();
        }
        return new SavedPaymentMethod(true, card.getBrand(), card.getLast4(),
                card.getExpMonth(), card.getExpYear());
    }

    public SavedPaymentMethod saveDefaultPaymentMethod(User user, String setupIntentId) throws StripeException {
        String customerId = user.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("No hay un cliente de Stripe asociado a esta cuenta");
        }

        SetupIntent setupIntent = SetupIntent.retrieve(setupIntentId);
        if (!"succeeded".equals(setupIntent.getStatus())) {
            throw new IllegalArgumentException("El método de pago no se ha verificado correctamente");
        }
        if (!customerId.equals(setupIntent.getCustomer())) {
            throw new IllegalArgumentException("El método de pago no pertenece a esta cuenta");
        }
        if (setupIntent.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Stripe no devolvió un método de pago verificado");
        }

        Customer customer = Customer.retrieve(customerId);
        CustomerUpdateParams params = CustomerUpdateParams.builder()
                .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(setupIntent.getPaymentMethod())
                        .build())
                .build();
        customer.update(params);
        return getDefaultPaymentMethod(user);
    }

    private String getOrCreateCustomer(User user) throws StripeException {
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (lockedUser.getStripeCustomerId() != null && !lockedUser.getStripeCustomerId().isBlank()) {
            return lockedUser.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(lockedUser.getEmail())
                .setName(lockedUser.getName())
                .putMetadata("encoreUserId", lockedUser.getId().toString())
                .build();
        Customer customer = Customer.create(params);
        lockedUser.setStripeCustomerId(customer.getId());
        userRepository.save(lockedUser);
        return customer.getId();
    }

    public record SavedPaymentMethod(
            boolean hasPaymentMethod,
            String brand,
            String last4,
            Long expMonth,
            Long expYear
    ) {
        public static SavedPaymentMethod none() {
            return new SavedPaymentMethod(false, null, null, null, null);
        }
    }
}