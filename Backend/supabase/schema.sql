-- =====================================================================
-- Illo Un Campero — Esquema Postgres para Supabase
-- Sustituye a las colecciones de Firestore:
--   usuarios, productos, pedidos (+ lineas), cupones, resenas, config/tienda
--
-- Nombres de columna en snake_case para que casen con la estrategia
-- de nombres por defecto de Hibernate (CamelCaseToUnderscoresNamingStrategy):
--   imagenUrl -> imagen_url, esOferta -> es_oferta, idUsuario -> id_usuario, etc.
--
-- IDs como TEXT (no uuid): el codigo Java trabaja con String en todos los IDs
-- y el id de usuario es el "sub" del JWT de Supabase (uuid en formato string).
-- Se guarda tal cual como text para no introducir conversiones por todo el codigo.
--
-- El backend Spring se conecta por la cadena de conexion directa de Postgres
-- (rol propietario), por lo que BYPASEA RLS. Activamos RLS sin politicas para
-- que la API auto-generada de Supabase (anon/authenticated) NO pueda tocar
-- estas tablas: solo el backend accede a ellas.
-- =====================================================================

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------
-- usuarios  (perfil; el id es el "sub" (uuid) del usuario de Supabase Auth)
-- ---------------------------------------------------------------------
create table if not exists public.usuarios (
    id          text primary key,
    nombre      text not null,
    apellidos   text,
    email       text not null,
    telefono    text,
    direccion   text,
    rol         text not null default 'CLIENTE'   -- CLIENTE | ADMIN | COCINA
);

-- ---------------------------------------------------------------------
-- push_subscriptions  (sustituye al fcm_token; Web Push / VAPID)
-- Un usuario puede tener varios dispositivos -> tabla aparte.
-- ---------------------------------------------------------------------
create table if not exists public.push_subscriptions (
    id          bigint generated always as identity primary key,
    usuario_id  text not null references public.usuarios(id) on delete cascade,
    endpoint    text not null unique,
    p256dh      text not null,
    auth        text not null,
    creado_en   timestamptz not null default now()
);
create index if not exists idx_push_usuario on public.push_subscriptions(usuario_id);

-- ---------------------------------------------------------------------
-- productos
-- ---------------------------------------------------------------------
create table if not exists public.productos (
    id            text primary key default gen_random_uuid()::text,
    nombre        text not null,
    descripcion   text,
    precio        numeric(10,2) not null default 0,
    imagen_url    text,
    categoria     text,
    subcategoria  text not null,
    disponible    boolean not null default true,
    es_oferta     boolean not null default false
);

-- ---------------------------------------------------------------------
-- cupones
-- ---------------------------------------------------------------------
create table if not exists public.cupones (
    id           text primary key default gen_random_uuid()::text,
    codigo       text not null unique,
    descuento    numeric(10,2) not null,
    descripcion  text,
    activo       boolean not null default true
);

-- ---------------------------------------------------------------------
-- pedidos
--   fecha es epoch millis (bigint) para conservar el modelo actual (long).
--   valorado lo usa ResenaController (no estaba en el modelo Java -> se anade).
-- ---------------------------------------------------------------------
create table if not exists public.pedidos (
    id               text primary key default gen_random_uuid()::text,
    id_usuario       text references public.usuarios(id) on delete set null,
    nombre_cliente   text,
    direccion        text,
    telefono         text,
    total            numeric(10,2) not null default 0,
    estado           text not null,   -- PENDIENTE_PAGO|PENDIENTE|COCINANDO|REPARTO|ENTREGADO|CANCELADO
    fecha            bigint not null,
    notas_generales  text,
    metodo_pago      text,
    cupon            text,
    descuento        numeric(10,2),
    valorado         boolean not null default false
);
create index if not exists idx_pedidos_usuario on public.pedidos(id_usuario);
create index if not exists idx_pedidos_estado  on public.pedidos(estado);
create index if not exists idx_pedidos_fecha   on public.pedidos(fecha);

-- ---------------------------------------------------------------------
-- linea_pedido  (era la lista embebida productos[] del Pedido en Firestore)
-- ---------------------------------------------------------------------
create table if not exists public.linea_pedido (
    id             bigint generated always as identity primary key,
    pedido_id      text not null references public.pedidos(id) on delete cascade,
    producto_id    text,
    nombre         text,
    cantidad       int not null default 1,
    precio_unidad  numeric(10,2) not null default 0,
    notas          text
);
create index if not exists idx_linea_pedido on public.linea_pedido(pedido_id);

-- ---------------------------------------------------------------------
-- resenas
-- ---------------------------------------------------------------------
create table if not exists public.resenas (
    id           text primary key default gen_random_uuid()::text,
    id_pedido    text references public.pedidos(id) on delete cascade,
    id_usuario   text references public.usuarios(id) on delete set null,
    puntuacion   int not null check (puntuacion between 1 and 5),
    comentario   text,
    fecha        bigint not null
);
create index if not exists idx_resenas_fecha on public.resenas(fecha);

-- ---------------------------------------------------------------------
-- tienda_config  (era config/tienda en Firestore, una sola fila)
-- ---------------------------------------------------------------------
create table if not exists public.tienda_config (
    id       text primary key default 'tienda',
    abierta  boolean not null default true
);
insert into public.tienda_config (id, abierta)
values ('tienda', true)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------
-- RLS: activar y NO crear politicas -> bloquea anon/authenticated via API.
-- El backend usa la conexion directa (rol propietario) y la bypasea.
-- ---------------------------------------------------------------------
alter table public.usuarios            enable row level security;
alter table public.push_subscriptions  enable row level security;
alter table public.productos           enable row level security;
alter table public.cupones             enable row level security;
alter table public.pedidos             enable row level security;
alter table public.linea_pedido        enable row level security;
alter table public.resenas             enable row level security;
alter table public.tienda_config       enable row level security;
