package com.illouncampero.Backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final Firestore db;

    public ProductoService(Firestore db) {
        this.db = db;
    }

    public Producto obtenerPorId(String id) throws Exception {
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
        if (producto.getId() == null || producto.getId().isEmpty()) {
            producto.setId(UUID.randomUUID().toString());
        }
        db.collection("productos").document(producto.getId()).set(producto).get();
        return "Producto guardado con éxito";
    }

    public String eliminarProducto(String id) throws Exception {
        String idLimpio = id.trim();
        DocumentSnapshot doc = db.collection("productos").document(idLimpio).get().get();
        if (doc.exists()) {
            db.collection("productos").document(idLimpio).delete().get();
            return "Producto eliminado con éxito";
        }
        return "El producto no existe o ya fue eliminado.";
    }
}
