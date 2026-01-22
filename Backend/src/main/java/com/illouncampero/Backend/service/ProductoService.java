package com.illouncampero.Backend.service;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    public List<Producto> obtenerTodos() throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        return db.collection("productos").get().get().getDocuments()
                .stream()
                .map(doc -> {
                    Producto p = doc.toObject(Producto.class);
                    p.setId(doc.getId());
                    return p;
                }).collect(Collectors.toList());
    }

    public String guardarProducto(Producto producto) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        if (producto.getId() == null || producto.getId().isEmpty()) {
            producto.setId(UUID.randomUUID().toString());
        }
        db.collection("productos").document(producto.getId()).set(producto);
        return "Producto guardado con éxito";
    }

    public String eliminarProducto(String id) {
        Firestore db = FirestoreClient.getFirestore();
        db.collection("productos").document(id).delete();
        return "Producto eliminado";
    }
}