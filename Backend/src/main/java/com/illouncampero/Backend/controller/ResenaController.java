package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.Resena;
import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.repository.PedidoRepository;
import com.illouncampero.Backend.repository.ResenaRepository;
import com.illouncampero.Backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final ResenaRepository resenaRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public ResenaController(ResenaRepository resenaRepository,
                            PedidoRepository pedidoRepository,
                            UsuarioRepository usuarioRepository) {
        this.resenaRepository = resenaRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> crearResena(@RequestBody Resena resena, Authentication auth) {
        String uid = auth.getName();
        resena.setIdUsuario(uid);
        resena.setFecha(System.currentTimeMillis());

        if (resena.getPuntuacion() < 1 || resena.getPuntuacion() > 5) {
            return ResponseEntity.badRequest().body("La puntuación debe ser entre 1 y 5.");
        }
        if (resena.getIdPedido() == null || resena.getIdPedido().isBlank()) {
            return ResponseEntity.badRequest().body("El ID del pedido es obligatorio.");
        }

        Pedido pedido = pedidoRepository.findById(resena.getIdPedido()).orElse(null);
        if (pedido == null) {
            return ResponseEntity.badRequest().body("El pedido no existe.");
        }

        if (!"ENTREGADO".equals(pedido.getEstado())) {
            return ResponseEntity.badRequest().body("Solo puedes valorar pedidos entregados.");
        }

        if (!uid.equals(pedido.getIdUsuario())) {
            return ResponseEntity.status(403).body("No puedes valorar un pedido que no es tuyo.");
        }

        if (pedido.isValorado()) {
            return ResponseEntity.badRequest().body("Este pedido ya ha sido valorado.");
        }

        resena.setId(UUID.randomUUID().toString());
        if (resena.getComentario() == null) {
            resena.setComentario("");
        }
        resenaRepository.save(resena);

        pedido.setValorado(true);
        pedidoRepository.save(pedido);

        return ResponseEntity.ok("Reseña guardada. Gracias por tu opinión.");
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarResenas() {
        List<Resena> resenas = resenaRepository.findTop50ByOrderByFechaDesc();

        List<Map<String, Object>> lista = new ArrayList<>();
        for (Resena r : resenas) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("idPedido", r.getIdPedido());
            m.put("idUsuario", r.getIdUsuario());
            m.put("puntuacion", r.getPuntuacion());
            m.put("comentario", r.getComentario());
            m.put("fecha", r.getFecha());

            String nombreUsuario = "Cliente";
            if (r.getIdUsuario() != null) {
                Usuario u = usuarioRepository.findById(r.getIdUsuario()).orElse(null);
                if (u != null && u.getNombre() != null) {
                    nombreUsuario = u.getNombre();
                }
            }
            m.put("nombreUsuario", nombreUsuario);
            lista.add(m);
        }
        return ResponseEntity.ok(lista);
    }
}
