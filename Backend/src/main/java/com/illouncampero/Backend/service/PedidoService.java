package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.LineaPedido;
import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PedidoService {
    private final Firestore db; // 1. Declaras la variable arriba
    private final ProductoService productoService;

    public PedidoService(Firestore db, ProductoService productoService) {
        this.db = db;

        this.productoService = productoService;
    }

    public String guardarNuevoPedido(Pedido pedido) throws Exception {

        double totalCalculado = 0;

        // 1. Validamos cada línea del pedido
        if (pedido.getProductos() == null || pedido.getProductos().isEmpty()) {
            throw new Exception("El pedido no tiene productos");
        }

        for (LineaPedido linea : pedido.getProductos()) {
            // Buscamos el producto en la DB para tener el precio real y actualizado
            Producto productoOriginal = productoService.obtenerPorId(linea.getProductoId());

            if (productoOriginal != null) {
                // Machacamos el nombre y el precio con lo que dice la DB (Seguridad)
                linea.setNombre(productoOriginal.getNombre());
                linea.setPrecioUnidad(productoOriginal.getPrecio());

                // Sumamos al total: precio_db * cantidad_cliente
                totalCalculado += productoOriginal.getPrecio() * linea.getCantidad();
            } else {
                throw new Exception("Producto no encontrado: " + linea.getProductoId());
            }
        }

        // 2. Asignamos el total real calculado en el servidor
        pedido.setTotal(totalCalculado);

        // 3. Forzamos datos de seguridad
        pedido.setId(UUID.randomUUID().toString());
        pedido.setEstado("PENDIENTE");
        pedido.setFecha(System.currentTimeMillis());

        // 4. Guardamos en Firestore
        db.collection("pedidos").document(pedido.getId()).set(pedido);

        return pedido.getId();
    }
}