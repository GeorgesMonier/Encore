import { useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import Icon from './Icon.jsx';

export default function SupportChat() {
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState([]);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, error, sending]);

  async function sendQuestion(event) {
    event.preventDefault();
    const normalizedQuestion = question.trim();
    if (!normalizedQuestion || sending) return;
    if (normalizedQuestion.length > 1000) {
      setError('La consulta no puede superar los 1000 caracteres.');
      return;
    }

    setMessages((current) => [...current, { from: 'user', text: normalizedQuestion }]);
    setQuestion('');
    setSending(true);
    setError('');
    try {
      const response = await api('/support/ask', {
        method: 'POST',
        body: JSON.stringify({ question: normalizedQuestion }),
      });
      if (typeof response?.answer !== 'string' || !response.answer.trim()) {
        throw new Error('El asistente no devolvió una respuesta. Inténtalo de nuevo.');
      }
      setMessages((current) => [...current, { from: 'assistant', text: response.answer }]);
    } catch (requestError) {
      setError(requestError.message || 'No se pudo enviar tu consulta.');
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="support-chat">
      {open && (
        <section className="support-panel" role="dialog" aria-modal="false" aria-labelledby="support-title">
          <header className="support-header">
            <div className="support-avatar"><Icon name="sparkle" size={18} /></div>
            <div><strong id="support-title">Asistente Encore</strong><span>Consultas sobre conciertos y entradas</span></div>
            <button type="button" className="support-close" aria-label="Cerrar asistente" onClick={() => setOpen(false)}>×</button>
          </header>
          <div className="support-messages" role="log" aria-live="polite" aria-relevant="additions text">
            <p className="support-welcome">¡Hola! Pregúntame sobre Encore, tus entradas o cómo funciona la plataforma.</p>
            {messages.map((message, index) => (
              <p className={`support-message support-message-${message.from}`} key={`${index}-${message.from}`}>{message.text}</p>
            ))}
            {sending && <p className="support-typing" role="status">El asistente está pensando…</p>}
            {error && <p className="support-error" role="alert">{error}</p>}
            <div ref={messagesEndRef} />
          </div>
          <form className="support-form" onSubmit={(event) => void sendQuestion(event)}>
            <label className="visually-hidden" htmlFor="support-question">Escribe tu consulta</label>
            <textarea
              id="support-question"
              value={question}
              onChange={(event) => { setQuestion(event.target.value); setError(''); }}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
                  event.preventDefault();
                  event.currentTarget.form.requestSubmit();
                }
              }}
              maxLength={1000}
              placeholder="Escribe tu consulta…"
              rows={2}
              disabled={sending}
            />
            <button type="submit" aria-label="Enviar consulta" disabled={sending || !question.trim()}>
              <Icon name="arrow" size={17} />
            </button>
          </form>
          <p className="support-disclaimer">Respuestas basadas en la información disponible de Encore.</p>
        </section>
      )}
      <button
        type="button"
        className={`support-launcher${open ? ' support-launcher-open' : ''}`}
        aria-label={open ? 'Cerrar asistente de consultas' : 'Abrir asistente de consultas'}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        {open ? <span aria-hidden="true">×</span> : <><Icon name="chat" size={21} /><span>¿Te ayudamos?</span></>}
      </button>
    </div>
  );
}
