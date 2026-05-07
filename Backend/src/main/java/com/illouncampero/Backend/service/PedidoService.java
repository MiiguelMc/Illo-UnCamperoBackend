package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.illouncampero.Backend.model.LineaPedido;
import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.model.dto.CrearPedidoRequest;
import com.illouncampero.Backend.model.dto.LineaPedidoRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final Firestore db;
    private final ProductoService productoService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    public PedidoService(Firestore db, ProductoService productoService,
                         NotificacionService notificacionService, EmailService emailService) {
        this.db = db;
        this.productoService = productoService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    public String guardarNuevoPedido(CrearPedidoRequest request, String uid) throws Exception {
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(uid);
        pedido.setNombreCliente(request.getNombreCliente());
        pedido.setDireccion(request.getDireccion());
        pedido.setTelefono(request.getTelefono());
        pedido.setNotasGenerales(request.getNotasGenerales());
        pedido.setMetodoPago(request.getMetodoPago());
        pedido.setCupon(request.getCupon());

        List<LineaPedido> lineas = new ArrayList<>();
        double subtotalCalculado = 0;

        for (LineaPedidoRequest lineaReq : request.getProductos()) {
            Producto productoOriginal = productoService.obtenerPorId(lineaReq.getProductoId());
            if (productoOriginal == null) {
                throw new IllegalArgumentException("Producto no encontrado: " + lineaReq.getProductoId());
            }
            if (!productoOriginal.isDisponible()) {
                throw new IllegalArgumentException("El producto no está disponible: " + productoOriginal.getNombre());
            }

            LineaPedido linea = new LineaPedido();
            linea.setProductoId(lineaReq.getProductoId());
            linea.setNombre(productoOriginal.getNombre());
            linea.setCantidad(lineaReq.getCantidad());
            linea.setPrecioUnidad(productoOriginal.getPrecio());
            linea.setNotas(lineaReq.getNotas());
            lineas.add(linea);
            subtotalCalculado += productoOriginal.getPrecio() * lineaReq.getCantidad();
        }

        pedido.setProductos(lineas);

        double totalFinal = subtotalCalculado;
        if (request.getDescuento() != null && request.getDescuento() > 0) {
            double descuentoAplicado = Math.min(request.getDescuento(), subtotalCalculado);
            pedido.setDescuento(descuentoAplicado);
            totalFinal = subtotalCalculado - descuentoAplicado;
        }

        pedido.setTotal(totalFinal);
        pedido.setId(UUID.randomUUID().toString());
        pedido.setEstado("PENDIENTE");
        pedido.setFecha(System.currentTimeMillis());

        db.collection("pedidos").document(pedido.getId()).set(pedido).get();

        // Email de confirmación — no bloquea la respuesta
        try {
            var usuarioDoc = db.collection("usuarios").document(uid).get().get();
            if (usuarioDoc.exists()) {
                String email = usuarioDoc.getString("email");
                String nombre = usuarioDoc.getString("nombre");
                if (email != null && !email.isEmpty()) {
                    emailService.enviarConfirmacionPedido(email, nombre != null ? nombre : "Cliente", pedido);
                }
            }
        } catch (Exception ignored) {}

        return pedido.getId();
    }

    public List<Pedido> obtenerPedidosPorUsuario(String uid) throws Exception {
        // Sin orderBy para evitar requerir índice compuesto en Firestore.
        // La ordenación se hace en Java.
        return db.collection("pedidos")
                .whereEqualTo("idUsuario", uid)
                .get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .sorted((a, b) -> Long.compare(b.getFecha(), a.getFecha()))
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

    public List<Pedido> obtenerTodosPedidos(Long desde, Long hasta) throws Exception {
        var ref = db.collection("pedidos");
        com.google.cloud.firestore.Query query = ref;

        if (desde != null && hasta != null) {
            query = ref.whereGreaterThanOrEqualTo("fecha", desde)
                       .whereLessThanOrEqualTo("fecha", hasta);
        } else if (desde != null) {
            query = ref.whereGreaterThanOrEqualTo("fecha", desde);
        } else if (hasta != null) {
            query = ref.whereLessThanOrEqualTo("fecha", hasta);
        }

        return query.get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .sorted((a, b) -> Long.compare(b.getFecha(), a.getFecha()))
                .collect(Collectors.toList());
    }

    public String actualizarEstado(String idPedido, String nuevoEstado) throws Exception {
        List<String> estadosValidos = List.of("PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO", "CANCELADO");
        String estadoMayus = nuevoEstado.toUpperCase();

        if (!estadosValidos.contains(estadoMayus)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado + ". Opciones: " + estadosValidos);
        }

        db.collection("pedidos").document(idPedido).update("estado", estadoMayus).get();

        Pedido pedido = obtenerPorId(idPedido);
        if (pedido != null && pedido.getIdUsuario() != null) {
            var usuarioDoc = db.collection("usuarios").document(pedido.getIdUsuario()).get().get();
            if (usuarioDoc.exists()) {
                String fcmToken = usuarioDoc.getString("fcmToken");
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    notificacionService.enviarNotificacion(
                            fcmToken,
                            obtenerTituloNotificacion(estadoMayus),
                            obtenerCuerpoNotificacion(estadoMayus, idPedido)
                    );
                }
            }
        }

        return "Pedido " + idPedido + " actualizado a " + estadoMayus;
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
        long pedidosContados = 0;
        for (Pedido p : pedidosDeHoy) {
            if (!"CANCELADO".equals(p.getEstado())) {
                dineroTotal += p.getTotal();
                pedidosContados++;
            }
        }

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalDinero", dineroTotal);
        estadisticas.put("totalPedidos", pedidosContados);
        return estadisticas;
    }

    public List<Map<String, Object>> obtenerTopProductos(Long desde, Long hasta) throws Exception {
        var ref = db.collection("pedidos");
        com.google.cloud.firestore.Query query = ref;

        if (desde != null && hasta != null) {
            query = ref.whereGreaterThanOrEqualTo("fecha", desde)
                       .whereLessThanOrEqualTo("fecha", hasta);
        } else if (desde != null) {
            query = ref.whereGreaterThanOrEqualTo("fecha", desde);
        } else if (hasta != null) {
            query = ref.whereLessThanOrEqualTo("fecha", hasta);
        }

        List<Pedido> pedidos = query.get().get().getDocuments()
                .stream()
                .map(doc -> doc.toObject(Pedido.class))
                .filter(p -> !"CANCELADO".equals(p.getEstado()))
                .collect(Collectors.toList());

        Map<String, Long> conteo = new HashMap<>();
        for (Pedido p : pedidos) {
            if (p.getProductos() != null) {
                for (var linea : p.getProductos()) {
                    conteo.merge(linea.getNombre(), (long) linea.getCantidad(), Long::sum);
                }
            }
        }

        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("nombre", e.getKey());
                    item.put("unidades", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
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
        String idCorto = idPedido.length() > 8 ? idPedido.substring(0, 8).toUpperCase() : idPedido;
        return switch (estado) {
            case "COCINANDO" -> "El pedido #" + idCorto + " ya está siendo preparado.";
            case "REPARTO"   -> "El pedido #" + idCorto + " ha salido. Llegará pronto.";
            case "ENTREGADO" -> "El pedido #" + idCorto + " ha sido entregado. Buen provecho.";
            case "CANCELADO" -> "El pedido #" + idCorto + " ha sido cancelado.";
            default          -> "Tu pedido #" + idCorto + " ha cambiado de estado a " + estado;
        };
    }
}
