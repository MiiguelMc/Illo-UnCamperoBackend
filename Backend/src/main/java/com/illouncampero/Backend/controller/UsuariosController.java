package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Usuarios;
import com.illouncampero.Backend.service.FirebaseService;
import org.springframework.web.bind.annotation.*;

public class UsuariosController {
    @RestController
    @RequestMapping("/api/usuarios")
    @CrossOrigin(origins = "*")
    public class UsuarioController {

        private final FirebaseService firebaseService;

        public UsuarioController(FirebaseService firebaseService) {
            this.firebaseService = firebaseService;
        }

        @PostMapping("/registro")
        public String registrarPerfil(@RequestBody Usuarios usuario) throws Exception {
            // Por defecto, todos los que se registran son CLIENTES
            usuario.setRol("CLIENTE");
            return firebaseService.saveUsuario(usuario);
        }

        @GetMapping("/{uid}")
        public Usuarios obtenerPerfil(@PathVariable String uid) throws Exception {
            return firebaseService.getUsuario(uid);
        }
    }
}
