# 🥖 Illo, un campero - API REST (Backend)

Este es el núcleo central del proyecto **Illo, un campero**. Se encarga de la lógica de negocio, la validación de seguridad y la comunicación con Firebase mediante el Admin SDK.

## 🚀 Tecnologías
* **Java 21** & **Spring Boot 3.5.x**
* **Spring Security** (Protección de rutas)
* **Firebase Admin SDK** (Gestión de base de datos y auth)
* **Lombok** (Productividad)
* **Swagger/OpenAPI** (Documentación de la API)

## 🛠️ Instalación y Configuración
1. Clona el repositorio: `git clone https://github.com/TuUsuario/Illo-UnCamperoBackend.git`
2. Añade tu archivo de claves `ServiceAccountKey.json` en `src/main/resources/`.
3. Ejecuta el proyecto: `./mvnw spring-boot:run`

## 📖 Documentación de la API
Una vez arrancado el servidor, puedes consultar los endpoints disponibles en:
`http://localhost:8080/swagger-ui/index.html`

## 🛡️ Seguridad
El servidor utiliza Spring Security para validar las peticiones y está configurado para permitir comunicación (CORS) con el frontend de Angular y la App de Android.
