import { useEffect, useState } from 'react';
import { Elements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import { api } from '../api.js';
import { fallbackImages, formatDate, formatPrice, formatTime, optimizeImageUrl } from '../utils/events.js';
import Icon from './Icon.jsx';
import Modal from './Modal.jsx';
import StripeCheckout from './StripeCheckout.jsx';

const stripeKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = stripeKey ? loadStripe(stripeKey) : null;

export default function EventDialog({ event, user, onClose, onRequestAuth, onPaymentSuccess }) {
  const [ticketTypes, setTicketTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [demoMode, setDemoMode] = useState(null);
  const [quantities, setQuantities] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [clientSecret, setClientSecret] = useState('');
  const [currentOrderId, setCurrentOrderId] = useState('');
  const [paymentError, setPaymentError] = useState('');
  const [paid, setPaid] = useState(false);
  const [paymentPending, setPaymentPending] = useState(false);
  const [imageFailed, setImageFailed] = useState(false);

  useEffect(() => {
    let active = true;
    api('/payments/mode')
      .then((result) => {
        if (typeof result?.demoMode !== 'boolean') {
          throw new Error('La API devolvió un modo de pago no válido.');
        }
        if (active) setDemoMode(result.demoMode);
      })
      .catch((requestError) => {
        if (active) setPaymentError(requestError.message || 'No se pudo consultar el modo de pago.');
      });
    api(`/ticket-types/event/${event.id}`)
      .then((result) => {
        if (active) setTicketTypes(Array.isArray(result) ? result : []);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'No se pudieron cargar las entradas.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [event.id]);

  const selectedTickets = ticketTypes
    .filter((ticket) => (quantities[ticket.id] ?? 0) > 0)
    .map((ticket) => ({ ticketTypeId: ticket.id, quantity: quantities[ticket.id] }));
  const total = ticketTypes.reduce(
    (sum, ticket) => sum + Number(ticket.price) * (quantities[ticket.id] ?? 0),
    0,
  );

  async function reserve() {
    if (!user) {
      onRequestAuth();
      return;
    }
    if (demoMode === null) {
      setPaymentError('No se pudo determinar el modo de pago. Inténtalo de nuevo.');
      return;
    }
    if (!demoMode && !stripePromise) {
      setPaymentError('Configura VITE_STRIPE_PUBLISHABLE_KEY en el frontend para habilitar los pagos.');
      return;
    }
    if (!selectedTickets.length) return;

    setSubmitting(true);
    setPaymentError('');
    try {
      const order = await api('/orders', {
        method: 'POST',
        body: JSON.stringify({ items: selectedTickets }),
      });
      if (demoMode) {
        await api(`/payments/demo-confirm/${order.id}`, { method: 'POST' });
        setPaid(true);
        onPaymentSuccess(true);
        return;
      }
      setCurrentOrderId(order.id);
      const payment = await api(`/payments/create-intent/${order.id}`, { method: 'POST' });
      setClientSecret(payment.clientSecret);
    } catch (requestError) {
      setPaymentError(requestError.message || 'No se pudo iniciar la reserva.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handlePaymentSuccess(paymentIntentId) {
    try {
      const confirmation = await api(`/payments/confirm/${currentOrderId}`, {
        method: 'POST',
        body: JSON.stringify({ paymentIntentId }),
      });
      if (confirmation.status === 'PAID') {
        setPaid(true);
        onPaymentSuccess(false);
        return;
      }
      setPaymentPending(true);
      setClientSecret('');
      for (let attempt = 0; attempt < 20; attempt += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 1500));
        const order = await api(`/orders/${currentOrderId}`);
        if (order.status === 'PAID') {
          setPaymentPending(false);
          setPaid(true);
          onPaymentSuccess(false);
          return;
        }
      }
      setPaymentPending(false);
      setPaymentError('Stripe recibió el pago, pero aún esperamos la confirmación del webhook. Comprueba “Mis entradas” en unos segundos.');
    } catch (requestError) {
      setPaymentPending(false);
      setPaymentError(requestError.message || 'No se pudo confirmar el pago.');
    }
  }

  return (
    <Modal onClose={onClose} wide label={`Entradas para ${event.name}`}>
      <div className="event-dialog">
        <div className="dialog-cover">
          <img
            src={optimizeImageUrl(!imageFailed && event.imageUrl ? event.imageUrl : fallbackImages[0], 1200)}
            alt=""
            decoding="async"
            onError={() => setImageFailed(true)}
          />
          <div className="dialog-cover-shade" />
          <button className="back-link" onClick={onClose}><Icon name="back" size={16} /> Volver a conciertos</button>
          <div className="dialog-title">
            <span>
              {event.city || 'España'} · {formatDate(event.eventDate)}
              {formatTime(event.eventTime) && ` · ${formatTime(event.eventTime)}`}
            </span>
            <h2>{event.name}</h2>
            {event.artist && <p>{event.artist}</p>}
          </div>
        </div>
        <div className="dialog-body">
          <div className="event-about">
            <span className="section-kicker"><span /> DETALLES DEL CONCIERTO</span>
            <h3>{event.venue || 'Sala por confirmar'}</h3>
            <div className="event-detail-meta">
              <Icon name="calendar" size={17} />
              {formatDate(event.eventDate)}
              {formatTime(event.eventTime) && ` · ${formatTime(event.eventTime)}`}
            </div>
            {event.description && <p className="event-description">{event.description}</p>}
          </div>
          <div className="ticket-panel">
            <h3>{clientSecret ? 'Completa tu reserva' : paid ? demoMode ? '¡Compra demo lista!' : '¡Nos vemos allí!' : 'Elige tus entradas'}</h3>
            {demoMode && <p className="demo-payment-note">Entradas de demostración: los precios y el stock son ficticios. No se procesan pagos reales.</p>}
            {demoMode === false && !stripePromise && (
              <p className="inline-error">Falta configurar la clave pública de Stripe (VITE_STRIPE_PUBLISHABLE_KEY) para mostrar el formulario de pago.</p>
            )}
            {loading && <div className="ticket-loading">Buscando entradas disponibles…</div>}
            {error && <p className="inline-error">{error}</p>}
            {!loading && !error && !ticketTypes.length && <p className="ticket-empty">Todavía no hay entradas disponibles para este concierto.</p>}
            {!clientSecret && !paid && ticketTypes.map((ticket) => (
              <div className="ticket-row" key={ticket.id}>
                <div>
                  <strong>{ticket.name}</strong>
                  <span>{ticket.availableQuantity} disponibles · {ticket.soldQuantity ?? (ticket.totalQuantity - ticket.availableQuantity)} vendidas</span>
                </div>
                <div className="ticket-actions">
                  <b>{formatPrice(ticket.price)}</b>
                  <div className="stepper">
                    <button
                      aria-label={`Quitar una entrada ${ticket.name}`}
                      disabled={!(quantities[ticket.id] > 0)}
                      onClick={() => setQuantities((current) => ({ ...current, [ticket.id]: Math.max((current[ticket.id] ?? 0) - 1, 0) }))}
                    ><Icon name="minus" size={14} /></button>
                    <span>{quantities[ticket.id] ?? 0}</span>
                    <button
                      aria-label={`Añadir una entrada ${ticket.name}`}
                      disabled={quantities[ticket.id] >= ticket.availableQuantity}
                      onClick={() => setQuantities((current) => ({ ...current, [ticket.id]: (current[ticket.id] ?? 0) + 1 }))}
                    ><Icon name="plus" size={14} /></button>
                  </div>
                </div>
              </div>
            ))}
            {clientSecret && stripePromise && (
              <Elements stripe={stripePromise} options={{ clientSecret, appearance: { theme: 'stripe', variables: { colorPrimary: '#274be8', borderRadius: '4px' } } }}>
                <StripeCheckout orderId={currentOrderId} onPaid={handlePaymentSuccess} onError={setPaymentError} />
              </Elements>
            )}
            {paid && <div className="paid-state">{demoMode ? 'Compra de demostración confirmada. No se ha realizado ningún cargo.' : 'Pago confirmado. Te hemos enviado la confirmación de tu pedido.'}</div>}
            {paymentError && <p className="inline-error">{paymentError}</p>}
            {paymentPending && <p className="ticket-loading" role="status">Pago recibido por Stripe. Esperando la confirmación segura del servidor…</p>}
            {!clientSecret && !paid && !loading && !error && ticketTypes.length > 0 && (
              <div className="ticket-total">
                {total > 0 && <div className="total-line"><span>Total</span><strong>{formatPrice(total)}</strong></div>}
                <button className="button button-dark reserve-button" disabled={!selectedTickets.length || submitting || demoMode === null || (demoMode === false && !stripePromise)} onClick={() => void reserve()}>
                  {submitting ? 'Preparando tu reserva…' : !user ? 'Inicia sesión para reservar' : demoMode ? 'Confirmar compra demo' : 'Continuar al pago'}
                  {!submitting && <Icon name="arrow" size={17} />}
                </button>
                <span className="secure-note"><Icon name="ticket" size={14} /> {demoMode ? 'Modo demo · No se realizará ningún cargo' : 'Reserva segura · Pago protegido'}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
}
