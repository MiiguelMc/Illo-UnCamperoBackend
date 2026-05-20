package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.model.dto.CrearPedidoRequest;
import com.illouncampero.Backend.service.PedidoService;
import jakarta.validation.Valid;
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
    public Map<String, String> realizarPedido(@Valid @RequestBody CrearPedidoRequest request,
                                              Authentication authentication) throws Exception {
        String id = pedidoService.guardarNuevoPedido(request, authentication.getName());
        return Map.of("id", id);
    }

    @GetMapping("/activos")
    public List<Pedido> listarActivos() throws Exception {
        return pedidoService.obtenerPedidosActivos();
    }

    @GetMapping("/todos")
    public List<Pedido> listarTodos(
            @RequestParam(required = false) Long desde,
            @RequestParam(required = false) Long hasta) throws Exception {
        return pedidoService.obtenerTodosPedidos(desde, hasta);
    }

    @GetMapping("/mis-pedidos")
    public List<Pedido> listarMisPedidos(Authentication authentication) throws Exception {
        return pedidoService.obtenerPedidosPorUsuario(authentication.getName());
    }

    @PatchMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable String id,
                                @RequestParam String nuevoEstado) throws Exception {
        return pedidoService.actualizarEstado(id, nuevoEstado);
    }

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

    @GetMapping("/estadisticas/resumen")
    public Map<String, Object> verVentas(
            @RequestParam(required = false) Long desde,
            @RequestParam(required = false) Long hasta) throws Exception {
        return pedidoService.obtenerVentas(desde, hasta);
    }

    @GetMapping("/estadisticas/productos")
    public List<Map<String, Object>> topProductos(
            @RequestParam(required = false) Long desde,
            @RequestParam(required = false) Long hasta) throws Exception {
        return pedidoService.obtenerTopProductos(desde, hasta);
    }
}
