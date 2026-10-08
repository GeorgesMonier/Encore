import Footer from './Footer.jsx';
import LegalContact from './LegalContact.jsx';

export default function TermsPage({ onOpenCookieSettings, theme, onToggleTheme }) {
  return (
    <>
      <header className="legal-header">
        <a className="brand" href="/">
          encore<span className="brand-mark">/</span>
        </a>
        <a href="/">← Volver a conciertos</a>
        <button className="theme-toggle" type="button" onClick={onToggleTheme} aria-label={theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro'}>
          <span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span>
        </button>
      </header>

      <main className="legal-page">
        <span className="section-kicker">
          <span /> INFORMACIÓN LEGAL
        </span>

        <h1>Términos y condiciones</h1>

        <div className="legal-draft-notice">
          <strong>Proyecto demostrativo.</strong> Encore es un proyecto
          ficticio desarrollado con fines de portfolio. Los datos societarios,
          fiscales y de contacto utilizados en esta página son de ejemplo y
          deberán sustituirse por los datos reales antes de poner el servicio
          en producción.
        </div>

        <p className="legal-updated">
          Última revisión: 8 de octubre de 2026
        </p>

        <h2>1. Titular del servicio</h2>

        <p>
          El titular de la plataforma Encore es{' '}
          <strong>Encore Events, S.L.</strong>, domicilio social en Calle de Alcalá 120, 28009 Madrid, España,
          e inscrita en el Registro Mercantil de Madrid.
        </p>

        <p>
          Para cualquier consulta relacionada con la plataforma, puedes
          contactar con <LegalContact kind="atención al cliente" />.
        </p>

        <h2>2. Objeto y uso de la plataforma</h2>

        <p>
          Encore es una plataforma digital destinada a la consulta y, cuando
          esta funcionalidad se encuentre habilitada, a la adquisición de
          entradas para conciertos y otros eventos musicales publicados en la
          plataforma.
        </p>

        <p>
          El usuario se compromete a utilizar Encore de forma lícita, diligente
          y conforme a estos términos, así como a facilitar información veraz,
          exacta y actualizada cuando sea necesaria para utilizar determinados
          servicios.
        </p>

        <p>
          El usuario será responsable de mantener la confidencialidad de sus
          credenciales de acceso y deberá comunicar cualquier uso no autorizado
          de su cuenta.
        </p>

        <h2>3. Eventos, promotores y disponibilidad</h2>

        <p>
          La información relativa a cada concierto —incluyendo fecha, hora,
          recinto, artista, promotor, tipo de entrada, precio y condiciones de
          acceso— se mostrará en la ficha correspondiente del evento.
        </p>

        <p>
          La disponibilidad de entradas está sujeta a las existencias
          disponibles en cada momento. La selección de una entrada no implica
          necesariamente la adquisición de la misma hasta que el proceso de
          compra haya sido completado y el pago haya sido confirmado.
        </p>

        <p>
          Cuando exista un sistema de reserva temporal, las entradas podrán
          mantenerse bloqueadas durante el tiempo indicado en pantalla.
          Transcurrido dicho plazo sin completar correctamente la compra, la
          reserva podrá cancelarse y las entradas volverán a estar disponibles.
        </p>

        <h2>4. Proceso de compra</h2>

        <p>
          Antes de confirmar un pedido, el usuario podrá consultar el resumen
          de su compra, incluyendo las entradas seleccionadas, su precio, los
          impuestos aplicables y, cuando corresponda, los gastos de gestión u
          otros costes adicionales.
        </p>

        <p>
          La contratación se entenderá realizada cuando el sistema confirme
          correctamente el pedido y el pago correspondiente. Encore enviará al
          usuario una confirmación de la compra por medios electrónicos.
        </p>

        <h2>5. Precios y pagos</h2>

        <p>
          Todos los precios se mostrarán en euros e indicarán, cuando resulte
          aplicable, los impuestos incluidos y cualquier coste adicional que
          deba asumir el consumidor antes de confirmar la compra.
        </p>

        <p>
          Los pagos electrónicos se procesan mediante{' '}
          <strong>Stripe</strong>. Encore no almacena directamente los datos
          completos de las tarjetas bancarias utilizadas para realizar los
          pagos.
        </p>

        <p>
          Un pedido no se considerará pagado hasta que el sistema de Encore
          reciba y valide correctamente la confirmación del proveedor de
          pagos.
        </p>

        <h2>6. Cancelaciones, cambios y reembolsos</h2>

        <p>
          Las condiciones de cancelación, modificación y reembolso podrán
          variar en función del evento y del promotor responsable de su
          organización.
        </p>

        <p>
          Las condiciones particulares aplicables a cada evento se mostrarán al
          usuario antes de finalizar la compra cuando resulten relevantes.
        </p>

        <p>
          En caso de cancelación, modificación sustancial, aplazamiento o
          suspensión de un evento, se informará a los titulares de entradas
          sobre las medidas aplicables y, cuando corresponda, sobre el
          procedimiento para solicitar un reembolso.
        </p>

        <p>
          Cuando la normativa aplicable reconozca al consumidor un derecho de
          desistimiento, este se aplicará en los términos establecidos
          legalmente. No obstante, determinadas entradas para actividades de
          ocio con una fecha o periodo de ejecución específicos pueden estar
          legalmente exceptuadas del derecho de desistimiento.
        </p>

        <h2>7. Responsabilidad sobre los eventos</h2>

        <p>
          La información sobre el organizador o promotor de cada concierto se
          indicará en la página correspondiente al evento.
        </p>

        <p>
          El organizador será responsable de la celebración del evento, de las
          condiciones de acceso al recinto y de aquellas obligaciones que le
          correspondan legalmente como responsable de la organización.
        </p>

        <p>
          Lo anterior se entiende sin perjuicio de las obligaciones y
          responsabilidades que correspondan legalmente a Encore como titular
          de la plataforma o como intermediario en la contratación.
        </p>

        <h2>8. Cuenta de usuario</h2>

        <p>
          Algunas funcionalidades de Encore pueden requerir la creación de una
          cuenta. El usuario deberá proporcionar información veraz y mantenerla
          actualizada.
        </p>

        <p>
          Encore podrá suspender o cancelar una cuenta cuando existan motivos
          legítimos relacionados con un uso fraudulento, ilícito o contrario a
          estos términos, respetando en todo caso los derechos que correspondan
          al usuario conforme a la legislación aplicable.
        </p>

        <h2>9. Propiedad intelectual</h2>

        <p>
          Los elementos que forman parte de la plataforma Encore, incluyendo su
          diseño, código, logotipo, textos, elementos gráficos y demás
          contenidos propios, están protegidos por la normativa aplicable en
          materia de propiedad intelectual e industrial.
        </p>

        <p>
          El uso de la plataforma no supone la cesión al usuario de ningún
          derecho de propiedad intelectual o industrial sobre sus contenidos,
          salvo aquellos derechos necesarios para utilizar el servicio.
        </p>

        <h2>10. Protección de datos personales</h2>

        <p>
          El tratamiento de los datos personales de los usuarios se realizará
          de acuerdo con la normativa aplicable en materia de protección de
          datos, incluyendo el Reglamento (UE) 2016/679 y la legislación
          española que resulte aplicable.
        </p>

        <p>
          Para obtener información detallada sobre las categorías de datos
          tratados, las finalidades, las bases jurídicas, los plazos de
          conservación y los derechos de los usuarios, consulta la{' '}
          <a href="/privacidad">Política de privacidad</a>.
        </p>

        <h2>11. Cookies</h2>

        <p>
          Encore podrá utilizar cookies y tecnologías similares de acuerdo con
          su política de cookies y con la normativa aplicable.
        </p>

        <p>
          El usuario podrá consultar y modificar sus preferencias de cookies
          mediante el panel de configuración disponible en la plataforma.
        </p>

        <h2>12. Atención al cliente y reclamaciones</h2>

        <p>
          Para realizar consultas, incidencias o reclamaciones relacionadas con
          Encore, el usuario podrá contactar con{' '}
          <LegalContact kind="atención al cliente" />.
        </p>

        <p>
          Encore atenderá las reclamaciones recibidas y facilitará, cuando
          corresponda, la información necesaria para contactar con el
          organizador responsable del evento.
        </p>

        <h2>13. Ley aplicable y jurisdicción</h2>

        <p>
          Estos términos se regirán por la legislación española, sin perjuicio
          de las normas imperativas de protección de los consumidores que
          resulten aplicables.
        </p>

        <p>
          Cuando el usuario tenga la condición de consumidor, cualquier
          controversia se someterá a los juzgados y tribunales que resulten
          competentes conforme a la normativa aplicable.
        </p>

        <h2>14. Modificación de los términos</h2>

        <p>
          Encore podrá modificar estos términos cuando resulte necesario, por
          ejemplo, para adaptarlos a cambios legales, técnicos o funcionales de
          la plataforma.
        </p>

        <p>
          La versión vigente estará disponible en esta página y se indicará la
          fecha de su última actualización.
        </p>

        <div className="legal-draft-notice">
          <strong>Aviso para este proyecto:</strong> los datos de la sociedad,
           domicilio, registro y contacto utilizados en esta página son
          ficticios y se incluyen exclusivamente como contenido demostrativo
          para un proyecto de portfolio. Este texto no constituye asesoramiento
          jurídico.
        </div>
      </main>

      <Footer onOpenCookieSettings={onOpenCookieSettings} />
    </>
  );
}