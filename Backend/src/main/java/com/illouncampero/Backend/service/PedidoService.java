package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.illouncampero.Backend.model.LineaPedido;
import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final Firestore db;
    private final ProductoService productoService;

    public PedidoService(Firestore db, ProductoService productoService) {
        this.db = db;
        this.productoService = productoService;
    }

    // 1. REALIZAR PEDIDO (Tu código original mejorado)
    public String guardarNuevoPedido(Pedido pedido) throws Exception {
        double totalCalculado = 0;

        if (pedido.getProductos() == null || pedido.getProductos().isEmpty()) {
            throw new Exception("El pedido no tiene productos");
        }

        for (LineaPedido linea : pedido.getProductos()) {
            Producto productoOriginal = productoService.obtenerPorId(linea.getProductoId());

            if (productoOriginal != null) {
                linea.setNombre(productoOriginal.getNombre());
                linea.setPrecioUnidad(productoOriginal.getPrecio());
                totalCalculado += productoOriginal.getPrecio() * linea.getCantidad();
            } else {
                throw new Exception("Producto no encontrado: " + linea.getProductoId());
            }
        }

        pedido.setTotal(totalCalculado);
        pedido.setId(UUID.randomUUID().toString());
        pedido.setEstado("PENDIENTE");
        pedido.setFecha(System.currentTimeMillis());

        db.collection("pedidos").document(pedido.getId()).set(pedido);

        return pedido.getId();
    }

    // 2. OBTENER HISTORIAL DE UN USUARIO (Para la App Móvil)
    // Esto permite que el cliente vea sus pedidos antiguos
    public List<Pedido> obtenerPedidosPorUsuario(String uid) throws Exception {
        return db.collection("pedidos")
                .whereEqualTo("idUsuario", uid)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());
    }

    // 3. OBTENER PEDIDOS ACTIVOS (Para la Web del Restaurante / Cocina)
    // Solo devuelve los pedidos que no han sido entregados ni cancelados
    public List<Pedido> obtenerPedidosActivos() throws Exception {
        return db.collection("pedidos")
                .whereIn("estado", List.of("PENDIENTE", "COCINANDO", "REPARTO"))
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());
    }

    // 4. ACTUALIZAR ESTADO (Para el Administrador)
    // Permite pasar de PENDIENTE -> COCINANDO -> REPARTO -> ENTREGADO
    public String actualizarEstado(String idPedido, String nuevoEstado) throws Exception {
        // 1. Definimos los estados permitidos para evitar errores
        List<String> estadosValidos = List.of("PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO", "CANCELADO");

        String estadoMayus = nuevoEstado.toUpperCase();

        if (!estadosValidos.contains(estadoMayus)) {
            throw new Exception("El estado '" + nuevoEstado + "' no es válido. Usa: " + estadosValidos);
        }

        // 2. Actualizamos solo el campo 'estado' en el documento de Firestore
        db.collection("pedidos").document(idPedido).update("estado", estadoMayus);

        return "Pedido " + idPedido + " actualizado a " + estadoMayus;
    }

    // 5. OBTENER UN PEDIDO POR ID (Para ver detalles específicos)
    public Pedido obtenerPorId(String id) throws Exception {
        return db.collection("pedidos").document(id).get().get().toObject(Pedido.class);
    }

    public java.util.Map<String, Object> obtenerVentasHoy() throws Exception {
        // 1. Calculamos el timestamp de cuando empezó el día de hoy (hora 00:00:00)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long comienzoDelDia = cal.getTimeInMillis();

        // 2. Pedimos a Firebase los pedidos que se han hecho desde que empezó el día
        List<Pedido> pedidosDeHoy = db.collection("pedidos")
                .whereGreaterThanOrEqualTo("fecha", comienzoDelDia)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());

        // 3. Sumamos los totales
        double dineroTotal = 0;
        for (Pedido p : pedidosDeHoy) {
            // Solo sumamos los que no estén cancelados
            if (!p.getEstado().equals("CANCELADO")) {
                dineroTotal += p.getTotal();
            }
        }

        // 4. Guardamos los datos en un "Mapa" para enviarlos juntos
        java.util.Map<String, Object> estadisticas = new java.util.HashMap<>();
        estadisticas.put("totalDinero", dineroTotal);
        estadisticas.put("totalPedidos", pedidosDeHoy.size());

        System.out.println("LOG: Ventas de hoy calculadas. Total: " + dineroTotal + "€");

        return estadisticas;
    }
}