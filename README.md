# Encore

Plataforma web de descubrimiento y venta de entradas para conciertos, construida con React y Spring Boot. Incluye catálogo de eventos, cuentas de usuario, reservas de entradas, modo de compra demo sin cargos, integración opcional con Stripe y un asistente de soporte con IA.

## Stack

- Frontend: React, Vite y Stripe Elements.
- Backend: Java 21, Spring Boot, Spring Security y JWT en cookies HttpOnly.
- Persistencia: PostgreSQL; Supabase es una opción de alojamiento.
- Integraciones opcionales: Ticketmaster, Google Gemini y Stripe.

## Ejecutar localmente

Consulta [instrucciones del backend](./encore-api/README.md) para configurar PostgreSQL, las variables de entorno y Docker Compose. Luego ejecuta el frontend desde `frontend` con `npm install` y `npm run dev`.

El modo de compra demo está activado por defecto. Genera órdenes y entradas de demostración sin crear PaymentIntents ni cobrar dinero. Para una demostración de portfolio, deja `PAYMENTS_DEMO_MODE=true`; las órdenes se marcan como demo y no como ventas pagadas.

## Despliegue

Hay una guía de configuración manual de Render + Supabase, variables requeridas, dominios, cookies y webhook Stripe en [README del backend](./encore-api/README.md#despliegue-manual-en-render--supabase). No se incluye `render.yaml` ni se han creado servicios de hosting; configurar esos servicios y validar un despliegue real sigue siendo un paso manual.

**Importante sobre el checkout actual:** la raíz Git conectada al remoto `Encore-APIs` es `encore-api`; `frontend` y este README están fuera de ese repositorio. La API puede desplegarse desde la raíz del repo existente, pero Render no podrá construir el frontend desde ese repo hasta que publiques el frontend en su propio repositorio o consolides ambos directorios en un repositorio común. El `.gitignore` dentro de `encore-api` protege los secretos de ese repositorio; el `.gitignore` de la raíz y `frontend/.gitignore` solo aplican al integrar/publicar esos directorios bajo sus respectivos repositorios.

Si habilitas Stripe, registra `https://<api>.onrender.com/api/payments/webhook` como webhook, selecciona `payment_intent.succeeded` y configura el secreto `whsec_...` únicamente en el backend. En modo demo no se necesita webhook ni una cuenta de Stripe configurada.

## Documentación

- [API: desarrollo, configuración y despliegue](./encore-api/README.md)
- [Frontend: desarrollo, seguridad y variables](./frontend/README.md)

## GitHub repository description

Encore — concert discovery and ticketing platform built with React and Spring Boot, featuring secure HttpOnly-cookie authentication, a no-charge demo checkout, optional Stripe payments, and AI-powered support.
