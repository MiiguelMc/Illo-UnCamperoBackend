package com.illouncampero.Backend.service;

import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SupabaseAdminService supabaseAdminService;

    @Test
    void guardarPerfilSinUidLanzaExcepcion() {
        UsuarioService service = new UsuarioService(usuarioRepository, supabaseAdminService);

        Usuario usuario = new Usuario();
        usuario.setUid(null);
        usuario.setNombre("Test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarPerfil(usuario));
        assertTrue(ex.getMessage().contains("UID"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void guardarPerfilNuevoAsignaRolClientePorDefecto() {
        when(usuarioRepository.findById("uid-1")).thenReturn(java.util.Optional.empty());
        UsuarioService service = new UsuarioService(usuarioRepository, supabaseAdminService);

        Usuario usuario = new Usuario();
        usuario.setUid("uid-1");
        usuario.setNombre("Test");
        usuario.setEmail("test@test.com");

        service.guardarPerfil(usuario);

        assertEquals("CLIENTE", usuario.getRol());
        verify(usuarioRepository).save(usuario);
    }
}
