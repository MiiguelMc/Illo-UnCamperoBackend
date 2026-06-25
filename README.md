# Illo Un Campero — Backend

API REST del proyecto TFG. Gestiona la lógica de negocio, valida tokens de Supabase Auth y procesa pagos con Stripe.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-Postgres-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-Pagos-635BFF?style=for-the-badge&logo=stripe&logoColor=white)

---

## Características

- Autenticación mediante tokens de Supabase Auth (Bearer) validados contra `/auth/v1/user`
- Persistencia en Postgres (Supabase) con Spring Data JPA
- CRUD completo de productos con categorías y subcategorías
- Gestión de pedidos con estados (PENDIENTE, COCINANDO, REPARTO, ENTREGADO)
- Creación de PaymentIntents de Stripe para pago con tarjeta
- Validación de cupones de descuento
- Gestión de reseñas de pedidos
- Notificaciones push con Web Push (VAPID)
- Gestión de tiendas y usuarios
- Manejo centralizado de errores con GlobalExceptionHandler
- Documentación automática con Swagger/OpenAPI
- CORS configurado para el frontend Angular y la app Android

---

## Stack

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 3.4 |
| Lenguaje | Java 21 |
| Autenticación | Supabase Auth (validación de token vía `/auth/v1/user`) |
| Base de datos | Supabase Postgres (Spring Data JPA) |
| Notificaciones | Web Push (VAPID) |
| Pagos | Stripe (PaymentIntents) |
| Seguridad | Spring Security + SupabaseJwtFilter |
| Documentación | SpringDoc OpenAPI / Swagger UI |

---

## Estructura del proyecto

```
Backend/src/main/java/com/illouncampero/Backend/
├── controller/
│   ├── CuponController.java
│   ├── PedidoController.java
│   ├── ProductoController.java
│   ├── ResenaController.java
│   ├── StripeController.java
│   ├── TiendaController.java
│   └── UsuarioController.java
├── service/
│   ├── NotificacionService.java
│   ├── PedidoService.java
│   ├── ProductoService.java
│   ├── StripeService.java
│   └── UsuarioService.java
├── model/
│   ├── Cupon.java
│   ├── LineaPedido.java
│   ├── Pedido.java
│   ├── Producto.java
│   ├── Resena.java
│   └── Usuario.java
├── config/
│   ├── SupabaseJwtFilter.java       # Valida el Bearer token de Supabase en cada petición
│   ├── RateLimitFilter.java         # Límite de peticiones (bucket4j)
│   ├── GlobalExceptionHandler.java  # Manejo centralizado de errores
│   └── SecurityConfig.java
└── BackendApplication.java
```

---

## Instalación

### Requisitos
- Java 21+
- Maven 3.9+
- Proyecto Supabase (Postgres + Auth)
- Cuenta Stripe (modo test)

### Configuración

La configuración va por variables de entorno (ver `MIGRACION-SUPABASE.md` para la lista completa):

```bash
SUPABASE_DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres.<ref>
SUPABASE_DB_PASSWORD=...
SUPABASE_URL=https://<ref>.supabase.co
SUPABASE_ANON_KEY=...
SUPABASE_SERVICE_ROLE_KEY=...
VAPID_PUBLIC_KEY=...
VAPID_PRIVATE_KEY=...
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

Crea el esquema ejecutando `Backend/supabase/schema.sql` en el SQL Editor de Supabase y arranca:

```bash
cd Backend
./mvnw spring-boot:run
```

El servidor arranca en `http://localhost:8081`.

---

## Documentación de la API

Con el servidor en marcha:

```
http://localhost:8080/swagger-ui/index.html
```

---

## Seguridad

Todas las rutas protegidas exigen un header `Authorization: Bearer <token>` con un access token de Supabase válido. El `SupabaseJwtFilter` lo valida contra `/auth/v1/user` (con una caché corta) en cada petición antes de que llegue al controlador, y resuelve el rol desde la tabla `usuarios`.

---

## Rutas principales

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/productos` | Público | Lista de productos |
| POST | `/api/productos` | ADMIN | Crear producto |
| PUT | `/api/productos/{id}` | ADMIN | Editar producto |
| DELETE | `/api/productos/{id}` | ADMIN | Eliminar producto |
| GET | `/api/pedidos` | Autenticado | Pedidos del usuario |
| POST | `/api/pedidos` | Autenticado | Crear pedido |
| PUT | `/api/pedidos/{id}/estado` | ADMIN / COCINA | Cambiar estado |
| POST | `/api/stripe/pagar` | Autenticado | Crear PaymentIntent |
| POST | `/api/cupones/validar` | Autenticado | Validar cupón |
| GET | `/api/tiendas` | Público | Lista de tiendas |
| POST | `/api/resenas` | Autenticado | Crear reseña |
