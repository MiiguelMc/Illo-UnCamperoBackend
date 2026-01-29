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
        // Validamos que el estado sea uno de los permitidos
        List<String> estadosValidos = List.of("PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO", "CANCELADO");
        if (!estadosValidos.contains(nuevoEstado.toUpperCase())) {
            throw new Exception("Estado no válido");
        }

        db.collection("pedidos").document(idPedido).update("estado", nuevoEstado.toUpperCase());
        return "Estado del pedido " + idPedido + " actualizado a " + nuevoEstado;
    }

    // 5. OBTENER UN PEDIDO POR ID (Para ver detalles específicos)
    public Pedido obtenerPorId(String id) throws Exception {
        return db.collection("pedidos").document(id).get().get().toObject(Pedido.class);
    }
}