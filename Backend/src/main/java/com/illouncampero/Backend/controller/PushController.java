package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.PushSubscription;
import com.illouncampero.Backend.model.dto.PushSubscriptionRequest;
import com.illouncampero.Backend.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final String vapidPublicKey;

    public PushController(PushSubscriptionRepository pushSubscriptionRepository,
                          @Value("${vapid.public-key}") String vapidPublicKey) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
    }

    /** Clave publica VAPID que el navegador necesita para suscribirse. */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> publicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidPublicKey));
    }

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequest req, Authentication auth) {
        if (req.getEndpoint() == null || req.getKeys() == null
                || req.getKeys().getP256dh() == null || req.getKeys().getAuth() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Upsert por endpoint (un mismo navegador no duplica suscripciones).
        PushSubscription sub = pushSubscriptionRepository.findByEndpoint(req.getEndpoint())
                .orElseGet(PushSubscription::new);
        sub.setUsuarioId(auth.getName());
        sub.setEndpoint(req.getEndpoint());
        sub.setP256dh(req.getKeys().getP256dh());
        sub.setAuth(req.getKeys().getAuth());
        pushSubscriptionRepository.save(sub);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<Void> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            pushSubscriptionRepository.deleteByEndpoint(endpoint);
        }
        return ResponseEntity.ok().build();
    }
}
