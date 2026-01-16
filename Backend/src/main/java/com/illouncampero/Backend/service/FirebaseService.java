package com.illouncampero.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.model.Usuarios;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FirebaseService {

    // LISTAR PRODUCTOS
    public List<Producto> getProductos() throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        return db.collection("productos").get().get().getDocuments()
                .stream()
                .map(doc -> {
                    Producto p = doc.toObject(Producto.class);
                    p.setId(doc.getId());
                    return p;
                }).collect(Collectors.toList());
    }

    // GUARDAR O ACTUALIZAR PRODUCTO
    public String saveProducto(Producto producto) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        if (producto.getId() == null || producto.getId().isEmpty()) {
            producto.setId(UUID.randomUUID().toString());
        }
        ApiFuture<WriteResult> future = db.collection("productos").document(producto.getId()).set(producto);
        return "Producto guardado a las: " + future.get().getUpdateTime();
    }

    // BORRAR PRODUCTO
    public String deleteProducto(String id) {
        Firestore db = FirestoreClient.getFirestore();
        db.collection("productos").document(id).delete();
        return "Producto " + id + " eliminado con éxito";
    }
    public String saveUsuario(Usuarios usuario) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        // Usamos el UID de Firebase como ID del documento
        ApiFuture<WriteResult> future = db.collection("usuarios")
                .document(usuario.getUid())
                .set(usuario);
        return "Perfil de usuario creado/actualizado";
    }

    // Obtener perfil (Para saber si es ADMIN o CLIENTE)
    public Usuarios getUsuario(String uid) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot doc = db.collection("usuarios").document(uid).get().get();
        if (doc.exists()) {
            return doc.toObject(Usuarios.class);
        }
        return null;
    }
}