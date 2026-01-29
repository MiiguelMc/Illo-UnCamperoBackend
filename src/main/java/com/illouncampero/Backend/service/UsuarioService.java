package com.illouncampero.Backend.service;

import com.google.cloud.firestore.*;
import com.illouncampero.Backend.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final Firestore db; // 1. Declaramos la variable de clase

    // 2. Inyectamos la conexión por el constructor
    public UsuarioService(Firestore db) {
        this.db = db;
    }

    public String guardarPerfil(Usuario usuario) throws Exception {
        // 3. Usamos la 'db' de la clase (eliminamos la llamada a FirestoreClient)
        db.collection("usuarios").document(usuario.getUid()).set(usuario);
        return "Perfil actualizado";
    }

    public Usuario obtenerPorUid(String uid) throws Exception {
        // 4. Usamos la 'db' de la clase
        DocumentSnapshot doc = db.collection("usuarios").document(uid).get().get();
        if (doc.exists()) {
            return doc.toObject(Usuario.class);
        }
        return null;
    }
}