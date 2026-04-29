# Illo Un Campero — Backend

API REST del proyecto TFG. Gestiona la lógica de negocio, valida tokens Firebase y procesa pagos con Stripe.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Admin_SDK-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Stripe](https://img.shields.io/badge/Stripe-Pagos-635BFF?style=for-the-badge&logo=stripe&logoColor=white)

---

## Características

- Autenticación mediante tokens Firebase (Bearer) verificados con el Admin SDK
- CRUD completo de productos con categorías y subcategorías
- Gestión de pedidos con estados (PENDIENTE, EN_COCINA, LISTO, ENTREGADO)
- Creación de PaymentIntents de Stripe para pago con tarjeta
- Validación de cupones de descuento
- Gestión de reseñas de pedidos
- Notificaciones push con FCM
- Gestión de tiendas y usuarios
- Manejo centralizado de errores con GlobalExceptionHandler
- Documentación automática con Swagger/OpenAPI
- CORS configurado para el frontend Angular y la app Android

---

## Stack

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 3.5 |
| Lenguaje | Java 21 |
| Autenticación | Firebase Admin SDK (verificación de tokens) |
| Base de datos | Firebase Firestore (vía Admin SDK) |
| Pagos | Stripe (PaymentIntents) |
| Seguridad | Spring Security + FirebaseFilter |
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
│   ├── FirebaseConfig.java
│   ├── FirebaseFilter.java          # Valida Bearer token en cada petición
│   ├── GlobalExceptionHandler.java  # Manejo centralizado de errores
│   └── SecurityConfig.java
└── BackendApplication.java
```

---

## Instalación

### Requisitos
- Java 21+
- Maven 3.9+
- Cuenta Firebase con Admin SDK habilitado
- Cuenta Stripe (modo test)

### Configuración

1. Coloca el archivo `ServiceAccountKey.json` de Firebase en `Backend/src/main/resources/`

2. En `Backend/src/main/resources/application.properties`:

```properties
stripe.secret.key=sk_test_...
```

3. Arranca el servidor:

```bash
cd Backend
./mvnw spring-boot:run
```

El servidor arranca en `http://localhost:8080`.

---

## Documentación de la API

Con el servidor en marcha:

```
http://localhost:8080/swagger-ui/index.html
```

---

## Seguridad

Todas las rutas protegidas exigen un header `Authorization: Bearer <token>` con un token Firebase válido. El `FirebaseFilter` lo verifica contra el Admin SDK en cada petición antes de que llegue al controlador.

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
