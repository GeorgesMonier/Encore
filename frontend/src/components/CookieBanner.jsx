import { useState } from 'react';
import Icon from './Icon.jsx';
import Modal from './Modal.jsx';

export default function CookieBanner({
  consent,
  consentError,
  settingsOpen,
  onOpenSettings,
  onCloseSettings,
  onAcceptAll,
  onRejectOptional,
  onSaveSettings,
}) {
  const [analytics, setAnalytics] = useState(consent?.analytics === true);

  function openSettings() {
    setAnalytics(consent?.analytics === true);
    onOpenSettings();
  }

  return (
    <>
      {!consent && (
        <aside className="cookie-banner" aria-label="Preferencias de cookies">
          <div className="cookie-copy">
            <strong><Icon name="cookie" size={18} /> Tu privacidad va primero.</strong>
            <p>Usamos almacenamiento esencial para el sitio. Google Analytics solo se carga si aceptas las cookies de analítica.</p>
            <a href="/privacidad">Consulta la política de privacidad</a>
            {consentError && <p className="inline-error" role="alert">{consentError}</p>}
          </div>
          <div className="cookie-actions">
            <button className="cookie-secondary" onClick={onRejectOptional}>Rechazar opcionales</button>
            <button className="button button-dark cookie-primary" onClick={onAcceptAll}>Aceptar analítica</button>
            <button className="cookie-settings-link" onClick={openSettings}>Configurar</button>
          </div>
        </aside>
      )}

      {consent && !settingsOpen && (
        <button className="cookie-reopen" onClick={openSettings} aria-label="Cambiar preferencias de cookies">
          <Icon name="cookie" size={17} />
        </button>
      )}

      {settingsOpen && (
        <Modal onClose={onCloseSettings} label="Configurar cookies">
          <div className="cookie-settings">
            <span className="section-kicker"><span /> CONTROL DE PRIVACIDAD</span>
            <h2>Tú tienes el control.</h2>
            <p>Las cookies esenciales guardan tus preferencias de consentimiento y son necesarias para ofrecer esta opción. Las cookies de analítica son opcionales y están desactivadas hasta que las autorices.</p>
            <div className="cookie-option">
              <div><strong>Esenciales</strong><span>Preferencias y funcionamiento básico.</span></div>
              <span className="always-on">Siempre activas</span>
            </div>
            <label className="cookie-option">
              <div><strong>Analítica (Google Analytics 4)</strong><span>Nos ayuda a entender el uso de la web.</span></div>
              <input type="checkbox" checked={analytics} onChange={(event) => setAnalytics(event.target.checked)} />
            </label>
            <p className="cookie-disclosure">Al aceptar analítica, Google puede recibir datos de uso. Puedes cambiar de opinión desde el enlace de cookies del pie de página.</p>
            {consentError && <p className="inline-error" role="alert">{consentError}</p>}
            <div className="cookie-settings-actions">
              <button className="cookie-secondary" onClick={onRejectOptional}>Rechazar opcionales</button>
              <button className="button button-dark" onClick={() => { onSaveSettings(analytics); onCloseSettings(); }}>Guardar preferencias</button>
            </div>
          </div>
        </Modal>
      )}
    </>
  );
}
