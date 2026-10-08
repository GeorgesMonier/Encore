import { useState } from 'react';
import { PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import Icon from './Icon.jsx';

export default function StripeCheckout({ orderId, onPaid, onError }) {
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!stripe || !elements) return;
    setSubmitting(true);
    onError('');
    const returnUrl = new URL(window.location.href);
    returnUrl.searchParams.set('order_id', orderId);
    try {
      const result = await stripe.confirmPayment({
        elements,
        confirmParams: { return_url: returnUrl.toString() },
        redirect: 'if_required',
      });
      if (result.error) {
        onError(result.error.message || 'No se pudo confirmar el pago.');
      } else if (result.paymentIntent?.status === 'succeeded') {
        onPaid(result.paymentIntent.id);
      }
    } catch (error) {
      onError(error instanceof Error ? error.message : 'No se pudo confirmar el pago.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="stripe-form" onSubmit={(event) => void handleSubmit(event)}>
      <PaymentElement />
      <button className="button button-dark reserve-button" disabled={!stripe || submitting}>
        {submitting ? 'Confirmando pago…' : 'Pagar y confirmar'}
        {!submitting && <Icon name="arrow" size={17} />}
      </button>
    </form>
  );
}
