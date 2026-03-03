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
    private final NotificacionService notificacionService; // ← AÑADIDO

    public PedidoService(Firestore db, ProductoService productoService, NotificacionService notificationService) {
        this.db = db;
        this.productoService = productoService;
        this.notificacionService = notificationService; // ← AÑADIDO
    }

    // 1. REALIZAR PEDIDO
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

    // 2. OBTENER HISTORIAL DE UN USUARIO
    public List<Pedido> obtenerPedidosPorUsuario(String uid) throws Exception {
        return db.collection("pedidos")
                .whereEqualTo("idUsuario", uid)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());
    }

    // 3. OBTENER PEDIDOS ACTIVOS
    public List<Pedido> obtenerPedidosActivos() throws Exception {
        return db.collection("pedidos")
                .whereIn("estado", List.of("PENDIENTE", "COCINANDO", "REPARTO"))
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());
    }

    // 4. ACTUALIZAR ESTADO ← MODIFICADO: ahora envía notificación al cliente
    public String actualizarEstado(String idPedido, String nuevoEstado) throws Exception {
        List<String> estadosValidos = List.of("PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO", "CANCELADO");
        String estadoMayus = nuevoEstado.toUpperCase();

        if (!estadosValidos.contains(estadoMayus)) {
            throw new Exception("El estado '" + nuevoEstado + "' no es válido. Usa: " + estadosValidos);
        }

        // Actualizamos el estado en Firestore
        db.collection("pedidos").document(idPedido).update("estado", estadoMayus);

        // Obtenemos el pedido para saber a qué usuario notificar
        Pedido pedido = obtenerPorId(idPedido);

        if (pedido != null && pedido.getIdUsuario() != null) {
            // Buscamos el fcmToken del usuario en su documento de Firestore
            var usuarioDoc = db.collection("usuarios").document(pedido.getIdUsuario()).get().get();

            if (usuarioDoc.exists()) {
                String fcmToken = usuarioDoc.getString("fcmToken");

                if (fcmToken != null && !fcmToken.isEmpty()) {
                    // Construimos el mensaje según el nuevo estado
                    String titulo = obtenerTituloNotificacion(estadoMayus);
                    String cuerpo  = obtenerCuerpoNotificacion(estadoMayus, idPedido);

                    notificacionService.enviarNotificacion(fcmToken, titulo, cuerpo);
                    System.out.println("LOG: Notificación enviada a usuario " + pedido.getIdUsuario());
                } else {
                    System.out.println("LOG: El usuario no tiene fcmToken registrado");
                }
            }
        }

        return "Pedido " + idPedido + " actualizado a " + estadoMayus;
    }

    // Mensajes de notificación por estado
    private String obtenerTituloNotificacion(String estado) {
        return switch (estado) {
            case "COCINANDO"  -> "🍳 ¡Tu pedido está en cocina!";
            case "REPARTO"    -> "🛵 ¡Tu pedido está en camino!";
            case "ENTREGADO"  -> "✅ ¡Pedido entregado!";
            case "CANCELADO"  -> "❌ Pedido cancelado";
            default           -> "📦 Actualización de tu pedido";
        };
    }

    private String obtenerCuerpoNotificacion(String estado, String idPedido) {
        String idCorto = idPedido.substring(0, 8).toUpperCase();
        return switch (estado) {
            case "COCINANDO"  -> "El pedido #" + idCorto + " ya está siendo preparado. ¡Paciencia!";
            case "REPARTO"    -> "El pedido #" + idCorto + " ha salido. Llegará pronto.";
            case "ENTREGADO"  -> "El pedido #" + idCorto + " ha sido entregado. ¡Buen provecho!";
            case "CANCELADO"  -> "El pedido #" + idCorto + " ha sido cancelado. Contacta con nosotros si tienes dudas.";
            default           -> "Tu pedido #" + idCorto + " ha cambiado de estado a " + estado;
        };
    }

    // 5. OBTENER UN PEDIDO POR ID
    public Pedido obtenerPorId(String id) throws Exception {
        return db.collection("pedidos").document(id).get().get().toObject(Pedido.class);
    }

    public java.util.Map<String, Object> obtenerVentasHoy() throws Exception {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long comienzoDelDia = cal.getTimeInMillis();

        List<Pedido> pedidosDeHoy = db.collection("pedidos")
                .whereGreaterThanOrEqualTo("fecha", comienzoDelDia)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());

        double dineroTotal = 0;
        for (Pedido p : pedidosDeHoy) {
            if (!p.getEstado().equals("CANCELADO")) {
                dineroTotal += p.getTotal();
            }
        }

        java.util.Map<String, Object> estadisticas = new java.util.HashMap<>();
        estadisticas.put("totalDinero", dineroTotal);
        estadisticas.put("totalPedidos", pedidosDeHoy.size());

        System.out.println("LOG: Ventas de hoy calculadas. Total: " + dineroTotal + "€");

        return estadisticas;
    }
}