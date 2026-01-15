package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.Producto;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FirebaseService {

    public List<Producto> getProductos() throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        return db.collection("productos").get().get().getDocuments()
                .stream()
                .map(doc -> {
                    Producto p = doc.toObject(Producto.class);
                    p.setId(doc.getId());
                    return p;
                })
                .collect(Collectors.toList());
    }
}