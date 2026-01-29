package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.service.PedidoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // 1. REALIZAR PEDIDO
    // Se usa desde: App Móvil
    @PostMapping("/realizar-pedido")
    public String realizarPedido(@RequestBody Pedido pedido) throws Exception {
        return pedidoService.guardarNuevoPedido(pedido);
    }

    // 2. VER PEDIDOS ACTIVOS (Pendientes, Cocinando, Reparto)
    // Se usa desde: Web Angular (Panel del restaurante)
    @GetMapping("/activos")
    public List<Pedido> listarActivos() throws Exception {
        return pedidoService.obtenerPedidosActivos();
    }

    // 3. VER HISTORIAL DE UN USUARIO
    // Se usa desde: App Móvil (Pantalla "Mis Pedidos")
    @GetMapping("/usuario/{uid}")
    public List<Pedido> listarPorUsuario(@PathVariable String uid) throws Exception {
        return pedidoService.obtenerPedidosPorUsuario(uid);
    }

    // 4. CAMBIAR ESTADO DEL PEDIDO
    // Se usa desde: Web Angular (Botones para avanzar el pedido)
    // Ejemplo: PATCH /api/pedidos/ID/estado?nuevoEstado=COCINANDO
    @PatchMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam String nuevoEstado) throws Exception {
        return pedidoService.actualizarEstado(id, nuevoEstado);
    }

    // 5. VER DETALLES DE UN PEDIDO
    @GetMapping("/{id}")
    public Pedido obtenerDetalles(@PathVariable String id) throws Exception {
        return pedidoService.obtenerPorId(id);
    }
}