import { useEffect, useState } from 'react';
import CookieBanner from './CookieBanner.jsx';
import HomePage from './HomePage.jsx';
import NotFoundPage from './NotFoundPage.jsx';
import ProfilePage from './ProfilePage.jsx';
import SupportChat from './SupportChat.jsx';
import PrivacyPage from './PrivacyPage.jsx';
import TermsPage from './TermsPage.jsx';
import useAnalyticsConsent from '../hooks/useAnalyticsConsent.js';
import useDocumentMetadata from '../hooks/useDocumentMetadata.js';

function getInitialPathname() {
  try {
    const requestedPath = window.sessionStorage.getItem('encore_static_route');
    if (requestedPath) {
      window.sessionStorage.removeItem('encore_static_route');
      if (requestedPath.startsWith('/') && !requestedPath.startsWith('//')) {
        window.history.replaceState(null, '', requestedPath);
      }
    }
  } catch (error) {
    console.warn('No se pudo restaurar la ruta solicitada.', error);
  }
  return window.location.pathname.replace(/\/+$/, '') || '/';
}

export default function AppRoutes() {
  const [cookieSettingsOpen, setCookieSettingsOpen] = useState(false);
  const [pathname, setPathname] = useState(getInitialPathname);
  const [theme, setTheme] = useState(() => {
    try {
      return window.localStorage.getItem('encore_theme') === 'dark' ? 'dark' : 'light';
    } catch (error) {
      console.warn('No se pudo recuperar la preferencia de tema.', error);
      return 'light';
    }
  });
  const isPrivacyPage = pathname === '/privacidad';
  const isTermsPage = pathname === '/terminos';
  const isProfilePage = pathname === '/perfil';
  const isNotFound = pathname !== '/' && !isPrivacyPage && !isTermsPage && !isProfilePage;
  const page = isPrivacyPage ? 'privacy' : isTermsPage ? 'terms' : isProfilePage ? 'profile' : isNotFound ? 'not-found' : 'home';

  useDocumentMetadata(page);
  const { consent, error: consentError, acceptAll, rejectOptional, saveSettings } = useAnalyticsConsent();

  useEffect(() => {
    const handleClick = (event) => {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }

      const anchor = event.target instanceof Element ? event.target.closest('a') : null;
      if (!anchor || anchor.target || anchor.hasAttribute('download')) return;

      const destination = new URL(anchor.href, window.location.href);
      if (destination.origin !== window.location.origin) return;

      event.preventDefault();
      window.history.pushState(null, '', destination);
      setPathname(destination.pathname.replace(/\/+$/, '') || '/');
    };
    const handlePopState = () => setPathname(window.location.pathname.replace(/\/+$/, '') || '/');

    document.addEventListener('click', handleClick);
    window.addEventListener('popstate', handlePopState);
    return () => {
      document.removeEventListener('click', handleClick);
      window.removeEventListener('popstate', handlePopState);
    };
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      window.localStorage.setItem('encore_theme', theme);
    } catch (error) {
      console.warn('No se pudo guardar la preferencia de tema.', error);
    }
  }, [theme]);

  useEffect(() => {
    try {
      window.localStorage.removeItem('encore_token');
    } catch (error) {
      console.warn('No se pudo eliminar la credencial antigua del almacenamiento local.', error);
    }
  }, []);

  const toggleTheme = () => setTheme((current) => current === 'dark' ? 'light' : 'dark');

  return (
    <div className={`app-shell${consent ? '' : ' cookie-consent-pending'}`}>
      {isPrivacyPage ? <PrivacyPage onCookieSettings={() => setCookieSettingsOpen(true)} theme={theme} onToggleTheme={toggleTheme} />
        : isTermsPage ? <TermsPage onOpenCookieSettings={() => setCookieSettingsOpen(true)} theme={theme} onToggleTheme={toggleTheme} />
          : isProfilePage ? <ProfilePage onOpenCookieSettings={() => setCookieSettingsOpen(true)} theme={theme} onToggleTheme={toggleTheme} />
          : isNotFound ? <NotFoundPage onOpenCookieSettings={() => setCookieSettingsOpen(true)} theme={theme} onToggleTheme={toggleTheme} />
            : <HomePage onOpenCookieSettings={() => setCookieSettingsOpen(true)} theme={theme} onToggleTheme={toggleTheme} />}
      <CookieBanner
        consent={consent}
        consentError={consentError}
        settingsOpen={cookieSettingsOpen}
        onOpenSettings={() => setCookieSettingsOpen(true)}
        onCloseSettings={() => setCookieSettingsOpen(false)}
        onAcceptAll={() => { acceptAll(); setCookieSettingsOpen(false); }}
        onRejectOptional={() => { rejectOptional(); setCookieSettingsOpen(false); }}
        onSaveSettings={(analytics) => { saveSettings(analytics); setCookieSettingsOpen(false); }}
      />
      <SupportChat />
    </div>
  );
}
