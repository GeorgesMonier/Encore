# Encore API

Backend de una plataforma de venta de entradas para conciertos, construido con Spring Boot. Autenticación robusta, integración con APIs externas reales, pagos, control de concurrencia y un asistente de soporte con RAG.

## Características

- **Autenticación**: registro e inicio de sesión inmediatos sin verificación por email, sesiones JWT en cookies HttpOnly y Secure en producción, CORS restringido y protección CSRF
- **Seguridad**: verificación en dos pasos (TOTP/Google Authenticator), rate limiting contra fuerza bruta
- **Eventos**: catálogo de conciertos sincronizado desde la Ticketmaster Discovery API
- **Compra de entradas**: reserva de stock con bloqueo optimista, expiración de reservas pendientes a los 15 minutos y entradas de demostración
- **Pagos**: modo demo sin cargos por defecto; Stripe (PaymentIntents + webhook y confirmación servidor-servidor) solo al habilitarlo explícitamente
- **Asistente de soporte (RAG)**: responde preguntas sobre la plataforma usando Google Gemini + pgvector
- **Panel de administración**: gestión de usuarios, órdenes, tipos de entrada y estadísticas

## Stack técnico

- Java 21, Spring Boot 4
- Spring Security + JWT (JJWT)
- Spring Data JPA + PostgreSQL + pgvector
- Spring AI (Google Gemini) para RAG
- Stripe Java SDK
- Docker / Docker Compose
- JUnit 5 + Mockito

## Arquitectura

El proyecto sigue una organización por responsabilidad (no por capas técnicas):

- `user` — registro, login, perfil, DTOs de usuario
- `security` — JWT, filtro de autenticación, TOTP, rate limiting
- `event` — integración con Ticketmaster, catálogo de eventos
- `ticket` — tipos de entrada, órdenes, control de stock
- `payment` — Stripe, PaymentIntents, webhook
- `support` — RAG (vector store + chat)
- `admin` — endpoints de administración
- `config` — utilidades transversales

## Cómo levantarlo

### Requisitos
- Docker y Docker Compose
- Java 21 o superior para desarrollo local
- Claves de: Ticketmaster Discovery API y Google AI Studio (Gemini). Stripe solo hace falta si se desactiva el modo demo.

### Variables de entorno

Copia `.env.example` a `.env` y rellena los valores:

```
DB_PASSWORD=
TICKETMASTER_API_KEY=
STRIPE_API_KEY=
STRIPE_WEBHOOK_SECRET=
JWT_SECRET=
GEMINI_API_KEY=
PAYMENTS_DEMO_MODE=
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=http://localhost:5173
DB_USER=encore_user
```

`AUTH_COOKIE_SECURE=false` y `AUTH_COOKIE_SAME_SITE=Lax` son solo para desarrollo local por HTTP. Para los dominios independientes `*.onrender.com`, configura en el Web Service `AUTH_COOKIE_SECURE=true`, `AUTH_COOKIE_SAME_SITE=None` y `CORS_ALLOWED_ORIGINS=https://encore-xpi2.onrender.com` (solo el origen del frontend, sin ruta final ni `/api`). `onrender.com` es un sufijo privado, por lo que los subdominios del Static Site y API son sitios distintos; `SameSite=Lax` impediría enviar las cookies de sesión/CSRF en las llamadas `fetch` cross-site. `SameSite=None` requiere HTTPS y `Secure`; la aplicación rechaza la combinación insegura. Los navegadores que bloqueen cookies de terceros pueden requerir dominios propios bajo el mismo sitio registrable. El frontend debe usar `VITE_API_BASE_URL=https://encore-de5y.onrender.com/api`; ese `/api` forma parte de las rutas del backend. CORS permite cookies solo para los orígenes explícitos configurados; no uses `*`.

El registro crea la cuenta y devuelve la sesión autenticada en la cookie HttpOnly inmediatamente. No se envían correos ni se requiere verificación: cualquier correo se acepta como identificador de acceso, así que la gestión o verificación de emails puede añadirse desde Supabase por separado si se cambia este flujo.

`PAYMENTS_DEMO_MODE=true` permite confirmar compras de muestra y nunca crea PaymentIntents ni cargos. Si se omite la variable, el backend usa Stripe cuando `STRIPE_API_KEY` está configurada y vuelve al modo demo cuando no hay clave; `PAYMENTS_DEMO_MODE=true` siempre fuerza el modo demo. En modo demo se añaden tipos de entrada de demostración (45 € general y 90 € VIP, con inventario ficticio) a los eventos, pero el catálogo ya no inventa conciertos. Para traer conciertos, configura `TICKETMASTER_API_KEY`, `TICKETMASTER_SYNC_ENABLED=true` y `TICKETMASTER_SYNC_CITIES` con nombres separados por comas. Cuando el servidor ya está disponible, la sincronización se ejecuta en segundo plano y consulta todas las páginas disponibles (hasta 200 eventos por página) para cada ciudad; la sincronización manual `POST /api/events/sync` sigue disponible para administradores. Ticketmaster limita los resultados y las llamadas según su API y cuota, así que “todos” significa los resultados que devuelve la búsqueda para esas ciudades, no todos los conciertos mundiales. Las compras demo quedan como `DEMO`, no cuentan como ventas pagadas ni como ingresos y se muestran claramente como demostración. Para activar Stripe con claves de prueba, configura `STRIPE_API_KEY=sk_test_...` en el backend y la clave pública correspondiente `VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...` en el build del frontend. También puedes fijar `PAYMENTS_DEMO_MODE=false`; si lo haces, el backend exige la clave secreta. No uses claves live para una demo de portfolio.
Los eventos vienen de Ticketmaster, pero Ticketmaster Discovery no proporciona el inventario vendible de Encore. La capacidad y los precios se configuran internamente en `ticket_types`; las categorías son `NORMAL` o `VIP`, y cada tipo tiene moneda ISO-4217. La API devuelve capacidad total, unidades disponibles, reservadas y vendidas, junto al límite de compra por usuario. Los límites predeterminados son 10 `NORMAL` y 5 `VIP` por evento; se pueden ajustar mediante `TICKETS_NORMAL_PURCHASE_LIMIT` y `TICKETS_VIP_PURCHASE_LIMIT` en el backend. No se crea stock a partir de los datos de Ticketmaster. En modo Stripe no se exponen ni se venden filas antiguas marcadas como `(demo)`.

Flyway aplica `src/main/resources/db/migration/V1__ticket_inventory.sql` antes de iniciar JPA y establece una línea base 0 para la base existente. La migración conserva `available_quantity`, añade categoría, moneda, unidades reservadas/vendidas, moneda a las órdenes y una clave de idempotencia única por usuario. Solo clasifica nombres heredados que identifiquen explícitamente `VIP`, `NORMAL` o `GENERAL`, y aborta si encuentra otro nombre o si los contadores actuales no coinciden con el historial de órdenes; en ese caso, reconcilia los datos antes de desplegar en vez de inventar una categoría o unidades. Revísala contra el esquema de PostgreSQL de destino antes del primer despliegue; el código local no tiene conexión a la base de Render. Las reservas se descuentan en transacción con bloqueo optimista, los pagos solo pasan de reservados a vendidos después del webhook verificado, y cancelaciones de órdenes pagadas solicitan reembolso Stripe antes de liberar inventario. El cliente manda un UUID `idempotencyKey` por intento de compra para que los reintentos de red reusen la orden sin duplicar reservas.

Para Supabase configura `SPRING_DATASOURCE_URL` como una URL JDBC de PostgreSQL con SSL (`jdbc:postgresql://...?...sslmode=require`), además de `DB_USER` y `DB_PASSWORD`, en el entorno de Render. No subas archivos `.env` ni claves: `.env.example` es únicamente una plantilla. El `.gitignore` de este repositorio excluye secretos locales y artefactos compilados; no configura por sí solo los servicios de Render o Supabase.

### Despliegue manual en Render + Supabase

Este repositorio no incluye `render.yaml`: los servicios, el dominio, la base de datos y los secretos se tienen que crear/configurar en los paneles de Render y Supabase. En el checkout actual, `encore-api` es la raíz Git conectada a `Encore-APIs`; para desplegar esa API desde ese repositorio deja **Root Directory** vacío. Usa `encore-api` solo si primero publicas ambas carpetas dentro de un único repositorio cuya raíz contiene `frontend` y `encore-api`.

1. En Supabase crea un proyecto, habilita la extensión `vector` desde Database → Extensions y copia los datos de conexión PostgreSQL SSL. Para Render, usa una cadena JDBC con el host/puerto/tipo de conexión indicados por Supabase, por ejemplo `jdbc:postgresql://<host>:5432/postgres?sslmode=require`; configura `SPRING_DATASOURCE_URL`, `DB_USER` y `DB_PASSWORD` como variables privadas en Render. Si usas el pooler, utiliza el usuario/puerto de pooler que muestra Supabase.
2. En Render crea un **Web Service** desde el repositorio, runtime Docker y **Health Check Path** `/api/events/cities`. Deja **Root Directory** vacío si usas el repositorio actual `Encore-APIs`; si publicas la aplicación como un único repo con las carpetas `frontend` y `encore-api`, establece **Root Directory** en `encore-api`. Configura `PORT=8080`; `server.port` también acepta la variable `PORT`.
3. En el Web Service configura `SPRING_DATASOURCE_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `TICKETMASTER_API_KEY`, `TICKETMASTER_SYNC_ENABLED=true`, `TICKETMASTER_SYNC_CITIES=Madrid,Barcelona,Valencia,Sevilla,Bilbao,Zaragoza,Málaga,Alicante,Granada`, `GEMINI_API_KEY`, `CORS_ALLOWED_ORIGINS=https://encore-xpi2.onrender.com`, `AUTH_COOKIE_SECURE=true` y `AUTH_COOKIE_SAME_SITE=None`. Si omites `PAYMENTS_DEMO_MODE`, el backend activa Stripe al detectar `STRIPE_API_KEY`; si no hay clave, mantiene el modo demo. Establece `PAYMENTS_DEMO_MODE=true` para forzar compras demo. La sincronización se inicia en segundo plano después de que el servidor abre el puerto, y amplía el catálogo con las ciudades indicadas. `JWT_SECRET` debe contener al menos 32 bytes aleatorios. No se necesitan variables SMTP ni de verificación de correo.
4. Para la web, crea un **Static Site** desde un repositorio que incluya el frontend. En un repositorio único con ambas carpetas, establece **Root Directory** `frontend`; en un repositorio independiente solo del frontend, déjalo vacío. Usa **Build Command** `npm ci && npm run build` y **Publish Directory** `dist`. Define `VITE_API_BASE_URL=https://encore-de5y.onrender.com/api` y, opcionalmente, `VITE_SITE_URL=https://encore-xpi2.onrender.com`. En **Redirects/Rewrites** del Static Site añade reglas **Rewrite** de `/perfil`, `/privacidad` y `/terminos` a `/index.html`; esto evita respuestas HTTP 404 en recargas o accesos directos a esas rutas. En la API, configura `CORS_ALLOWED_ORIGINS=https://encore-xpi2.onrender.com`: solo el origen del Static Site, sin ruta final ni ruta `/api`.
5. Con los dominios HTTPS `*.onrender.com` de la web y API, usa `AUTH_COOKIE_SAME_SITE=None` y `AUTH_COOKIE_SECURE=true`; son sitios distintos según la Public Suffix List y requieren cookies cross-site. Los navegadores con bloqueo de cookies de terceros podrían impedir el acceso pese a esa configuración; para evitar esa dependencia, usa dominios propios bajo el mismo sitio registrable (por ejemplo `www.ejemplo.com` y `api.ejemplo.com`) y `SameSite=Lax`.
6. Para usar Stripe en modo de prueba, configura `STRIPE_API_KEY=sk_test_...` en el Web Service y `VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...` en el Static Site. Configura también `STRIPE_WEBHOOK_SECRET=whsec_...` con el signing secret del endpoint de webhook de Stripe. Si `PAYMENTS_DEMO_MODE` no existe o está vacío, Stripe se activa automáticamente cuando está configurada la clave secreta. Si la variable está en `true`, elimínala o cámbiala a `false` para comprar con Stripe; después redepliega el servicio backend (y el frontend si cambiaste su clave) y comprueba que `/api/payments/mode` responde `{"demoMode":false}`. El endpoint solo marca la orden pagada tras un webhook firmado válido; comprueba los eventos `payment_intent.succeeded` y sus logs en Stripe/Render. Para mantener compras de muestra, establece `PAYMENTS_DEMO_MODE=true`.

Render puede tardar en despertar una instancia gratuita y la base de datos de Supabase debe seguir activa y permitir la conexión de red desde Render. Revisa los logs de arranque, salud, correo de verificación y una compra demo en el dominio desplegado. No se puede afirmar que el despliegue esté operativo hasta completar esos pasos y probar sus URLs reales.

### Webhook Stripe (solo si activas Stripe)

En el Dashboard de Stripe, crea un destino de webhook para la **URL HTTPS pública de la API**, no para el frontend:

```text
https://<api>.onrender.com/api/payments/webhook
```

Selecciona el evento `payment_intent.succeeded`, guarda el destino y copia su **signing secret** (`whsec_...`) a `STRIPE_WEBHOOK_SECRET` en Render. En el mismo entorno establece `PAYMENTS_DEMO_MODE=false`, `STRIPE_API_KEY=sk_test_...` y la clave pública correspondiente `VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...` en la configuración de build del Static Site; ambas claves deben ser de la misma cuenta y entorno de Stripe. Después de cambiar variables, redepliega los servicios. Para probar, usa datos de tarjeta de prueba de Stripe y confirma que la orden se muestra en “Mis entradas” como `PAID`. Nunca pongas `sk_test`, `sk_live` ni `whsec` en el frontend, GitHub o un README.

Para desarrollo local, instala Stripe CLI y ejecuta `stripe listen --forward-to localhost:8080/api/payments/webhook`; usa el `whsec_...` temporal que imprime CLI como `STRIPE_WEBHOOK_SECRET`. El Dashboard de Stripe no puede llamar directamente a `localhost`.

El webhook verifica firma, importe, moneda, cliente y orden antes de actualizarla. La confirmación del pago desde el navegador también se verifica servidor a servidor con Stripe. Para portfolio se recomienda dejar demo mode activado y no publicar claves live.

### Levantar con Docker

```bash
docker compose up --build
```

La API queda disponible en `http://localhost:8080`.

### Desarrollo local (sin Docker, con IntelliJ)

1. Comprueba que IntelliJ usa un JDK 21 o superior como Project SDK y como SDK del módulo.
2. Levanta solo la base de datos: `docker compose up -d postgres`. Se publica en `localhost:5433` para no interferir con otros PostgreSQL que utilicen el puerto `5432`.
3. Define las variables de entorno de `.env` en la configuración de Run de tu IDE. El archivo `.env` lo carga Docker Compose automáticamente, pero Spring Boot no lo importa por sí solo al ejecutar desde IntelliJ.
4. Ejecuta `EncoreApiApplication`.

### Tests

```bash
./mvnw test
```

## Endpoints principales

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/api/auth/register` | Registro e inicio de sesión inmediatos; establece cookie HttpOnly | Público + CSRF |
| POST | `/api/auth/login` | Login; establece cookie de sesión HttpOnly | Público + CSRF |
| POST | `/api/auth/logout` | Cerrar sesión y borrar la cookie | CSRF |
| GET | `/api/auth/csrf` | Obtener token CSRF para el navegador | Público |
| POST | `/api/auth/login/totp` | Login con código 2FA | Público |
| POST | `/api/auth/totp/setup` | Activar 2FA (genera QR) | Autenticado |
| POST | `/api/auth/totp/confirm` | Confirmar activación 2FA | Autenticado |
| GET | `/api/auth/me` | Perfil propio | Autenticado |
| PATCH | `/api/auth/me` | Actualizar el nombre del perfil (`{"name":"..."}`) | Autenticado + CSRF |
| GET | `/api/events?city=Barcelona&search=...` | Buscar/listar eventos; ciudad y búsqueda son opcionales | Público |
| GET | `/api/events/cities` | Listar ciudades disponibles en el catálogo | Público |
| POST | `/api/events/sync` | Sincronizar con Ticketmaster | ADMIN |
| GET | `/api/ticket-types/event/{id}` | Tipos de entrada de un evento | Público |
| POST | `/api/ticket-types` | Crear tipo de entrada | ADMIN |
| PATCH | `/api/ticket-types/{id}` | Editar tipo de entrada | ADMIN |
| POST | `/api/orders` | Crear una reserva de entradas | Autenticado + CSRF |
| GET | `/api/orders` | Ver mis órdenes | Autenticado |
| GET | `/api/payments/mode` | Consultar modo demo/Stripe | Público |
| POST | `/api/payments/demo-confirm/{orderId}` | Confirmar compra ficticia sin cargo (solo modo demo) | Autenticado + CSRF |
| POST | `/api/payments/create-intent/{orderId}` | Iniciar pago | Autenticado |
| POST | `/api/payments/confirm/{orderId}` | Verificar el PaymentIntent en Stripe y confirmar la orden | Autenticado + CSRF |
| GET | `/api/payments/payment-method` | Consultar la tarjeta principal guardada en Stripe | Autenticado |
| POST | `/api/payments/setup-intent` | Iniciar la verificación segura de una tarjeta | Autenticado |
| POST | `/api/payments/payment-method` | Guardar como principal un SetupIntent verificado (`{"setupIntentId":"..."}`) | Autenticado + CSRF |
| POST | `/api/payments/webhook` | Webhook de Stripe | Stripe (firma verificada) |
| POST | `/api/support/ask` | Consultar al asistente IA (`{"question":"..."}`; máximo 1000 caracteres) | Público |
| GET | `/api/admin/orders` | Ver todas las órdenes | ADMIN |
| POST | `/api/admin/orders/{id}/cancel` | Cancelar orden | ADMIN |
| GET | `/api/admin/users` | Listar usuarios | ADMIN |
| PATCH | `/api/admin/users/{id}/role` | Cambiar rol de usuario | ADMIN |
| GET | `/api/admin/stats` | Estadísticas | ADMIN |

## Notas honestas

- La búsqueda pública admite `city` y `search`; `/api/events/cities` devuelve los lugares disponibles, y omitir `city` permite consultar todos. La API ya no acepta sesiones Bearer desde el navegador: el frontend usa cookies de sesión HttpOnly y CSRF. La gestión de tarjetas usa SetupIntents y Stripe Elements solo fuera del modo demo; Encore no guarda el número completo ni el CVC.
- Se limita el API a 120 peticiones por minuto y por IP de conexión, y el asistente IA a 10 consultas por minuto y por IP. El límite es local a cada instancia y se reinicia al reiniciar el proceso; no reemplaza el WAF/CDN, la protección DDoS del proveedor ni un limitador compartido si se despliegan varias instancias. El webhook de Stripe queda excluido del límite general para no descartar notificaciones legítimas. Detrás de un proxy, configura y prueba la IP de cliente real mediante una cadena de proxies confiables; la API no confía directamente en `X-Forwarded-For`.
- El asistente recupera documentos de la base de conocimiento de soporte, no perfiles, órdenes, contraseñas, claves ni datos de otros usuarios. Las respuestas no deben contener secretos; el contenido de soporte se entrega intencionadamente a los usuarios y debe revisarse antes de publicarse en esa base.
- Este repositorio no incluye configuración de despliegue de Render ni de Supabase, así que no permite confirmar si esos proveedores están activos, si sus reglas de red están cerradas o si hay WAF/rate limits configurados allí. Docker Compose configura PostgreSQL local; las credenciales de producción y las reglas del proveedor deben verificarse por separado.
- El webhook de Stripe verifica la firma; además, el navegador solicita al backend que vuelva a verificar el PaymentIntent antes de marcar la orden como pagada, así el historial no depende de que el webhook llegue antes de mostrar la compra.
- No incluye frontend.
