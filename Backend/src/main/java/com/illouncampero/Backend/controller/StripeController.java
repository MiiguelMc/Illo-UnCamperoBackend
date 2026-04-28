package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class StripeController {

    private final StripeService stripeService;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/crear-intent")
    public ResponseEntity<Map<String, String>> crearIntent(@RequestBody Map<String, Object> body) throws Exception {
        double total = Double.parseDouble(body.get("total").toString());
        String clientSecret = stripeService.crearIntentPago(total);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }
}
