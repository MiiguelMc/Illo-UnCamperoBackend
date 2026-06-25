package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public String registrarPerfil(@Valid @RequestBody Usuario usuario) {
        usuario.setRol("CLIENTE");
        return usuarioService.registrarPerfil(usuario);
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@Valid @RequestBody Usuario usuario,
                                              Authentication authentication) {
        usuario.setUid(authentication.getName());
        usuarioService.guardarPerfil(usuario);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/{uid}")
    public Usuario obtenerPerfil(@PathVariable String uid, Authentication authentication) {
        if (!uid.equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }
        Usuario user = usuarioService.obtenerPorUid(uid);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return user;
    }

    @DeleteMapping("/cuenta")
    public ResponseEntity<Void> eliminarCuenta(Authentication authentication) throws Exception {
        usuarioService.eliminarCuenta(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
