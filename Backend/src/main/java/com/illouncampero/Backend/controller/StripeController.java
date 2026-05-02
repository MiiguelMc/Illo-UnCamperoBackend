package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.service.PedidoService;
import com.illouncampero.Backend.service.StripeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class StripeController {

    private final StripeService stripeService;
    private final PedidoService pedidoService;

    public StripeController(StripeService stripeService, PedidoService pedidoService) {
        this.stripeService = stripeService;
        this.pedidoService = pedidoService;
    }

    @PostMapping("/crear-intent")
    public ResponseEntity<Map<String, String>> crearIntent(
            @RequestBody Map<String, Object> body,
            Authentication authentication) throws Exception {

        String pedidoId = (String) body.get("pedidoId");
        if (pedidoId == null || pedidoId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "pedidoId requerido"));
        }

        Pedido pedido = pedidoService.obtenerPorId(pedidoId);
        if (pedido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Pedido no encontrado"));
        }

        // Verificar que el pedido pertenece al usuario autenticado
        String uidAutenticado = authentication.getName();
        if (!uidAutenticado.equals(pedido.getIdUsuario())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Acceso denegado"));
        }

        // Total siempre desde la BD, nunca del cliente
        String clientSecret = stripeService.crearIntentPago(pedido.getTotal());
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }
}
