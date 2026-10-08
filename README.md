# Encore

Plataforma web de descubrimiento y venta de entradas para conciertos, construida con **React** y **Spring Boot**. Incluye catálogo de eventos, cuentas de usuario, reservas de entradas, modo de compra demo sin cargos, integración opcional con Stripe y un asistente de soporte con IA.

## Stack

| Capa | Tecnologías |
|---|---|
| **Frontend** | React, Vite y Stripe Elements |
| **Backend** | Java 21, Spring Boot, Spring Security y JWT mediante cookies HttpOnly |
| **Persistencia** | PostgreSQL, alojado opcionalmente mediante Supabase |
| **Integraciones opcionales** | Ticketmaster, Google Gemini y Stripe |

## Estructura del proyecto

```text
Encore/
├── backend/       # API REST con Spring Boot
├── frontend/      # Aplicación React + Vite
├── README.md
└── .gitignore
```

## Ejecutar localmente

### Backend

Consulta [`backend/README.md`](backend/README.md) para configurar PostgreSQL, las variables de entorno y Docker Compose.

Desde `backend` puedes ejecutar:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### Frontend

Desde `frontend`:

```bash
npm install
npm run dev
```

El frontend se ejecutará mediante Vite y se conectará a la API configurada mediante sus variables de entorno.

## Modo de compra demo

El modo de compra demo está **activado por defecto**.

Cuando `PAYMENTS_DEMO_MODE=true`, las compras generan órdenes y entradas de demostración sin crear `PaymentIntent` ni realizar cargos reales.

Para una demostración de portfolio se recomienda mantener:

```env
PAYMENTS_DEMO_MODE=true
```

Las órdenes realizadas en este modo se identifican como operaciones demo y no como pagos reales.

## Despliegue

El proyecto está preparado para desplegar el frontend y el backend desde el mismo repositorio.

### Arquitectura de producción

```text
                    GitHub
                       │
                  Encore repository
                       │
          ┌────────────┴────────────┐
          │                         │
          ▼                         ▼
   Render Backend            Render Frontend
   Spring Boot               React + Vite
          │
          ▼
       Supabase
      PostgreSQL
```

### Backend

El backend se encuentra en `backend/`.

En Render debe configurarse este directorio como **Root Directory** del servicio del backend.

La configuración de build, ejecución y variables de entorno está documentada en [`backend/README.md`](backend/README.md).

### Frontend

El frontend se encuentra en `frontend/`.

En Render debe configurarse este directorio como **Root Directory** del servicio del frontend.

El frontend debe configurarse para utilizar la URL pública del backend desplegado en Render.

### Supabase

Supabase puede utilizarse como proveedor de PostgreSQL para el backend.

Las credenciales y variables de conexión deben configurarse como variables de entorno en Render y **no deben almacenarse en Git**.

### Stripe

Stripe es **opcional**.

Si se habilitan los pagos reales, registra el webhook:

```text
https://<api>.onrender.com/api/payments/webhook
```

y configura el evento:

```text
payment_intent.succeeded
```

El secreto `whsec_...` debe configurarse únicamente como variable de entorno del backend.

En modo demo no es necesario configurar Stripe ni su webhook.

## Seguridad

Los archivos `.env` contienen información sensible y están excluidos mediante `.gitignore`.

Utiliza los archivos `.env.example` como referencia para conocer las variables necesarias sin incluir sus valores reales.

## Documentación

- [API: desarrollo, configuración y despliegue](backend/README.md)
- [Frontend: desarrollo, seguridad y variables](frontend/README.md)