import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import useDebouncedValue from './useDebouncedValue.js';
import useEvents from './useEvents.js';
import useSession from './useSession.js';

export default function useHomePage() {
  const [query, setQuery] = useState('');
  const [city, setCity] = useState('');
  const [cities, setCities] = useState([]);
  const [citiesError, setCitiesError] = useState('');
  const debouncedQuery = useDebouncedValue(query.trim(), 300);
  const { events, loading: requestLoading, error, reloadEvents } = useEvents({ city, search: debouncedQuery });
  const loading = requestLoading || query.trim() !== debouncedQuery;
  const { user, refreshUser, logout } = useSession();
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [ordersOpen, setOrdersOpen] = useState(false);
  const [toast, setToast] = useState('');

  const loadCities = useCallback(async () => {
    setCitiesError('');
    try {
      const result = await api('/events/cities');
      if (!Array.isArray(result)) throw new Error('La API devolvió una lista de ciudades no válida.');
      const cityNames = result
        .filter((name) => typeof name === 'string' && name.trim())
        .map((name) => name.trim());
      const uniqueCities = [...new Map(cityNames.map((name) => [name.toLocaleLowerCase('es'), name])).values()];
      setCities(uniqueCities.sort((a, b) => a.localeCompare(b, 'es')));
    } catch (requestError) {
      setCitiesError(requestError.message || 'No se pudieron cargar los lugares disponibles.');
    }
  }, []);

  useEffect(() => {
    void loadCities();
  }, [loadCities]);

  useEffect(() => {
    const currentUrl = new URL(window.location.href);
    const paymentIntentId = currentUrl.searchParams.get('payment_intent');
    const orderId = currentUrl.searchParams.get('order_id');
    const redirectStatus = currentUrl.searchParams.get('redirect_status');

    if (redirectStatus === 'succeeded' && paymentIntentId && orderId) {
      currentUrl.searchParams.delete('payment_intent_client_secret');
      window.history.replaceState({}, '', `${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`);
      api(`/payments/confirm/${orderId}`, {
        method: 'POST',
        body: JSON.stringify({ paymentIntentId }),
      })
        .then(() => {
          setToast('Pago confirmado. Tu reserva está lista.');
          currentUrl.searchParams.delete('payment_intent');
          currentUrl.searchParams.delete('redirect_status');
          currentUrl.searchParams.delete('order_id');
          window.history.replaceState({}, '', `${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`);
        })
        .catch((requestError) => setToast(requestError.message || 'No se pudo confirmar el pago.'))
    } else if (redirectStatus === 'succeeded') {
      setToast('Pago confirmado. Tu reserva está lista.');
    }
    if (paymentIntentId && !(redirectStatus === 'succeeded' && orderId)) {
      currentUrl.searchParams.delete('payment_intent');
      currentUrl.searchParams.delete('payment_intent_client_secret');
      currentUrl.searchParams.delete('redirect_status');
      currentUrl.searchParams.delete('order_id');
      window.history.replaceState({}, '', `${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`);
    }
  }, []);

  useEffect(() => {
    if (!toast) return undefined;
    const timeout = window.setTimeout(() => setToast(''), 4200);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  const scrollToEvents = useCallback(() => {
    document.getElementById('eventos')?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  const handleLogin = useCallback((session) => {
    refreshUser({ name: session.name, email: session.email });
    setAuthOpen(false);
    setToast(`¡Qué bueno verte, ${session.name}!`);
  }, [refreshUser]);

  const handleLogout = useCallback(async () => {
    await logout();
    setOrdersOpen(false);
    setToast('Has cerrado sesión.');
  }, [logout]);

  return {
    events,
    visibleEvents: events,
    loading,
    error,
    reloadEvents,
    cities,
    citiesError,
    loadCities,
    city,
    setCity,
    query,
    setQuery,
    clearFilters: () => { setQuery(''); setCity(''); },
    scrollToEvents,
    user,
    handleLogin,
    handleLogout,
    selectedEvent,
    setSelectedEvent,
    authOpen,
    setAuthOpen,
    ordersOpen,
    setOrdersOpen,
    toast,
    showPaymentSuccess: (demoMode) => setToast(
      demoMode
        ? 'Compra de demostración confirmada. No se ha realizado ningún cargo.'
        : 'Pago confirmado. Tu reserva está lista.',
    ),
  };
}
