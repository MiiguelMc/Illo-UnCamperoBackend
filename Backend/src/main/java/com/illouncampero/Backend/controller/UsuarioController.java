package com.illouncampero.Backend.controller;

import com.google.cloud.firestore.Firestore;
import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final Firestore db;

    public UsuarioController(UsuarioService usuarioService, Firestore db) {
        this.usuarioService = usuarioService;
        this.db = db;
    }

    @PostMapping("/registro")
    public String registrarPerfil(@RequestBody Usuario usuario) throws Exception {
        usuario.setRol("CLIENTE");
        return usuarioService.guardarPerfil(usuario);
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Usuario usuario, Authentication authentication) throws Exception {
        String uidAutenticado = authentication.getName();
        usuario.setUid(uidAutenticado);
        usuarioService.guardarPerfil(usuario);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/{uid}")
    public Usuario obtenerPerfil(@PathVariable String uid) throws Exception {
        Usuario user = usuarioService.obtenerPorUid(uid);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return user;
    }

    @PatchMapping("/{uid}/fcm-token")
    public ResponseEntity<Void> actualizarFcmToken(
            @PathVariable String uid,
            @RequestBody Map<String, String> body) throws Exception {

        String token = body.get("fcmToken");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        db.collection("usuarios").document(uid).update("fcmToken", token).get();
        return ResponseEntity.ok().build();
    }
}
