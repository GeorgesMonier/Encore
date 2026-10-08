import { useCallback, useEffect, useState } from 'react';
import { Elements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import { api } from '../api.js';
import Icon from './Icon.jsx';
import PaymentMethodForm from './PaymentMethodForm.jsx';

const stripeKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = stripeKey ? loadStripe(stripeKey) : null;

export default function PaymentMethodSettings() {
  const [method, setMethod] = useState(null);
  const [clientSecret, setClientSecret] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadMethod = useCallback(async () => {
    setError('');
    try {
      const savedMethod = await api('/payments/payment-method');
      setMethod(savedMethod);
    } catch (requestError) {
      setError(requestError.message || 'No se pudo consultar el método de pago.');
    } finally {
      setLoading(false);
    }
  }, []);

  const confirmSetupIntent = useCallback(async (setupIntentId) => {
    setSubmitting(true);
    setError('');
    try {
      const savedMethod = await api('/payments/payment-method', {
        method: 'POST',
        body: JSON.stringify({ setupIntentId }),
      });
      setMethod(savedMethod);
      setClientSecret('');
      setMessage('Método de pago verificado y actualizado.');
    } catch (requestError) {
      setError(requestError.message || 'No se pudo guardar el método de pago.');
    } finally {
      setSubmitting(false);
    }
  }, []);

  useEffect(() => {
    const currentUrl = new URL(window.location.href);
    const setupIntentId = currentUrl.searchParams.get('setup_intent');
    const redirectStatus = currentUrl.searchParams.get('redirect_status');

    if (setupIntentId && redirectStatus === 'succeeded') {
      void confirmSetupIntent(setupIntentId).then(() => loadMethod());
      currentUrl.searchParams.delete('setup_intent');
      currentUrl.searchParams.delete('setup_intent_client_secret');
      currentUrl.searchParams.delete('redirect_status');
      window.history.replaceState({}, '', `${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`);
      return;
    }

    void loadMethod();
  }, [confirmSetupIntent, loadMethod]);

  async function startSetup() {
    setError('');
    setMessage('');
    setSubmitting(true);
    try {
      const intent = await api('/payments/setup-intent', { method: 'POST' });
      if (!intent.clientSecret) throw new Error('Stripe no devolvió la clave de verificación.');
      setClientSecret(intent.clientSecret);
    } catch (requestError) {
      setError(requestError.message || 'No se pudo iniciar la actualización.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="profile-card payment-card">
      <div className="profile-card-heading">
        <div><span className="section-kicker"><span /> PAGO SEGURO CON STRIPE</span><h2>Método de pago</h2></div>
        <Icon name="ticket" size={21} />
      </div>
      {loading ? <p className="payment-hint">Consultando Stripe…</p> : null}
      {!loading && method?.hasPaymentMethod && (
        <div className="saved-card">
          <div className="saved-card-brand"><Icon name="ticket" size={19} /></div>
          <div><strong>{method.brand?.toUpperCase() ?? 'TARJETA'} ···· {method.last4}</strong><span>Caduca {String(method.expMonth).padStart(2, '0')}/{method.expYear}</span></div>
          <span className="saved-default">PRINCIPAL</span>
        </div>
      )}
      {!loading && method && !method.hasPaymentMethod && !clientSecret && (
        <p className="payment-hint">Todavía no tienes una tarjeta guardada. Añade una para dejarla asociada de forma segura a tu cuenta.</p>
      )}
      {!loading && !clientSecret && stripePromise && (
        <button className="button button-dark payment-change-button" disabled={submitting} onClick={() => void startSetup()}>
          {submitting ? 'Preparando verificación…' : method?.hasPaymentMethod ? 'Actualizar tarjeta' : 'Añadir tarjeta'}
          {!submitting && <Icon name="arrow" size={15} />}
        </button>
      )}
      {!loading && !stripePromise && (
        <p className="inline-error">Añadir tarjeta requiere configurar la clave pública de Stripe del entorno de Encore.</p>
      )}
      {clientSecret && stripePromise && (
        <div className="payment-method-form-wrap">
          <p className="payment-hint">Introduce la nueva tarjeta. Stripe la verificará y reemplazará el método principal anterior.</p>
          <Elements stripe={stripePromise} options={{ clientSecret, appearance: { theme: 'stripe', variables: { colorPrimary: '#294be8', borderRadius: '0px' } } }}>
            <PaymentMethodForm onConfirmSetup={confirmSetupIntent} submitting={submitting} />
          </Elements>
          <button className="cancel-payment-setup" disabled={submitting} onClick={() => setClientSecret('')}>Cancelar</button>
        </div>
      )}
      {message && <p className="profile-save-message" role="status">{message}</p>}
      {error && <p className="inline-error" role="alert">{error}</p>}
      <p className="payment-hint payment-disclosure">Encore no recibe ni almacena el número completo de tarjeta. El método verificado se guarda en Stripe para futuras compras.</p>
    </section>
  );
}
