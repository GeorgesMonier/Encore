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
  const [selectedOrderIds, setSelectedOrderIds] = useState([]);
  const [removingOrderIds, setRemovingOrderIds] = useState([]);

  useEffect(() => {
    api('/orders')
      .then((result) => setOrders(Array.isArray(result) ? result : []))
      .catch((requestError) => setError(requestError.message || 'No se pudieron cargar tus entradas.'))
      .finally(() => setLoading(false));
  }, []);

  async function removeOrders(orderIds) {
    setError('');
    setRemovingOrderIds((current) => [...new Set([...current, ...orderIds])]);
    const removedIds = [];
    let removalError = '';
    for (const orderId of orderIds) {
      try {
        await api(`/orders/${orderId}`, { method: 'DELETE' });
        removedIds.push(orderId);
      } catch (requestError) {
        removalError = requestError.message || 'No se pudieron quitar algunas reservas.';
        break;
      }
    }
    if (removedIds.length) {
      setOrders((current) => current.filter((order) => !removedIds.includes(order.id)));
      setSelectedOrderIds((current) => current.filter((id) => !removedIds.includes(id)));
    }
    setRemovingOrderIds((current) => current.filter((id) => !orderIds.includes(id)));
    if (removalError) setError(removalError);
  }

  return (
    <Modal onClose={onClose} label="Mis entradas">
      <div className="orders-dialog">
        <span className="section-kicker"><span /> TU HISTORIAL DE CONCIERTOS</span>
        <h2>Mis entradas</h2>
        <p className="orders-removal-note">Quitar una reserva pendiente devuelve sus entradas al inventario. Las compras ya pagadas solo se quitan del historial.</p>
        {loading && <p className="ticket-loading">Cargando tus reservas…</p>}
        {error && <p className="inline-error">{error}</p>}
        {!loading && !error && orders.length > 0 && (
          <div className="orders-selection">
            <label>
              <input
                type="checkbox"
                checked={orders.every((order) => selectedOrderIds.includes(order.id))}
                onChange={(event) => setSelectedOrderIds(event.target.checked
                  ? orders.map((order) => order.id)
                  : [])}
                aria-label="Seleccionar todas las entradas"
              />
              Seleccionar todas las entradas
            </label>
            <button
              type="button"
              className="order-remove-selected"
              disabled={!selectedOrderIds.length || removingOrderIds.length > 0}
              onClick={() => void removeOrders(selectedOrderIds)}
            >
              Quitar seleccionadas ({selectedOrderIds.length})
            </button>
          </div>
        )}
        {!loading && !error && orders.length === 0 && (
          <div className="empty-orders"><Icon name="ticket" size={25} /><p>Aún no tienes reservas.</p><span>Cuando compres entradas, aparecerán aquí.</span></div>
        )}
        <div className="orders-list">
          {orders.map((order) => (
            <article className="order-card" key={order.id}>
              <input
                className="order-select"
                type="checkbox"
                checked={selectedOrderIds.includes(order.id)}
                disabled={removingOrderIds.length > 0}
                onChange={(event) => setSelectedOrderIds((current) => event.target.checked
                  ? [...current, order.id]
                  : current.filter((id) => id !== order.id))}
                aria-label={`Seleccionar entrada del ${formatDate(order.createdAt)}`}
              />
              <div className="order-icon"><Icon name="ticket" size={19} /></div>
              <div className="order-content">
                <strong>{order.items?.map((item) => `${item.quantity} × ${item.ticketTypeName}`).join(', ') || 'Reserva Encore'}</strong>
                <span>{formatDate(order.createdAt)} · {statusLabels[order.status] ?? order.status}</span>
                {order.expiresAt && order.status === 'PENDING' && <span>Reserva hasta {formatDate(order.expiresAt)}</span>}
              </div>
              <b>{formatPrice(order.totalAmount, order.currency)}</b>
              <button
                type="button"
                className="order-remove"
                disabled={removingOrderIds.length > 0}
                onClick={() => void removeOrders([order.id])}
              >
                {removingOrderIds.includes(order.id) ? 'Quitando…' : 'Quitar'}
              </button>
            </article>
          ))}
        </div>
      </div>
    </Modal>
  );
}
