import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api.js';

export default function useSession() {
  const [user, setUser] = useState(null);
  const requestVersion = useRef(0);

  useEffect(() => {
    const version = ++requestVersion.current;
    api('/auth/me')
      .then((result) => {
        if (requestVersion.current === version) setUser(result);
      })
      .catch((error) => {
        if (requestVersion.current !== version) return;
        if (error.status !== 401) {
          console.error('No se pudo restaurar la sesión de Encore.', error);
        }
        setUser(null);
      });
  }, []);

  const refreshUser = useCallback((session) => {
    requestVersion.current += 1;
    setUser(session);
  }, []);

  const logout = useCallback(async () => {
    await api('/auth/logout', { method: 'POST' });
    requestVersion.current += 1;
    setUser(null);
  }, []);

  return { user, refreshUser, logout };
}
