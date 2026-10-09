import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import AuthDialog from './AuthDialog.jsx';
import Footer from './Footer.jsx';
import Header from './Header.jsx';
import Icon from './Icon.jsx';
import PaymentMethodSettings from './PaymentMethodSettings.jsx';

export default function ProfilePage({ onOpenCookieSettings, theme, onToggleTheme }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState('');
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');
  const [authOpen, setAuthOpen] = useState(false);
  const [demoMode, setDemoMode] = useState(null);
  const [paymentModeError, setPaymentModeError] = useState('');
  const [totpSetup, setTotpSetup] = useState(null);
  const [totpCode, setTotpCode] = useState('');
  const [totpMessage, setTotpMessage] = useState('');
  const [totpError, setTotpError] = useState('');
  const [totpBusy, setTotpBusy] = useState(false);
  const profileLoadVersion = useRef(0);

  const loadProfile = useCallback(async () => {
    const version = ++profileLoadVersion.current;
    setPageError('');
    setLoading(true);
    try {
      const result = await api('/auth/me');
      if (profileLoadVersion.current !== version) return;
      setProfile(result);
      setName(result.name ?? '');
    } catch (requestError) {
      if (profileLoadVersion.current !== version) return;
      if (requestError.status === 401) {
        setProfile(null);
      } else {
        setPageError(requestError.message || 'No se pudo cargar tu perfil.');
      }
    } finally {
      if (profileLoadVersion.current === version) setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  useEffect(() => {
    api('/payments/mode')
      .then((result) => {
        if (typeof result?.demoMode !== 'boolean') {
          throw new Error('La API devolvió un modo de pago no válido.');
        }
        setDemoMode(result.demoMode);
      })
      .catch((requestError) => setPaymentModeError(requestError.message || 'No se pudo consultar el modo de pago.'));
  }, []);

  async function saveProfile(event) {
    event.preventDefault();
    const normalizedName = name.trim();
    if (!normalizedName) {
      setSaveMessage('Introduce tu nombre antes de guardar.');
      return;
    }
    if (normalizedName.length > 100) {
      setSaveMessage('El nombre no puede superar los 100 caracteres.');
      return;
    }
    setSaving(true);
    setSaveMessage('');
    try {
      const updatedProfile = await api('/auth/me', {
        method: 'PATCH',
        body: JSON.stringify({ name: normalizedName }),
      });
      setProfile(updatedProfile);
      setName(updatedProfile.name);
      setSaveMessage('Tu nombre se ha actualizado.');
    } catch (requestError) {
      setSaveMessage(requestError.message || 'No se pudo actualizar tu nombre.');
    } finally {
      setSaving(false);
    }
  }

  function handleLogin() {
    setAuthOpen(false);
    void loadProfile();
  }

  async function handleLogout() {
    setPageError('');
    try {
      await api('/auth/logout', { method: 'POST' });
      profileLoadVersion.current += 1;
      setProfile(null);
    } catch (requestError) {
      setPageError(requestError.message || 'No se pudo cerrar la sesión.');
    }
  }

  async function startTotpSetup() {
    setTotpBusy(true);
    setTotpError('');
    setTotpMessage('');
    try {
      const setup = await api('/auth/totp/setup', { method: 'POST' });
      if (!setup?.qrCodeImage || !setup?.secret) {
        throw new Error('La API devolvió datos de configuración 2FA no válidos.');
      }
      setTotpSetup(setup);
      setTotpCode('');
    } catch (requestError) {
      setTotpError(requestError.message || 'No se pudo iniciar la configuración 2FA.');
    } finally {
      setTotpBusy(false);
    }
  }

  async function confirmTotp(event) {
    event.preventDefault();
    setTotpBusy(true);
    setTotpError('');
    setTotpMessage('');
    try {
      await api('/auth/totp/confirm', {
        method: 'POST',
        body: JSON.stringify({ code: totpCode }),
      });
      setTotpSetup(null);
      setTotpCode('');
      setProfile((current) => current ? { ...current, totpEnabled: true } : current);
      setTotpMessage('La verificación en dos pasos está activada.');
    } catch (requestError) {
      setTotpError(requestError.message || 'No se pudo verificar el código.');
    } finally {
      setTotpBusy(false);
    }
  }

  return (
    <>
      <Header
        city="Todos los lugares"
        user={profile}
        onLoginClick={() => setAuthOpen(true)}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />
      <main className="profile-page">
        <div className="profile-page-kicker"><span className="section-kicker"><span /> TU CUENTA EN ENCORE</span></div>
        <h1>Tu perfil<span>.</span></h1>
        <p className="profile-intro">Tus datos y entradas, justo como te gusta.</p>

        {!loading && !profile && !pageError && (
          <section className="profile-signin">
            <div className="empty-icon"><Icon name="user" size={23} /></div>
            <h2>Entra para ver tu perfil.</h2>
            <p>Inicia sesión para cambiar tus datos y gestionar tus métodos de pago.</p>
            <button className="button button-dark" onClick={() => setAuthOpen(true)}>Iniciar sesión <Icon name="arrow" size={16} /></button>
          </section>
        )}

        {loading && <div className="profile-loading" role="status">Cargando los datos de tu cuenta…</div>}
        {pageError && (
          <div className="profile-error" role="alert">
            <p>{pageError}</p>
            <button className="button button-dark" onClick={() => void loadProfile()}>Volver a intentarlo</button>
          </div>
        )}

        {profile && (
          <div className="profile-layout">
            <aside className="profile-side">
              <div className="profile-avatar-large">{profile.name?.slice(0, 1)?.toUpperCase() ?? 'U'}</div>
              <strong>{profile.name}</strong>
              <span>{profile.email}</span>
              <span className="profile-side-tag">MIEMBRO ENCORE</span>
              <a href="/"><Icon name="ticket" size={16} /> Descubrir conciertos</a>
            </aside>

            <div className="profile-content">
              <section className="profile-card">
                <div className="profile-card-heading">
                  <div><span className="section-kicker"><span /> DATOS DE LA CUENTA</span><h2>Tu información</h2></div>
                  <Icon name="user" size={21} />
                </div>
                <form className="profile-form" onSubmit={(event) => void saveProfile(event)}>
                  <label>Nombre visible
                    <input
                      name="name"
                      autoComplete="name"
                      maxLength={100}
                      minLength={1}
                      required
                      value={name}
                      onChange={(event) => { setName(event.target.value); setSaveMessage(''); }}
                    />
                  </label>
                  <label>Correo electrónico
                    <input type="email" name="email" autoComplete="email" value={profile.email ?? ''} readOnly />
                    <span className="field-help">El correo no se puede cambiar desde esta pantalla.</span>
                  </label>
                  {saveMessage && <p className="profile-save-message" role="status">{saveMessage}</p>}
                  <div className="profile-form-actions">
                    <span>Usamos estos datos para gestionar tus reservas.</span>
                    <button className="button button-dark" disabled={saving || name.trim() === profile.name}>
                      {saving ? 'Guardando…' : 'Guardar cambios'} <Icon name="arrow" size={15} />
                    </button>
                  </div>
                </form>
              </section>

              <section className="profile-card">
                <div className="profile-card-heading">
                  <div><span className="section-kicker"><span /> SEGURIDAD</span><h2>Verificación en dos pasos</h2></div>
                  <Icon name="user" size={21} />
                </div>
                {profile.totpEnabled ? (
                  <p className="profile-form-actions">
                    <span>La verificación en dos pasos está activada para tu cuenta.</span>
                  </p>
                ) : totpSetup ? (
                  <div className="profile-totp-setup">
                    <p>Escanea este código QR con Google Authenticator, Microsoft Authenticator o una aplicación compatible. Después introduce el código de seis dígitos para confirmar.</p>
                    <img
                      className="profile-totp-qr"
                      src={`data:image/png;base64,${totpSetup.qrCodeImage}`}
                      alt="Código QR para configurar la verificación en dos pasos"
                    />
                    <p className="profile-totp-secret">Si no puedes escanearlo, introduce esta clave manualmente: <code>{totpSetup.secret}</code></p>
                    <form className="profile-form" onSubmit={(event) => void confirmTotp(event)}>
                      <label htmlFor="totp-confirm-code">Código de verificación
                        <input
                          id="totp-confirm-code"
                          inputMode="numeric"
                          autoComplete="one-time-code"
                          pattern="[0-9]{6}"
                          maxLength={6}
                          required
                          value={totpCode}
                          onChange={(event) => {
                            const nextCode = event.target.value;
                            if (/^\d{0,6}$/.test(nextCode)) setTotpCode(nextCode);
                          }}
                          placeholder="000000"
                        />
                      </label>
                      <div className="profile-form-actions">
                        <button className="button button-dark" disabled={totpBusy || totpCode.length !== 6}>
                          {totpBusy ? 'Verificando…' : 'Confirmar y activar'}
                        </button>
                        <button className="cancel-payment-setup" type="button" disabled={totpBusy} onClick={() => { setTotpSetup(null); setTotpCode(''); }}>
                          Cancelar
                        </button>
                      </div>
                    </form>
                  </div>
                ) : (
                  <>
                    <p className="profile-totp-description">Añade un código temporal de tu aplicación de autenticación al iniciar sesión para proteger mejor tu cuenta.</p>
                    <button className="button button-dark" disabled={totpBusy} onClick={() => void startTotpSetup()}>
                      {totpBusy ? 'Preparando…' : 'Configurar con código QR'}
                    </button>
                  </>
                )}
                {totpMessage && <p className="profile-save-message" role="status">{totpMessage}</p>}
                {totpError && <p className="inline-error" role="alert">{totpError}</p>}
              </section>

              {demoMode === true
                ? <section className="profile-card demo-payment-note">Modo demostración activo. No se solicitan ni almacenan datos de pago y las compras no realizan cargos.</section>
                : demoMode === false
                  ? <PaymentMethodSettings />
                  : <p className="inline-error" role="alert">{paymentModeError || 'Consultando configuración de pagos…'}</p>}

              <div className="profile-account-actions">
                <a className="profile-orders-link" href="/"><Icon name="ticket" size={17} /> Volver a la cartelera y tus entradas</a>
                <button className="profile-logout" onClick={handleLogout}>Cerrar sesión</button>
              </div>
            </div>
          </div>
        )}
      </main>
      <Footer onOpenCookieSettings={onOpenCookieSettings} />
      {authOpen && <AuthDialog onClose={() => setAuthOpen(false)} onLogin={handleLogin} />}
    </>
  );
}
