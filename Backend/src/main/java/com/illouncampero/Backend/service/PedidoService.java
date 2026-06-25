package com.illouncampero.Backend.service;

import com.illouncampero.Backend.model.LineaPedido;
import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.model.PushSubscription;
import com.illouncampero.Backend.model.dto.CrearPedidoRequest;
import com.illouncampero.Backend.model.dto.LineaPedidoRequest;
import com.illouncampero.Backend.repository.PedidoRepository;
import com.illouncampero.Backend.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoService productoService;
    private final NotificacionService notificacionService;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoService productoService,
                         NotificacionService notificacionService,
                         PushSubscriptionRepository pushSubscriptionRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoService = productoService;
        this.notificacionService = notificacionService;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    @Transactional
    public String guardarNuevoPedido(CrearPedidoRequest request, String uid) {
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
        pedido.setEstado("TARJETA".equals(request.getMetodoPago()) ? "PENDIENTE_PAGO" : "PENDIENTE");
        pedido.setFecha(System.currentTimeMillis());

        pedidoRepository.save(pedido);

        return pedido.getId();
    }

    public List<Pedido> obtenerPedidosPorUsuario(String uid) {
        return pedidoRepository.findByIdUsuario(uid)
                .stream()
                .sorted((a, b) -> Long.compare(b.getFecha(), a.getFecha()))
                .collect(Collectors.toList());
    }

    public List<Pedido> obtenerPedidosActivos() {
        return pedidoRepository.findByEstadoIn(
                List.of("PENDIENTE_PAGO", "PENDIENTE", "COCINANDO", "REPARTO"));
    }

    public List<Pedido> obtenerTodosPedidos(Long desde, Long hasta) {
        List<Pedido> pedidos;
        if (desde != null && hasta != null) {
            pedidos = pedidoRepository.findByFechaGreaterThanEqualAndFechaLessThanEqual(desde, hasta);
        } else if (desde != null) {
            pedidos = pedidoRepository.findByFechaGreaterThanEqual(desde);
        } else if (hasta != null) {
            pedidos = pedidoRepository.findByFechaLessThanEqual(hasta);
        } else {
            pedidos = pedidoRepository.findAll();
        }
        return pedidos.stream()
                .sorted((a, b) -> Long.compare(b.getFecha(), a.getFecha()))
                .collect(Collectors.toList());
    }

    @Transactional
    public String actualizarEstado(String idPedido, String nuevoEstado) {
        List<String> estadosValidos = List.of("PENDIENTE_PAGO", "PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO", "CANCELADO");
        String estadoMayus = nuevoEstado.toUpperCase();

        if (!estadosValidos.contains(estadoMayus)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado + ". Opciones: " + estadosValidos);
        }

        Pedido pedido = obtenerPorId(idPedido);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido no encontrado: " + idPedido);
        }

        String estadoActual = pedido.getEstado() != null ? pedido.getEstado().toUpperCase() : "";
        if ("CANCELADO".equals(estadoMayus)
                && !"PENDIENTE".equals(estadoActual)
                && !"PENDIENTE_PAGO".equals(estadoActual)) {
            throw new IllegalStateException(
                    "No se puede cancelar un pedido que ya esta en preparacion, reparto, entregado o cancelado."
            );
        }

        if (estadoMayus.equals(estadoActual)) {
            return "Pedido " + idPedido + " ya esta en " + estadoMayus;
        }

        pedido.setEstado(estadoMayus);
        pedidoRepository.save(pedido);

        if (pedido.getIdUsuario() != null) {
            List<PushSubscription> subs = pushSubscriptionRepository.findByUsuarioId(pedido.getIdUsuario());
            for (PushSubscription sub : subs) {
                notificacionService.enviarNotificacion(
                        sub,
                        obtenerTituloNotificacion(estadoMayus),
                        obtenerCuerpoNotificacion(estadoMayus, idPedido)
                );
            }
        }

        return "Pedido " + idPedido + " actualizado a " + estadoMayus;
    }

    public Pedido obtenerPorId(String id) {
        return pedidoRepository.findById(id).orElse(null);
    }

    public Map<String, Object> obtenerVentasHoy() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long comienzoDelDia = cal.getTimeInMillis();

        List<Pedido> pedidosDeHoy = pedidoRepository.findByFechaGreaterThanEqual(comienzoDelDia);

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

    public List<Map<String, Object>> obtenerTopProductos(Long desde, Long hasta) {
        List<Pedido> pedidos = obtenerTodosPedidos(desde, hasta).stream()
                .filter(p -> !"CANCELADO".equals(p.getEstado()))
                .collect(Collectors.toList());

        Map<String, Long> conteo = new HashMap<>();
        for (Pedido p : pedidos) {
            if (p.getProductos() != null) {
                for (LineaPedido linea : p.getProductos()) {
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
