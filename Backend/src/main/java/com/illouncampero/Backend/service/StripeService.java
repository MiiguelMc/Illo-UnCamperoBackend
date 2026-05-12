package com.illouncampero.Backend.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    // Se ejecuta una sola vez al arrancar, no en cada petición
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String crearIntentPago(double total) throws Exception {
        long amountCents = Math.round(total * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("eur")
                .build();

        return PaymentIntent.create(params).getClientSecret();
    }
}
