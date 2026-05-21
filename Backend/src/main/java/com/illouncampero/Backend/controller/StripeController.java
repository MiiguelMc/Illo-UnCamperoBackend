package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Pedido;
import com.illouncampero.Backend.service.PedidoService;
import com.illouncampero.Backend.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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
            Authentication authentication) {

        String pedidoId = (String) body.get("pedidoId");
        if (pedidoId == null || pedidoId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "pedidoId requerido"));
        }

        Pedido pedido = pedidoService.obtenerPorId(pedidoId);
        if (pedido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Pedido no encontrado"));
        }

        String uidAutenticado = authentication.getName();
        if (!uidAutenticado.equals(pedido.getIdUsuario())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Acceso denegado"));
        }

        try {
            String clientSecret = stripeService.crearIntentPago(pedido.getTotal(), pedidoId);
            return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
        } catch (Exception e) {
            System.err.println("Error al crear PaymentIntent: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el pago"));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = stripeService.construirEvento(payload, sigHeader);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
            stripeObject.ifPresent(obj -> {
                PaymentIntent intent = (PaymentIntent) obj;
                String pedidoId = intent.getMetadata().get("pedidoId");
                if (pedidoId != null) {
                    try {
                        pedidoService.actualizarEstado(pedidoId, "PENDIENTE");
                    } catch (Exception e) {
                        System.err.println("Error al actualizar pedido " + pedidoId + ": " + e.getMessage());
                    }
                }
            });
        }

        return ResponseEntity.ok("OK");
    }
}
