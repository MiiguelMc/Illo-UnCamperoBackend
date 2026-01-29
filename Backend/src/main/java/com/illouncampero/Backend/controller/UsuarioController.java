package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.service.UsuarioService; // Usamos el servicio específico
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Inyectamos UsuarioService (Recuerda que FirebaseService ya no deberías usarlo aquí)
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 1. REGISTRO (Crea el perfil inicial como CLIENTE)
    @PostMapping("/registro")
    public String registrarPerfil(@RequestBody Usuario usuario) throws Exception {
        usuario.setRol("CLIENTE");
        return usuarioService.guardarPerfil(usuario);
    }

    // 2. ACTUALIZAR PERFIL (Lo que necesita tu compañero)
    @PutMapping("/actualizar")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Usuario usuario) throws Exception {
        // 1. Obtenemos el UID del token
        String uidAutenticado = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Validamos el permiso
        if (!uidAutenticado.equals(usuario.getUid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"No tienes permiso para modificar este perfil\"}");
        }

        // 3. Guardamos los cambios
        // Lo ideal es que guardarPerfil devuelva el objeto Usuario actualizado
        usuarioService.guardarPerfil(usuario);

        // 4. IMPORTANTE: Devolvemos el objeto usuario (JSON) y no un String suelto
        return ResponseEntity.ok(usuario);
    }
    // 3. OBTENER PERFIL
    @GetMapping("/{uid}")
    public Usuario obtenerPerfil(@PathVariable String uid) throws Exception {
        Usuario user = usuarioService.obtenerPorUid(uid);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return user;
    }
}