import Footer from './Footer.jsx';
import Icon from './Icon.jsx';

export default function NotFoundPage({ onOpenCookieSettings, theme, onToggleTheme }) {
  return (
    <>
      <header className="legal-header">
        <a className="brand" href="/">encore<span className="brand-mark">/</span></a>
        <span>EL ESCENARIO ESTÁ EN OTRA PARTE</span>
        <button className="theme-toggle" type="button" onClick={onToggleTheme} aria-label={theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro'}>
          <span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span>
        </button>
      </header>
      <main className="not-found-page">
        <span className="not-found-number">404</span>
        <span className="section-kicker"><span /> PÁGINA NO ENCONTRADA</span>
        <h1>Este enlace<br />ha perdido el <em>ritmo.</em></h1>
        <p>La página que buscas no existe o se ha movido.</p>
        <a className="button button-dark" href="/">Volver a conciertos <Icon name="arrow" size={17} /></a>
      </main>
      <Footer onOpenCookieSettings={onOpenCookieSettings} />
    </>
  );
}
