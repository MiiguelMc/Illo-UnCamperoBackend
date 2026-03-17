package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.illouncampero.Backend.model.LineaPedido;
import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final Firestore db;
    private final ProductoService productoService;
    private final NotificacionService notificacionService;

    public PedidoService(Firestore db, ProductoService productoService, NotificacionService notificacionService) {
        this.db = db;
        this.productoService = productoService;
        this.notificacionService = notificacionService;
    }

    public String guardarNuevoPedido(Pedido pedido) throws Exception {
        if (pedido.getProductos() == null || pedido.getProductos().isEmpty()) {
            throw new Exception("El pedido no tiene productos");
        }

        double totalCalculado = 0;
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

    public List<Pedido> obtenerPedidosPorUsuario(String uid) throws Exception {
        return db.collection("pedidos")
                .whereEqualTo("idUsuario", uid)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());
    }

    public List<Pedido> obtenerPedidosActivos() throws Exception {
        return db.collection("pedidos")
                .whereIn("estado", List.of("PENDIENTE", "COCINANDO", "REPARTO"))
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());
    }

    public String actualizarEstado(String idPedido, String nuevoEstado) throws Exception {
        List<String> estadosValidos = List.of("PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO", "CANCELADO");
        String estadoMayus = nuevoEstado.toUpperCase();

        if (!estadosValidos.contains(estadoMayus)) {
            throw new Exception("Estado no válido: " + nuevoEstado + ". Opciones: " + estadosValidos);
        }

        db.collection("pedidos").document(idPedido).update("estado", estadoMayus);

        Pedido pedido = obtenerPorId(idPedido);
        if (pedido != null && pedido.getIdUsuario() != null) {
            var usuarioDoc = db.collection("usuarios").document(pedido.getIdUsuario()).get().get();
            if (usuarioDoc.exists()) {
                String fcmToken = usuarioDoc.getString("fcmToken");
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    String titulo = obtenerTituloNotificacion(estadoMayus);
                    String cuerpo = obtenerCuerpoNotificacion(estadoMayus, idPedido);
                    notificacionService.enviarNotificacion(fcmToken, titulo, cuerpo);
                }
            }
        }

        return "Pedido " + idPedido + " actualizado a " + estadoMayus;
    }

    private String obtenerTituloNotificacion(String estado) {
        return switch (estado) {
            case "COCINANDO" -> "Tu pedido está en cocina";
            case "REPARTO"   -> "Tu pedido está en camino";
            case "ENTREGADO" -> "Pedido entregado";
            case "CANCELADO" -> "Pedido cancelado";
            default          -> "Actualización de tu pedido";
        };
    }

    private String obtenerCuerpoNotificacion(String estado, String idPedido) {
        String idCorto = idPedido.substring(0, 8).toUpperCase();
        return switch (estado) {
            case "COCINANDO" -> "El pedido #" + idCorto + " ya está siendo preparado.";
            case "REPARTO"   -> "El pedido #" + idCorto + " ha salido. Llegará pronto.";
            case "ENTREGADO" -> "El pedido #" + idCorto + " ha sido entregado. Buen provecho.";
            case "CANCELADO" -> "El pedido #" + idCorto + " ha sido cancelado.";
            default          -> "Tu pedido #" + idCorto + " ha cambiado de estado a " + estado;
        };
    }

    public Pedido obtenerPorId(String id) throws Exception {
        return db.collection("pedidos").document(id).get().get().toObject(Pedido.class);
    }

    public Map<String, Object> obtenerVentasHoy() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long comienzoDelDia = cal.getTimeInMillis();

        List<Pedido> pedidosDeHoy = db.collection("pedidos")
                .whereGreaterThanOrEqualTo("fecha", comienzoDelDia)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .collect(Collectors.toList());

        double dineroTotal = 0;
        for (Pedido p : pedidosDeHoy) {
            if (!"CANCELADO".equals(p.getEstado())) {
                dineroTotal += p.getTotal();
            }
        }

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalDinero", dineroTotal);
        estadisticas.put("totalPedidos", pedidosDeHoy.size());
        return estadisticas;
    }
}
