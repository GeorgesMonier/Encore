import { useCallback, useEffect, useState } from 'react';

export const COOKIE_CONSENT_KEY = 'encore_cookie_consent_v1';

function readConsent() {
  try {
    const saved = window.localStorage.getItem(COOKIE_CONSENT_KEY);
    if (!saved) return null;
    const consent = JSON.parse(saved);
    if (consent?.essential !== true || typeof consent.analytics !== 'boolean') return null;
    return consent;
  } catch {
    return null;
  }
}

export default function useAnalyticsConsent() {
  const [consent, setConsent] = useState(readConsent);
  const [error, setError] = useState('');

  useEffect(() => {
    if (consent?.analytics !== true) return undefined;
    const measurementId = import.meta.env.VITE_GA4_MEASUREMENT_ID;
    if (!measurementId) {
      console.error('Analytics consent is enabled, but VITE_GA4_MEASUREMENT_ID is not configured.');
      return undefined;
    }
    if (!/^G-[A-Z0-9]+$/i.test(measurementId)) {
      console.error('VITE_GA4_MEASUREMENT_ID must be a GA4 measurement ID (G-...).');
      return undefined;
    }

    window.dataLayer = window.dataLayer || [];
    window.gtag = function gtag() {
      window.dataLayer.push(arguments);
    };
    window.gtag('js', new Date());
    window.gtag('consent', 'default', {
      ad_storage: 'denied',
      analytics_storage: 'granted',
      ad_user_data: 'denied',
      ad_personalization: 'denied',
    });
    window.gtag('config', measurementId, { anonymize_ip: true });

    const script = document.createElement('script');
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId)}`;
    document.head.appendChild(script);

    return () => {
      window.gtag?.('consent', 'update', {
        analytics_storage: 'denied',
        ad_storage: 'denied',
        ad_user_data: 'denied',
        ad_personalization: 'denied',
      });
      script.remove();
    };
  }, [consent?.analytics]);

  const saveConsent = useCallback((analytics) => {
    const nextConsent = {
      essential: true,
      analytics,
      savedAt: new Date().toISOString(),
    };
    try {
      window.localStorage.setItem(COOKIE_CONSENT_KEY, JSON.stringify(nextConsent));
      setConsent(nextConsent);
      setError('');
    } catch {
      setError('No se pudieron guardar tus preferencias en este navegador. Revisa la configuración de almacenamiento.');
    }
  }, []);

  return {
    consent,
    error,
    saveSettings: saveConsent,
    acceptAll: useCallback(() => saveConsent(true), [saveConsent]),
    rejectOptional: useCallback(() => saveConsent(false), [saveConsent]),
  };
}
