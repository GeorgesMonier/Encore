import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { formatDate, formatPrice } from '../utils/events.js';
import Icon from './Icon.jsx';
import Modal from './Modal.jsx';

const statusLabels = {
  DEMO: 'DEMO · sin cobro',
  PAID: 'PAGADA',
  PENDING: 'PENDIENTE',
  EXPIRED: 'EXPIRADA',
  CANCELLED: 'CANCELADA',
};

export default function OrdersDialog({ onClose }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/orders')
      .then((result) => setOrders(Array.isArray(result) ? result : []))
      .catch((requestError) => setError(requestError.message || 'No se pudieron cargar tus entradas.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Modal onClose={onClose} label="Mis entradas">
      <div className="orders-dialog">
        <span className="section-kicker"><span /> TU HISTORIAL DE CONCIERTOS</span>
        <h2>Mis entradas</h2>
        {loading && <p className="ticket-loading">Cargando tus reservas…</p>}
        {error && <p className="inline-error">{error}</p>}
        {!loading && !error && orders.length === 0 && (
          <div className="empty-orders"><Icon name="ticket" size={25} /><p>Aún no tienes reservas.</p><span>Cuando compres entradas, aparecerán aquí.</span></div>
        )}
        <div className="orders-list">
          {orders.map((order) => (
            <article className="order-card" key={order.id}>
              <div className="order-icon"><Icon name="ticket" size={19} /></div>
              <div className="order-content">
                <strong>{order.items?.map((item) => `${item.quantity} × ${item.ticketTypeName}`).join(', ') || 'Reserva Encore'}</strong>
                <span>{formatDate(order.createdAt)} · {statusLabels[order.status] ?? order.status}</span>
                {order.expiresAt && order.status === 'PENDING' && <span>Reserva hasta {formatDate(order.expiresAt)}</span>}
              </div>
              <b>{formatPrice(order.totalAmount, order.currency)}</b>
            </article>
          ))}
        </div>
      </div>
    </Modal>
  );
}
