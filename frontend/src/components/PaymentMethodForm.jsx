import { useState } from 'react';
import { PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import Icon from './Icon.jsx';

export default function PaymentMethodForm({ onConfirmSetup, submitting }) {
  const stripe = useStripe();
  const elements = useElements();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    if (!stripe || !elements || confirming || submitting) return;
    setConfirming(true);
    setError('');
    try {
      const result = await stripe.confirmSetup({
        elements,
        confirmParams: { return_url: `${window.location.origin}/perfil` },
        redirect: 'if_required',
      });
      if (result.error) {
        setError(result.error.message || 'No se pudo verificar la tarjeta.');
      } else if (result.setupIntent?.status === 'succeeded' && result.setupIntent.id) {
        await onConfirmSetup(result.setupIntent.id);
      } else {
        setError('Stripe no confirmó la configuración de la tarjeta.');
      }
    } catch (requestError) {
      setError(requestError.message || 'No se pudo verificar la tarjeta.');
    } finally {
      setConfirming(false);
    }
  }

  return (
    <form className="stripe-form" onSubmit={(event) => void submit(event)}>
      <PaymentElement />
      {error && <p className="inline-error" role="alert">{error}</p>}
      <button className="button button-dark reserve-button" disabled={!stripe || confirming || submitting}>
        {confirming || submitting ? 'Verificando…' : 'Verificar y guardar tarjeta'}
        {!confirming && !submitting && <Icon name="arrow" size={16} />}
      </button>
    </form>
  );
}
