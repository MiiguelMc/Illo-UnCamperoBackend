package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.service.PedidoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/realizar-pedido")
    public String realizarPedido(@RequestBody Pedido pedido) throws Exception {
        return pedidoService.guardarNuevoPedido(pedido);
    }

    @GetMapping("/activos")
    public List<Pedido> listarActivos() throws Exception {
        return pedidoService.obtenerPedidosActivos();
    }

    @GetMapping("/usuario/{uid}")
    public List<Pedido> listarPorUsuario(@PathVariable String uid) throws Exception {
        return pedidoService.obtenerPedidosPorUsuario(uid);
    }

    @PatchMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam String nuevoEstado) throws Exception {
        return pedidoService.actualizarEstado(id, nuevoEstado);
    }

    @GetMapping("/{id}")
    public Pedido obtenerDetalles(@PathVariable String id) throws Exception {
        return pedidoService.obtenerPorId(id);
    }

    @GetMapping("/estadisticas/hoy")
    public java.util.Map<String, Object> verVentasHoy() throws Exception {
        return pedidoService.obtenerVentasHoy();
    }
}
