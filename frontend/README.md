# Encore frontend

Frontend de conciertos en React + Vite conectado a la API Spring Boot de Encore.

## Estructura

- `src/App.jsx`: punto de entrada de la interfaz; delega el enrutado.
- `src/components/`: páginas, cabecera, catálogo, autenticación, cookies y diálogos.
- `src/hooks/`: estado y efectos del catálogo, sesión, página, consentimiento y metadatos.
- `src/utils/`: formato y optimización de imágenes.
- `src/styles/`: estilos globales, componentes y páginas legales.
- `public/`: favicon y páginas/archivos estáticos.

## Desarrollo

1. Arranca la API y la base de datos de Encore; consulta `encore-api/README.md`.
2. Copia `.env.example` a `.env` y configura, cuando estén disponibles, las variables `VITE_*`. Para desarrollo local, cambia `VITE_API_BASE_URL` a `/api` (Vite lo reenvía al backend local); el valor de ejemplo apunta al API publicado en Render.
3. Desde esta carpeta, ejecuta `npm install` y `npm run dev`.

Vite reenvía las llamadas `/api` al backend local. El backend usa el modo demo si no hay una clave secreta de Stripe; cuando `STRIPE_API_KEY` está configurada, activa Stripe salvo que `PAYMENTS_DEMO_MODE=true` fuerce el modo demo. Para cobrar con Stripe, configura también `VITE_STRIPE_PUBLISHABLE_KEY` con una clave de la misma cuenta y entorno; el formulario de pago aparece después de reservar las entradas. Una variable `VITE_*` queda expuesta en el código descargado por el navegador: no guardes secretos ni claves privadas en el frontend.

El JWT de acceso ya no se guarda en `localStorage`: el backend lo emite en una cookie HttpOnly y el frontend envía cookies con `credentials: include`, solicitando además el token CSRF del API para las operaciones que modifican datos. En despliegues cross-origin configura `CORS_ALLOWED_ORIGINS` en el backend con el origen exacto de esta web y usa HTTPS.

Para este despliegue de Render, configura en el **Static Site** `VITE_API_BASE_URL=https://encore-de5y.onrender.com/api` y vuelve a desplegar el frontend. En **Redirects/Rewrites** añade reglas **Rewrite** para `/perfil`, `/privacidad` y `/terminos` hacia `/index.html`, de modo que las recargas directas de rutas SPA respondan correctamente. En el **Web Service** de la API configura `CORS_ALLOWED_ORIGINS=https://encore-xpi2.onrender.com`, sin ruta final ni `/api`, junto con `AUTH_COOKIE_SECURE=true` y `AUTH_COOKIE_SAME_SITE=None`. `onrender.com` figura como sufijo privado en la Public Suffix List: los subdominios separados de la web y API son *cross-site*, así que `SameSite=Lax` no enviaría las cookies HttpOnly de sesión ni la cookie CSRF en las llamadas `fetch`. `SameSite=None` exige HTTPS y `Secure`, y algunos navegadores pueden bloquear cookies de terceros; usar dominios propios bajo el mismo sitio registrable es la opción más fiable. Para importar conciertos reales cuando la API arranca, configura además `TICKETMASTER_API_KEY`, `TICKETMASTER_SYNC_ENABLED=true` y `TICKETMASTER_SYNC_CITIES` (por defecto se consultan Madrid, Barcelona, Valencia, Sevilla, Bilbao, Zaragoza, Málaga, Alicante y Granada). La sincronización comienza en segundo plano cuando el servidor ya está disponible; revisa los logs del backend para ver cuántos eventos nuevos se importaron. La búsqueda pagina los resultados de Ticketmaster, con un máximo de 200 por página y los límites/cuotas del proveedor. CORS debe permitir el origen exacto de la página web; no uses `*`, especialmente porque la sesión usa cookies. El sufijo `/api` sí es necesario en la URL base de la API: las rutas de autenticación son, por ejemplo, `/api/auth/me`. En modo Stripe, solo se muestran tickets de inventario de Encore; las filas demo antiguas no se ofertan ni se pueden comprar. Las cantidades y límites se validan en backend contra la base de datos. Un aviso `ERR_BLOCKED_BY_CLIENT` en `r.stripe.com/b` suele corresponder a un bloqueador del navegador; no es prueba por sí solo de que el pago haya fallado. Valida el resultado con el estado de la orden y el webhook de Stripe. La base de datos Supabase se configura en el backend con su URL JDBC SSL; no se guarda ninguna clave en el frontend ni en `.gitignore`. Consulta [README principal](../README.md) para la guía completa y [guía de la API](../encore-api/README.md) para variables, webhook y despliegue paso a paso.

La cartelera comienza en Barcelona, pero permite elegir cualquiera de las ciudades disponibles en `/api/events/cities` o ver todos los lugares. Al escribir, espera 300 ms y consulta el backend con el texto y ciudad actuales; las respuestas anteriores se cancelan para no reemplazar resultados más recientes. El backend busca por nombre de concierto, artista, sala y ciudad. El perfil está en `/perfil`: permite actualizar el nombre con la sesión autenticada y, fuera del modo demo, gestionar la tarjeta principal mediante SetupIntents. El asistente flotante envía las consultas a `/api/support/ask`, el endpoint IA/RAG existente; requiere que Gemini y pgvector estén configurados en el backend.

## Privacidad, analítica y SEO

- GA4 está desactivado por defecto y solo carga después de que la persona acepte analítica en el banner. Configura `VITE_GA4_MEASUREMENT_ID=G-...`; no se manda un ID inventado ni se inicia analítica sin consentimiento.
- Configura `VITE_SITE_URL=https://tu-dominio` en el entorno de producción. Con esta variable, el build emite etiquetas canónicas por página y `sitemap.xml`; `robots.txt` apunta al sitemap. Sin el dominio, no se genera un sitemap con una URL ficticia.
- La página principal incluye título, descripción y metadatos sociales. Las páginas de privacidad, términos, perfil y error actualizan los metadatos en el cliente; perfil y 404 están marcados `noindex`. Los previsualizadores que no ejecutan JavaScript todavía mostrarán las etiquetas estáticas de la página principal; para una vista previa individual por URL hace falta renderizado SSR o prerenderizado.
- El banner permite aceptar o rechazar analítica, guardar la elección y volver a configurarla desde el pie de página. El texto legal es un **borrador**, porque faltan los datos del responsable, el contacto, la jurisdicción y las reglas reales de cancelación y reembolso.
- Las imágenes remotas se solicitan a resoluciones reducidas cuando proceden de Unsplash. Las tarjetas cargan imágenes bajo demanda, incluyen texto alternativo y dimensiones. Los originales externos de Ticketmaster no pueden recomprimirse desde esta aplicación: hace falta soporte del proveedor, CDN o backend.

## Seguridad y despliegue: responsabilidades pendientes

**Frontend implementado:** redirige a HTTPS en builds de producción cuando se visita desde HTTP; excluye `localhost`, valida formularios en navegador, muestra una página 404 de reemplazo para hosts estáticos y añade una trampa honeypot básica al formulario de autenticación.

**Infraestructura/backend necesario antes de producción:**

- Configurar TLS real en el hosting o proxy inverso, HSTS, cabeceras CSP y redirección HTTP → HTTPS también en el dominio de la API. La redirección del frontend no reemplaza TLS.
- Añadir limitación de peticiones de registro, recuperación de cuenta y pago en el backend. El backend existente limita intentos de **login** en memoria; el honeypot del navegador no detiene un bot que envía peticiones directamente a la API.
- El backend ya usa cookies HttpOnly, Secure configurable, SameSite configurable y protección CSRF. Antes de producción hay que comprobar las políticas de cookies en los dominios reales, configurar CORS de origen exacto, revisar las duraciones de sesión y añadir mitigación de bots al registro.
- Configurar la clave privada de Stripe, Ticketmaster y Gemini exclusivamente en el backend/entorno del servidor. Publicar solo la clave pública de Stripe en `VITE_STRIPE_PUBLISHABLE_KEY`.
- Configurar en el hosting las reglas de reescritura para servir la SPA en `/privacidad`, `/terminos` y mostrar `public/404.html` para rutas inexistentes. Los hosts de archivo estático varían; la pantalla 404 de React depende de la reescritura SPA de cada hosting.
- Habilitar compresión HTTP Brotli/gzip en el CDN o servidor. Vite optimiza los bundles durante el build, pero la compresión de las respuestas la controla el hosting.

## Variables de entorno

| Variable | Uso | Secreto |
| --- | --- | --- |
| `VITE_STRIPE_PUBLISHABLE_KEY` | Stripe Elements en el navegador | No; es una clave pública |
| `VITE_API_BASE_URL` | URL base de la API, `/api` por defecto | No |
| `VITE_SITE_URL` | Dominio HTTPS canónico, sitemap y robots | No |
| `VITE_GA4_MEASUREMENT_ID` | GA4, solo después del consentimiento | No |
| `VITE_CONTACT_EMAIL` | Dirección de contacto legal, si se añade al sitio | No |

Todos los textos de privacidad y términos que contienen corchetes deben completarse y validarse antes de aceptar compras de usuarios reales.
