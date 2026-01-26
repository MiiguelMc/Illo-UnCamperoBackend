package com.illouncampero.Backend.service;

import com.google.cloud.firestore.*;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final Firestore db; // Esta es la única que necesitamos

    // Spring inyecta la conexión aquí una sola vez al arrancar
    public ProductoService(Firestore db) {
        this.db = db;
    }

    public Producto obtenerPorId(String id) throws Exception {
        // ELIMINADA la línea de FirestoreClient.getFirestore()
        DocumentSnapshot doc = db.collection("productos").document(id).get().get();
        if (doc.exists()) {
            Producto p = doc.toObject(Producto.class);
            p.setId(doc.getId());
            return p;
        }
        return null;
    }

    public List<Producto> obtenerTodos() throws Exception {
        return db.collection("productos").get().get().getDocuments()
                .stream()
                .map(doc -> {
                    Producto p = doc.toObject(Producto.class);
                    p.setId(doc.getId());
                    return p;
                }).collect(Collectors.toList());
    }

    public String guardarProducto(Producto producto) throws Exception {
        // ELIMINADA la línea de FirestoreClient.getFirestore()
        if (producto.getId() == null || producto.getId().isEmpty()) {
            producto.setId(UUID.randomUUID().toString());
        }
        db.collection("productos").document(producto.getId()).set(producto);
        return "Producto guardado con éxito";
    }

    public String eliminarProducto(String id) {
        // Usamos directamente this.db
        db.collection("productos").document(id).delete();
        return "Producto eliminado";
    }
}