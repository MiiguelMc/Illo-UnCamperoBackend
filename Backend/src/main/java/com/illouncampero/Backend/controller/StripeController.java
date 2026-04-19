package com.illouncampero.Backend.controller;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class StripeController {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostMapping("/crear-intent")
    public Map<String, String> crearIntent(@RequestBody Map<String, Object> body) throws Exception {
        Stripe.apiKey = stripeSecretKey;

        double total = Double.parseDouble(body.get("total").toString());
        // Stripe trabaja en céntimos (sin decimales)
        long amountCents = Math.round(total * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("eur")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return Map.of("clientSecret", intent.getClientSecret());
    }
}
