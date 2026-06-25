package com.illouncampero.Backend.service;

import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SupabaseAdminService supabaseAdminService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          SupabaseAdminService supabaseAdminService) {
        this.usuarioRepository = usuarioRepository;
        this.supabaseAdminService = supabaseAdminService;
    }

    public String guardarPerfil(Usuario usuario) {
        if (usuario.getUid() == null || usuario.getUid().isEmpty()) {
            throw new IllegalArgumentException("El UID del usuario no puede estar vacío");
        }

        // Merge: conserva el rol existente si la peticion no lo trae.
        Usuario existente = usuarioRepository.findById(usuario.getUid()).orElse(null);
        if (existente != null && (usuario.getRol() == null || usuario.getRol().isEmpty())) {
            usuario.setRol(existente.getRol());
        }
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("CLIENTE");
        }

        usuarioRepository.save(usuario);
        return "Perfil actualizado con éxito";
    }

    public String registrarPerfil(Usuario usuario) {
        return guardarPerfil(usuario);
    }

    public Usuario obtenerPorUid(String uid) {
        return usuarioRepository.findById(uid).orElse(null);
    }

    public void eliminarCuenta(String uid) throws Exception {
        supabaseAdminService.deleteUser(uid);
        usuarioRepository.deleteById(uid);
    }
}
