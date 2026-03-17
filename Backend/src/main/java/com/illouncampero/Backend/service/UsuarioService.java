package com.illouncampero.Backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.illouncampero.Backend.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final Firestore db;

    public UsuarioService(Firestore db) {
        this.db = db;
    }

    public String guardarPerfil(Usuario usuario) throws Exception {
        if (usuario.getUid() == null || usuario.getUid().isEmpty()) {
            throw new Exception("El UID del usuario no puede estar vacío");
        }

        db.collection("usuarios")
                .document(usuario.getUid())
                .set(usuario, SetOptions.merge())
                .get();

        return "Perfil actualizado con éxito";
    }

    public Usuario obtenerPorUid(String uid) throws Exception {
        DocumentSnapshot doc = db.collection("usuarios").document(uid).get().get();
        if (doc.exists()) {
            Usuario u = doc.toObject(Usuario.class);
            if (u != null) u.setUid(doc.getId());
            return u;
        }
        return null;
    }
}
