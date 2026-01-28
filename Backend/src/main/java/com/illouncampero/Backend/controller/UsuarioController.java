package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.service.UsuarioService; // Usamos el servicio específico
import org.springframework.http.HttpStatus;
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
    public String actualizarPerfil(@RequestBody Usuario usuario) throws Exception {
        // Obtenemos el UID del token de quien hace la llamada
        String uidAutenticado = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Validamos: ¿El que intenta editar es el dueño de la cuenta?
        if (!uidAutenticado.equals(usuario.getUid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este perfil");
        }

        // Si es el dueño, guardamos los cambios (nombre, dirección, teléfono, etc.)
        return usuarioService.guardarPerfil(usuario);
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