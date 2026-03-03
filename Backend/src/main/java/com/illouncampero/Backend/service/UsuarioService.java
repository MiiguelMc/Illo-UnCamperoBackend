package com.illouncampero.Backend.service;

import com.google.cloud.firestore.*;
import com.illouncampero.Backend.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Service
public class UsuarioService {

    private final Firestore db;

    public UsuarioService(Firestore db) {
        this.db = db;
    }

    public String guardarPerfil(Usuario usuario) throws Exception {
        // 1. Verificamos que el UID no sea nulo antes de tocar la BD
        if (usuario.getUid() == null || usuario.getUid().isEmpty()) {
            throw new Exception("El UID del usuario no puede estar vacío");
        }

        System.out.println("LOG: Intentando guardar en Firebase el perfil del UID: " + usuario.getUid());

        // 2. IMPORTANTE: Añadimos .get() al final para esperar a Firebase.
        // 3. RECOMENDADO: Usamos SetOptions.merge() para que si el móvil no envía algún campo
        // (como el rol o la contraseña), Firebase NO los borre de la base de datos.
        db.collection("usuarios")
                .document(usuario.getUid())
                .set(usuario, SetOptions.merge()) // El merge protege tus datos existentes
                .get(); // <--- LA LLAVE MAESTRA QUE TE FALTABA

        return "Perfil actualizado con éxito";
    }

    public Usuario obtenerPorUid(String uid) throws Exception {
        // El .get().get() aquí ya lo tenías bien puesto
        DocumentSnapshot doc = db.collection("usuarios").document(uid).get().get();
        if (doc.exists()) {
            Usuario u = doc.toObject(Usuario.class);
            // Nos aseguramos de que el objeto lleva su UID
            if (u != null) u.setUid(doc.getId());
            return u;
        }
        return null;
    }

}