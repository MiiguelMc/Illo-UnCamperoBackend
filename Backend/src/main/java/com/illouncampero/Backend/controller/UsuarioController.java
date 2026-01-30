package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.service.UsuarioService; // Usamos el servicio específico
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // 1. Cambiamos la ruta a "/perfil" para que coincida con el móvil
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Usuario usuario, Authentication authentication) throws Exception {

        // 2. Sacamos el UID directamente del Token (esto es lo más seguro)
        // Dependiendo de tu filtro, puede ser authentication.getName() o el Principal
        String uidAutenticado = authentication.getName();

        System.out.println("LOG: El usuario " + uidAutenticado + " quiere actualizar su perfil.");

        // 3. Le asignamos al objeto usuario el UID que viene del token
        // Así nos aseguramos de que actualiza SU documento y no el de otro
        usuario.setUid(uidAutenticado);

        // 4. Guardamos (Asegúrate de que guardarPerfil tenga el .get() dentro del Service)
        usuarioService.guardarPerfil(usuario);

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