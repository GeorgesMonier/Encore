import { useState } from 'react';
import { api } from '../api.js';
import Icon from './Icon.jsx';
import Modal from './Modal.jsx';

export default function AuthDialog({ onClose, onLogin }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [needsTotp, setNeedsTotp] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [website, setWebsite] = useState('');

  async function submit(event) {
    event.preventDefault();
    if (website) {
      setError('No se pudo validar el formulario. Recarga la página e inténtalo de nuevo.');
      return;
    }
    setBusy(true);
    setError('');
    setMessage('');
    try {
      if (mode === 'register') {
        const session = await api('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ name, email, password }),
        });
        onLogin(session);
        return;
      }

      const path = needsTotp ? `/auth/login/totp?code=${encodeURIComponent(totpCode)}` : '/auth/login';
      const session = await api(path, {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      if (session.requiresTotp) {
        setNeedsTotp(true);
        setMessage('Introduce el código de tu aplicación de autenticación.');
        return;
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message || 'No se pudo completar la solicitud.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose} label="Acceder a Encore">
      <div className="auth-dialog">
        <a className="brand auth-brand" href="/">encore<span className="brand-mark">/</span></a>
        <span className="section-kicker"><span /> {needsTotp ? 'SEGURIDAD' : 'MÚSICA EN VIVO, A UN CLIC'}</span>
        <h2>{needsTotp ? 'Un paso más.' : mode === 'login' ? 'Vuelve al ruido.' : 'Entra en la lista.'}</h2>
        <p className="auth-subtitle">
          {needsTotp ? 'Usa el código de seis dígitos de tu app de autenticación.' : mode === 'login' ? 'Inicia sesión para reservar tu próximo concierto.' : 'Crea una cuenta para hacerte con tus próximas entradas.'}
        </p>
        {!needsTotp && (
          <div className="auth-tabs">
            <button className={mode === 'login' ? 'selected' : ''} onClick={() => { setMode('login'); setError(''); }}>Iniciar sesión</button>
            <button className={mode === 'register' ? 'selected' : ''} onClick={() => { setMode('register'); setError(''); }}>Crear cuenta</button>
          </div>
        )}
        <form className="auth-form" onSubmit={(event) => void submit(event)}>
          <label className="honeypot-field" aria-hidden="true">
            Sitio web<input tabIndex={-1} autoComplete="off" name="website" value={website} onChange={(event) => setWebsite(event.target.value)} />
          </label>
          {mode === 'register' && !needsTotp && (
            <label>Nombre completo<input autoComplete="name" required value={name} onChange={(event) => setName(event.target.value)} placeholder="Tu nombre" /></label>
          )}
          <label>Correo electrónico<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="tu@email.com" /></label>
          {!needsTotp && <label>Contraseña<input type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} minLength={mode === 'register' ? 8 : undefined} required value={password} onChange={(event) => setPassword(event.target.value)} placeholder={mode === 'register' ? 'Mínimo 8 caracteres' : 'Tu contraseña'} /></label>}
          {needsTotp && <label>Código de autenticación<input inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required value={totpCode} onChange={(event) => setTotpCode(event.target.value)} placeholder="000000" /></label>}
          {message && <p className="inline-message">{message}</p>}
          {error && <p className="inline-error">{error}</p>}
          <button className="button button-dark auth-submit" disabled={busy}>
            {busy ? 'Un momento…' : needsTotp ? 'Verificar y entrar' : mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}
            {!busy && <Icon name="arrow" size={16} />}
          </button>
        </form>
        {!needsTotp && <p className="auth-terms">Al continuar, aceptas nuestros <a href="/terminos">términos</a> y la <a href="/privacidad">política de privacidad</a>.</p>}
      </div>
    </Modal>
  );
}
