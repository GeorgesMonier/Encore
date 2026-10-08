import Footer from './Footer.jsx';
import LegalContact from './LegalContact.jsx';

export default function PrivacyPage({ onCookieSettings, theme, onToggleTheme }) {
  return (
    <>
      <header className="legal-header">
        <a className="brand" href="/">encore<span className="brand-mark">/</span></a>
        <a href="/">← Volver a conciertos</a>
        <button className="theme-toggle" type="button" onClick={onToggleTheme} aria-label={theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro'}>
          <span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span>
        </button>
      </header>
      <main className="legal-page">
        <span className="section-kicker"><span /> INFORMACIÓN LEGAL</span>
        <h1>Política de privacidad</h1>
        <div className="legal-draft-notice"><strong>Borrador pendiente de completar y revisar.</strong> Antes de publicar la web, el responsable del tratamiento debe completar su identidad y datos de contacto y validar este texto con asesoramiento legal.</div>
        <p className="legal-updated">Última revisión: 8 de octubre de 2026</p>

        <h2>1. Quién trata tus datos</h2>
        <p>El responsable del tratamiento de los datos de Encore es <strong>Encore</strong>. Puedes contactar con el responsable en <LegalContact />. Estos datos deben completarse antes de la publicación.</p>

        <h2>2. Qué datos se tratan y por qué</h2>
        <p>La plataforma puede tratar los datos que facilitas al crear una cuenta o iniciar sesión, como nombre y correo electrónico; los datos necesarios para gestionar pedidos y entradas; y la información que envías al equipo de soporte. La base jurídica, los plazos de conservación y los destinatarios de cada tratamiento deben concretarse y revisarse con el responsable del tratamiento.</p>

        <h2>3. Pagos y proveedores</h2>
        <p>El modo de demostración predeterminado no procesa pagos ni solicita datos de tarjeta. Stripe solo se utiliza si la persona responsable del despliegue lo habilita expresamente; en ese caso, los datos de pago se envían a Stripe desde sus componentes y se sujetan también a la política de privacidad de Stripe. La web carga imágenes alojadas en Ticketmaster y Unsplash y fuentes alojadas en Google Fonts; estos proveedores pueden recibir datos técnicos, como la dirección IP, al solicitar esos recursos.</p>

        <h2>4. Inicio de sesión y seguridad</h2>
        <p>La sesión utiliza una cookie HttpOnly y Secure en producción, con protección CSRF para las solicitudes que cambian datos. El navegador no puede leer el token de sesión desde JavaScript. Cierra la sesión cuando termines, especialmente en dispositivos compartidos. La configuración final de dominio, HTTPS y políticas de cookies debe verificarse en el hosting antes de un lanzamiento real.</p>

        <h2>5. Cookies y medición de audiencia</h2>
        <p>La preferencia de consentimiento se guarda en el almacenamiento local de este navegador. Google Analytics 4 solo se inicializa si activas la categoría de analítica; esta web no carga GA4 antes de obtener esa autorización. Puedes retirar o cambiar tu elección en cualquier momento:</p>
        <button className="button button-dark legal-cookie-button" onClick={onCookieSettings}>Configurar cookies</button>

        <h2>6. Tus derechos</h2>
        <p>Según la normativa aplicable, puedes tener derecho a acceder, rectificar, suprimir, limitar u oponerte al tratamiento de tus datos y a solicitar su portabilidad. Para ejercerlos, contacta con <LegalContact />. Deben añadirse la autoridad de control y la información sobre reclamaciones aplicables al país del responsable.</p>

        <h2>7. Cambios y contacto</h2>
        <p>Este proyecto forma parte de mi portfolio personal y tiene fines exclusivamente demostrativos. La información y los textos incluidos son de ejemplo.</p>
      </main>
      <Footer onOpenCookieSettings={onCookieSettings} />
    </>
  );
}
