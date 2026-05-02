package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/realizar-pedido")
    public String realizarPedido(@RequestBody Pedido pedido, Authentication authentication) throws Exception {
        // El idUsuario siempre se toma del token, nunca del body
        pedido.setIdUsuario(authentication.getName());
        return pedidoService.guardarNuevoPedido(pedido);
    }

    @GetMapping("/activos")
    public List<Pedido> listarActivos() throws Exception {
        return pedidoService.obtenerPedidosActivos();
    }

    // El uid se resuelve desde el token, no desde la URL — Fix #4
    @GetMapping("/mis-pedidos")
    public List<Pedido> listarMisPedidos(Authentication authentication) throws Exception {
        return pedidoService.obtenerPedidosPorUsuario(authentication.getName());
    }

    @PatchMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam String nuevoEstado) throws Exception {
        return pedidoService.actualizarEstado(id, nuevoEstado);
    }

    // Fix #5: verifica que el pedido pertenece al usuario autenticado (salvo admin/cocina)
    @GetMapping("/{id}")
    public Pedido obtenerDetalles(@PathVariable String id, Authentication authentication) throws Exception {
        Pedido pedido = pedidoService.obtenerPorId(id);
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
        }

        boolean esAdminOCocina = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_COCINA"));

        if (!esAdminOCocina && !authentication.getName().equals(pedido.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }

        return pedido;
    }

    @GetMapping("/estadisticas/hoy")
    public Map<String, Object> verVentasHoy() throws Exception {
        return pedidoService.obtenerVentasHoy();
    }
}
