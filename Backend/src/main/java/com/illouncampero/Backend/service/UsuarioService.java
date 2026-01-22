package com.illouncampero.Backend.service;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    public String guardarPerfil(Usuario usuario) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        db.collection("usuarios").document(usuario.getUid()).set(usuario);
        return "Perfil actualizado";
    }

    public Usuario obtenerPorUid(String uid) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot doc = db.collection("usuarios").document(uid).get().get();
        if (doc.exists()) {
            return doc.toObject(Usuario.class);
        }
        return null;
    }
}