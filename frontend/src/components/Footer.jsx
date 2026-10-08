export default function Footer({ onOpenCookieSettings }) {
  return (
    <footer className="site-footer">
      <a className="brand footer-brand" href="/">
        encore<span className="brand-mark">/</span>
      </a>
      <span>Esto no se escucha igual desde casa.</span>
      <nav className="footer-legal" aria-label="Enlaces legales">
        <a href="/privacidad">Privacidad</a>
        <a href="/terminos">Términos</a>
        <button onClick={onOpenCookieSettings}>Cookies</button>
      </nav>
      <span className="footer-copy">© 2026 ENCORE</span>
    </footer>
  );
}
