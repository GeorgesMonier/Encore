import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';

export default function useEvents({ city, search }) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reloadEvents = useCallback(async (signal) => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (city) params.set('city', city);
      if (search) params.set('search', search);
      const query = params.toString();
      const result = await api(`/events${query ? `?${query}` : ''}`, { signal });
      if (signal?.aborted) return;
      if (!Array.isArray(result)) {
        throw new Error('La API devolvió una respuesta de conciertos no válida.');
      }
      setEvents(result);
    } catch (requestError) {
      if (!signal?.aborted && requestError.name !== 'AbortError') {
        setError(requestError.message || 'No se pudieron cargar los conciertos.');
      }
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [city, search]);

  useEffect(() => {
    const controller = new AbortController();
    void reloadEvents(controller.signal);
    return () => controller.abort();
  }, [reloadEvents]);

  return { events, loading, error, reloadEvents };
}
