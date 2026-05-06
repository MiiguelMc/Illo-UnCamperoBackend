package com.illouncampero.Backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.illouncampero.Backend.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final Firestore db;
    private final EmailService emailService;

    public UsuarioService(Firestore db, EmailService emailService) {
        this.db = db;
        this.emailService = emailService;
    }

    public String guardarPerfil(Usuario usuario) throws Exception {
        if (usuario.getUid() == null || usuario.getUid().isEmpty()) {
            throw new IllegalArgumentException("El UID del usuario no puede estar vacío");
        }

        db.collection("usuarios")
                .document(usuario.getUid())
                .set(usuario, SetOptions.merge())
                .get();

        return "Perfil actualizado con éxito";
    }

    public String registrarPerfil(Usuario usuario) throws Exception {
        String resultado = guardarPerfil(usuario);

        if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
            String nombre = usuario.getNombre() != null ? usuario.getNombre() : "Cliente";
            emailService.enviarBienvenida(usuario.getEmail(), nombre);
        }

        return resultado;
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

    public void eliminarCuenta(String uid) throws Exception {
        FirebaseAuth.getInstance().deleteUser(uid);
        db.collection("usuarios").document(uid).delete().get();
    }
}
