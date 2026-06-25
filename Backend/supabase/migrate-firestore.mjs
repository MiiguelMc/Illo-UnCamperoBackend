/**
 * Migracion de datos Firestore -> Supabase (Postgres + Auth).
 *
 * Migra: usuarios, productos, cupones, pedidos (+ lineas), resenas, config/tienda.
 * Para los usuarios crea cuentas en Supabase Auth y remapea el UID de Firebase
 * al nuevo id de Supabase en pedidos y resenas.
 *
 * NOTA sobre contrasenas: no se migran los hashes (Firebase usa un scrypt propio).
 * Cada usuario migrado debera usar "He olvidado mi contrasena" para fijar una nueva.
 * (El script puede enviar el email de recuperacion: pon SEND_RECOVERY=true.)
 *
 * --- USO ---
 * 1) cd Backend/supabase && npm install
 * 2) Define variables de entorno:
 *      FIREBASE_JSON              -> contenido del serviceAccountKey.json de Firebase
 *      SUPABASE_DB_URL           -> postgresql://postgres:PASS@db.xxx.supabase.co:5432/postgres
 *      SUPABASE_URL              -> https://xxx.supabase.co
 *      SUPABASE_SERVICE_ROLE_KEY -> service_role key
 *      SEND_RECOVERY             -> (opcional) true para enviar email de reset
 * 3) node migrate-firestore.mjs
 *
 * Idempotente: usa upsert/ON CONFLICT donde puede. Reejecutar no duplica filas,
 * pero SI intentara recrear usuarios en Auth (fallaria silenciosamente si ya existen).
 */

import admin from 'firebase-admin';
import pg from 'pg';
import { randomUUID } from 'node:crypto';

const { Client } = pg;

// ---------------------------------------------------------------------------
const FIREBASE_JSON = process.env.FIREBASE_JSON;
const SUPABASE_DB_URL = process.env.SUPABASE_DB_URL;
const SUPABASE_URL = (process.env.SUPABASE_URL || '').replace(/\/$/, '');
const SERVICE_ROLE = process.env.SUPABASE_SERVICE_ROLE_KEY;
const SEND_RECOVERY = process.env.SEND_RECOVERY === 'true';

for (const [k, v] of Object.entries({ FIREBASE_JSON, SUPABASE_DB_URL, SUPABASE_URL, SERVICE_ROLE })) {
  if (!v) { console.error(`ERROR: falta la variable de entorno ${k}`); process.exit(1); }
}

admin.initializeApp({ credential: admin.credential.cert(JSON.parse(FIREBASE_JSON)) });
const fs = admin.firestore();
const fbAuth = admin.auth();

const db = new Client({ connectionString: SUPABASE_DB_URL, ssl: { rejectUnauthorized: false } });

// uidFirebase -> idSupabase
const uidMap = new Map();

// ---------------------------------------------------------------------------
async function createSupabaseUser(email) {
  const res = await fetch(`${SUPABASE_URL}/auth/v1/admin/users`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SERVICE_ROLE}`,
      'apikey': SERVICE_ROLE,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      email,
      email_confirm: true,
      // Contrasena aleatoria; el usuario la cambiara via "reset password".
      password: randomUUID() + 'Aa1!',
    }),
  });
  const data = await res.json();
  if (!res.ok) {
    // Si el usuario ya existe, intenta localizarlo.
    if (res.status === 422 || (data?.msg || '').includes('already')) {
      const lookup = await fetch(`${SUPABASE_URL}/auth/v1/admin/users?email=${encodeURIComponent(email)}`, {
        headers: { 'Authorization': `Bearer ${SERVICE_ROLE}`, 'apikey': SERVICE_ROLE },
      });
      const list = await lookup.json();
      const existing = (list.users || list)?.find?.(u => u.email === email);
      if (existing) return existing.id;
    }
    throw new Error(`No se pudo crear el usuario ${email}: ${res.status} ${JSON.stringify(data)}`);
  }
  return data.id;
}

async function sendRecovery(email) {
  await fetch(`${SUPABASE_URL}/auth/v1/recover`, {
    method: 'POST',
    headers: { 'apikey': SERVICE_ROLE, 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
}

// ---------------------------------------------------------------------------
async function migrarUsuarios() {
  console.log('-> Usuarios...');
  const snap = await fs.collection('usuarios').get();
  for (const doc of snap.docs) {
    const u = doc.data();
    const email = u.email;
    if (!email) { console.warn(`  (saltado) usuario ${doc.id} sin email`); continue; }

    let newId;
    try {
      newId = await createSupabaseUser(email);
    } catch (e) {
      console.warn(`  (saltado) ${email}: ${e.message}`);
      continue;
    }
    uidMap.set(doc.id, newId);

    await db.query(
      `insert into usuarios (id, nombre, apellidos, email, telefono, direccion, rol)
       values ($1,$2,$3,$4,$5,$6,$7)
       on conflict (id) do update set
         nombre=excluded.nombre, apellidos=excluded.apellidos, email=excluded.email,
         telefono=excluded.telefono, direccion=excluded.direccion, rol=excluded.rol`,
      [newId, u.nombre || 'Usuario', u.apellidos || null, email,
       u.telefono || null, u.direccion || null, (u.rol || 'CLIENTE')]
    );

    if (SEND_RECOVERY) await sendRecovery(email);
  }
  console.log(`   ${uidMap.size} usuarios migrados.`);
}

async function migrarProductos() {
  console.log('-> Productos...');
  const snap = await fs.collection('productos').get();
  for (const doc of snap.docs) {
    const p = doc.data();
    await db.query(
      `insert into productos (id, nombre, descripcion, precio, imagen_url, categoria, subcategoria, disponible, es_oferta)
       values ($1,$2,$3,$4,$5,$6,$7,$8,$9)
       on conflict (id) do update set
         nombre=excluded.nombre, descripcion=excluded.descripcion, precio=excluded.precio,
         imagen_url=excluded.imagen_url, categoria=excluded.categoria, subcategoria=excluded.subcategoria,
         disponible=excluded.disponible, es_oferta=excluded.es_oferta`,
      [doc.id, p.nombre, p.descripcion || null, p.precio || 0, p.imagenUrl || null,
       p.categoria || null, p.subcategoria || 'General', p.disponible !== false, !!p.esOferta]
    );
  }
  console.log(`   ${snap.size} productos.`);
}

async function migrarCupones() {
  console.log('-> Cupones...');
  const snap = await fs.collection('cupones').get();
  for (const doc of snap.docs) {
    const c = doc.data();
    await db.query(
      `insert into cupones (id, codigo, descuento, descripcion, activo)
       values ($1,$2,$3,$4,$5)
       on conflict (id) do update set
         codigo=excluded.codigo, descuento=excluded.descuento,
         descripcion=excluded.descripcion, activo=excluded.activo`,
      [doc.id, c.codigo, c.descuento || 0, c.descripcion || null, c.activo !== false]
    );
  }
  console.log(`   ${snap.size} cupones.`);
}

async function migrarPedidos() {
  console.log('-> Pedidos...');
  const snap = await fs.collection('pedidos').get();
  for (const doc of snap.docs) {
    const p = doc.data();
    const idUsuario = uidMap.get(p.idUsuario) || null; // remapea; si no existe -> null
    await db.query(
      `insert into pedidos (id, id_usuario, nombre_cliente, direccion, telefono, total, estado,
                            fecha, notas_generales, metodo_pago, cupon, descuento, valorado)
       values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
       on conflict (id) do nothing`,
      [doc.id, idUsuario, p.nombreCliente || null, p.direccion || null, p.telefono || null,
       p.total || 0, p.estado || 'PENDIENTE', p.fecha || Date.now(), p.notasGenerales || null,
       p.metodoPago || null, p.cupon || null, p.descuento ?? null, !!p.valorado]
    );

    const lineas = Array.isArray(p.productos) ? p.productos : [];
    for (const l of lineas) {
      await db.query(
        `insert into linea_pedido (pedido_id, producto_id, nombre, cantidad, precio_unidad, notas)
         values ($1,$2,$3,$4,$5,$6)`,
        [doc.id, l.productoId || null, l.nombre || null, l.cantidad || 1, l.precioUnidad || 0, l.notas || null]
      );
    }
  }
  console.log(`   ${snap.size} pedidos.`);
}

async function migrarResenas() {
  console.log('-> Resenas...');
  const snap = await fs.collection('resenas').get();
  for (const doc of snap.docs) {
    const r = doc.data();
    await db.query(
      `insert into resenas (id, id_pedido, id_usuario, puntuacion, comentario, fecha)
       values ($1,$2,$3,$4,$5,$6)
       on conflict (id) do nothing`,
      [doc.id, r.idPedido || null, uidMap.get(r.idUsuario) || null,
       r.puntuacion || 1, r.comentario || '', r.fecha || Date.now()]
    );
  }
  console.log(`   ${snap.size} resenas.`);
}

async function migrarConfig() {
  console.log('-> Config tienda...');
  const doc = await fs.collection('config').doc('tienda').get();
  const abierta = doc.exists ? (doc.data().abierta !== false) : true;
  await db.query(
    `insert into tienda_config (id, abierta) values ('tienda', $1)
     on conflict (id) do update set abierta=excluded.abierta`,
    [abierta]
  );
  console.log(`   tienda abierta=${abierta}.`);
}

// ---------------------------------------------------------------------------
async function main() {
  await db.connect();
  try {
    await migrarUsuarios();   // primero: rellena uidMap
    await migrarProductos();
    await migrarCupones();
    await migrarPedidos();
    await migrarResenas();
    await migrarConfig();
    console.log('\nMigracion completada con exito.');
  } finally {
    await db.end();
  }
}

main().catch(err => { console.error('FALLO la migracion:', err); process.exit(1); });
