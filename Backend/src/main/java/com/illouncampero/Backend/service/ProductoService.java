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
        db.collection("productos").document(producto.getId()).set(producto).get();
        return "Producto guardado con éxito";
    }


    public String eliminarProducto(String id) throws Exception {
        String idLimpio = id.trim(); // Limpiamos el ID de posibles espacios


        System.out.println("LOG SERVICE: Intentando borrar ID limpio: '" + idLimpio + "'");


        // Verificamos si el documento existe antes de intentar borrarlo (opcional, para mejor logging)
        DocumentSnapshot doc = db.collection("productos").document(idLimpio).get().get();
        if (doc.exists()) {
            // El .get() es VITAL aquí para que Spring Boot espere a que Firebase lo borre de verdad
            db.collection("productos").document(idLimpio).delete().get();
            System.out.println("LOG SERVICE: Producto con ID '" + idLimpio + "' BORRADO de Firebase.");
            return "Producto eliminado con éxito";
        } else {
            System.out.println("LOG SERVICE: No se encontró producto con ID '" + idLimpio + "' para borrar.");
            return "Error: El producto no existe o ya fue eliminado.";
        }
    }
}
