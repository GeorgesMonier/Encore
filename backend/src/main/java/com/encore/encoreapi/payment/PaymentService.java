package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderStatus;
import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class PaymentService {

    private final UserRepository userRepository;

    public PaymentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PaymentIntent createPaymentIntent(Order order, User user) throws StripeException {
        if (order.getStatus() != OrderStatus.PENDING || order.isExpired()) {
            throw new IllegalArgumentException("La reserva ya no está pendiente o ha expirado");
        }
        String customerId = getOrCreateCustomer(user);
        // Stripe trabaja en la unidad mínima de la moneda (céntimos para EUR)
        long amountInCents = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur")
                .setCustomer(customerId)
                .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                .putMetadata("orderId", order.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    public void verifyPaymentIntent(Order order, User user, String paymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        long expectedAmount = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
        String customerId = user.getStripeCustomerId();
        var metadata = intent.getMetadata();

        if (!"succeeded".equals(intent.getStatus())
                || metadata == null
                || !order.getId().toString().equals(metadata.get("orderId"))
                || customerId == null || customerId.isBlank()
                || !Objects.equals(customerId, intent.getCustomer())
                || !Objects.equals("eur", intent.getCurrency())
                || !Objects.equals(expectedAmount, intent.getAmount())) {
            throw new IllegalArgumentException("Stripe no confirmó un pago válido para esta reserva");
        }
    }

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
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getName())
                .putMetadata("encoreUserId", user.getId().toString())
                .build();
        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
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