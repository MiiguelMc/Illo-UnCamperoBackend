# Migración a "cero Firebase" — Supabase + Render + Vercel

Esta guía explica cómo terminar de poner en producción el proyecto **sin Firebase**:

| Capa | Antes | Ahora |
|---|---|---|
| Base de datos | Firestore | **Supabase Postgres** |
| Autenticación | Firebase Auth | **Supabase Auth** |
| Notificaciones | FCM | **Web Push (VAPID)** |
| Hosting backend | Render | Render (sin cambios) |
| Hosting frontend | Firebase Hosting | **Vercel** |

El código ya está migrado. Lo que queda son pasos de infraestructura que haces tú.

---

## Paso 1 — Crear el proyecto Supabase

> ⚠️ El plan gratuito permite **2 proyectos activos por organización** y ya tienes 2
> (`FPTrack` inactivo y `mainake-ecommerce`). Para crear uno nuevo: pausa `FPTrack`
> (Settings → General → Pause project) o sube de plan.

1. https://supabase.com/dashboard → **New project** → nombre `illo-un-campero`, región **eu-west-1** (Irlanda), genera y **guarda la contraseña de la base de datos**.
2. Espera a que el proyecto esté `ACTIVE_HEALTHY`.

## Paso 2 — Crear el esquema

En el dashboard → **SQL Editor** → pega el contenido de `Backend/supabase/schema.sql` → **Run**.
Crea las tablas `usuarios, productos, cupones, pedidos, linea_pedido, resenas, push_subscriptions, tienda_config` con RLS activado.

## Paso 3 — Recoger credenciales

| Valor | Dónde está en Supabase |
|---|---|
| `SUPABASE_URL` | Settings → API → **Project URL** (`https://xxxx.supabase.co`) |
| `SUPABASE_ANON_KEY` | Settings → API → **anon public** |
| `SUPABASE_SERVICE_ROLE_KEY` | Settings → API → **service_role** (¡secreto!) |
| Cadena de conexión BD | Settings → Database → **Connection string → Session pooler** |

> 🛑 **MUY IMPORTANTE (fallo típico de deploy):** usa la **Session pooler** (puerto 5432,
> host `aws-0-eu-west-1.pooler.supabase.com`, usuario `postgres.<ref>`). La conexión
> "Direct" de Supabase es **solo IPv6** y Render no tiene salida IPv6 → fallaría.

De esa cadena saca para el backend:
- `SUPABASE_DB_URL` = `jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:5432/postgres?sslmode=require`
- `SUPABASE_DB_USER` = `postgres.<tu-project-ref>`
- `SUPABASE_DB_PASSWORD` = la contraseña del Paso 1

## Paso 4 — Generar claves VAPID (Web Push)

En tu PC:
```bash
npx web-push generate-vapid-keys
```
Apunta `Public Key` y `Private Key`.

## Paso 5 — Configurar Supabase Auth

- Authentication → **Providers → Email**: activado.
- Authentication → **URL Configuration**:
  - **Site URL**: la URL de Vercel (cuando la tengas, p. ej. `https://illo-un-campero.vercel.app`).
  - **Redirect URLs**: añade esa URL y `http://localhost:4200`.
- (Opcional, para que el registro inicie sesión al instante como antes) Authentication →
  Sign In / Providers → **desactiva "Confirm email"**. Si lo dejas activado, el usuario
  tendrá que confirmar su correo antes de poder entrar.

## Paso 6 — Variables de entorno del BACKEND (Render)

Render → tu servicio → **Environment** → añade:

```
SPRING_PROFILES_ACTIVE=prod
SUPABASE_DB_URL=jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres.<ref>
SUPABASE_DB_PASSWORD=<contraseña-bd>
SUPABASE_URL=https://<ref>.supabase.co
SUPABASE_ANON_KEY=<anon>
SUPABASE_SERVICE_ROLE_KEY=<service_role>
VAPID_PUBLIC_KEY=<public-key>
VAPID_PRIVATE_KEY=<private-key>
VAPID_SUBJECT=mailto:tu-email@dominio.com
APP_CORS_ALLOWED_ORIGINS=https://<tu-app>.vercel.app,http://localhost:4200
STRIPE_SECRET_KEY=<ya la tienes>
STRIPE_WEBHOOK_SECRET=<ya la tienes>
CLOUDINARY_CLOUD_NAME=<si lo usas>
CLOUDINARY_API_KEY=<si lo usas>
CLOUDINARY_API_SECRET=<si lo usas>
```
Ya **no** hace falta `FIREBASE_JSON`. Tras guardar, Render redeploya y compila con Docker.

## Paso 7 — Datos iniciales

La base de datos ya está poblada en Supabase (carta de productos, cupones y datos de
demostración). Si partes de un proyecto Supabase vacío: ejecuta el `schema.sql` del Paso 2
y carga los productos desde el **panel de administración** (rol ADMIN) o con un `INSERT`
en el SQL Editor.

> Los datos provenían originalmente de Firestore; la migración se hizo una sola vez con
> scripts puntuales que ya se han retirado del repo al completarse. Las contraseñas de los
> usuarios **no se migran**: cada uno fija una nueva con "He olvidado mi contraseña".

## Paso 8 — Marcar tu usuario ADMIN

Tras registrarte/migrarte, en Supabase → **Table editor → usuarios** → tu fila → `rol` = `ADMIN`
(o `COCINA`). Para fijar la contraseña del admin sin email: **Authentication → Users →** tu
usuario → **Reset password** / o créalo con contraseña conocida.

## Paso 9 — Desplegar el FRONTEND en Vercel

1. https://vercel.com → **Add New → Project** → importa el repo del frontend.
2. **Root Directory**: `illo-un-campero-web`  ← (el código está en esa subcarpeta).
3. Framework: Angular (o "Other"). El `vercel.json` ya fija build y carpeta de salida.
4. **Environment Variables**:
   ```
   STRIPE_PUBLISHABLE_KEY=<pk_...>
   SUPABASE_URL=https://<ref>.supabase.co
   SUPABASE_ANON_KEY=<anon>
   SITE_URL=https://<tu-app>.vercel.app
   API_URL=https://illo-uncamperobackend.onrender.com/api
   ```
5. **Deploy**. Copia la URL final.

## Paso 10 — Cerrar el círculo

- Pon esa URL de Vercel en `APP_CORS_ALLOWED_ORIGINS` (Render) y en **Site URL / Redirect URLs** (Supabase Auth). Redeploya el backend.
- Stripe Dashboard → Webhooks → apunta a `https://illo-uncamperobackend.onrender.com/api/pagos/webhook` (si no lo estaba ya).

---

## Comprobación rápida

1. `GET https://illo-uncamperobackend.onrender.com/api/health` → `{"status":"UP"}`.
2. `GET .../api/productos` → lista de productos (público).
3. En la web: registro/login, ver carta, hacer un pedido, y (en el panel) cambiar el estado
   de un pedido → debería llegar una notificación push si aceptaste permisos.

## Problemas frecuentes

- **El backend no arranca / error de conexión a BD** → casi seguro estás usando la conexión
  "Direct" (IPv6). Cambia a **Session pooler** (Paso 3).
- **401/403 en endpoints autenticados** → revisa `SUPABASE_URL` y `SUPABASE_ANON_KEY` en Render;
  el backend valida el token contra `SUPABASE_URL/auth/v1/user`.
- **CORS bloqueado** → falta la URL de Vercel en `APP_CORS_ALLOWED_ORIGINS`.
- **No llegan las push** → solo funcionan en HTTPS (Vercel sí), requieren permiso del navegador,
  y las claves VAPID del backend deben coincidir (public/private del mismo par).
