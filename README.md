# Encore API

Backend de una plataforma de venta de entradas para conciertos, construido con Spring Boot. Autenticación robusta, integración con APIs externas reales, pagos, control de concurrencia y un asistente de soporte con RAG.

## Características

- **Autenticación**: registro con verificación de email real, login con JWT, roles (USER/ADMIN)
- **Seguridad**: verificación en dos pasos (TOTP/Google Authenticator), rate limiting contra fuerza bruta
- **Eventos**: catálogo de conciertos sincronizado desde la Ticketmaster Discovery API
- **Compra de entradas**: reserva de stock con bloqueo optimista para evitar overselling, expiración automática de órdenes no pagadas
- **Pagos**: integración con Stripe (PaymentIntents + webhook de confirmación)
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
- `email`, `config` — utilidades transversales

## Cómo levantarlo

### Requisitos
- Docker y Docker Compose
- Claves de: Mailtrap (o SMTP propio), Ticketmaster Discovery API, Stripe (modo test), Google AI Studio (Gemini)

### Variables de entorno

Copia `.env.example` a `.env` y rellena los valores:

\```
DB_PASSWORD=
MAIL_PASSWORD=
TICKETMASTER_API_KEY=
STRIPE_API_KEY=
STRIPE_WEBHOOK_SECRET=
JWT_SECRET=
GEMINI_API_KEY=
\```

### Levantar con Docker

\```bash
docker compose up --build
\```

La API queda disponible en `http://localhost:8080`.

### Desarrollo local (sin Docker, con IntelliJ)

1. Levanta solo la base de datos: `docker compose up -d postgres`
2. Define las variables de entorno de arriba en la configuración de Run de tu IDE
3. Ejecuta `EncoreApiApplication`

### Tests

\```bash
./mvnw test
\```

## Endpoints principales

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/api/auth/register` | Registro de usuario | Público |
| GET | `/api/auth/verify` | Verificación de email | Público |
| POST | `/api/auth/login` | Login | Público |
| POST | `/api/auth/login/totp` | Login con código 2FA | Público |
| POST | `/api/auth/totp/setup` | Activar 2FA (genera QR) | Autenticado |
| POST | `/api/auth/totp/confirm` | Confirmar activación 2FA | Autenticado |
| GET | `/api/auth/me` | Perfil propio | Autenticado |
| GET | `/api/events` | Listar eventos | Público |
| POST | `/api/events/sync` | Sincronizar con Ticketmaster | ADMIN |
| GET | `/api/ticket-types/event/{id}` | Tipos de entrada de un evento | Público |
| POST | `/api/ticket-types` | Crear tipo de entrada | ADMIN |
| PATCH | `/api/ticket-types/{id}` | Editar tipo de entrada | ADMIN |
| POST | `/api/orders` | Comprar entradas | Autenticado |
| GET | `/api/orders` | Ver mis órdenes | Autenticado |
| POST | `/api/payments/create-intent/{orderId}` | Iniciar pago | Autenticado |
| POST | `/api/payments/webhook` | Webhook de Stripe | Stripe (firma verificada) |
| POST | `/api/support/ask` | Preguntar al asistente | Público |
| GET | `/api/admin/orders` | Ver todas las órdenes | ADMIN |
| POST | `/api/admin/orders/{id}/cancel` | Cancelar orden | ADMIN |
| GET | `/api/admin/users` | Listar usuarios | ADMIN |
| PATCH | `/api/admin/users/{id}/role` | Cambiar rol de usuario | ADMIN |
| GET | `/api/admin/stats` | Estadísticas | ADMIN |

## Notas honestas

- El webhook de Stripe está implementado y verifica la firma correctamente, pero no se ha probado contra un pago real de Stripe porque el proyecto no está desplegado con una URL pública. Se activaría configurando el endpoint en el Dashboard de Stripe apuntando a la URL del deploy.
- No incluye frontend.

