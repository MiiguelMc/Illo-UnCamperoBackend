package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.service.PedidoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*") // Para que no bloquee a tus compañeros
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/realizar-pedido")
    public String realizarPedido(@RequestBody Pedido pedido) throws Exception {
        // Llamamos al servicio para que haga su magia
        return pedidoService.guardarNuevoPedido(pedido);
    }
}