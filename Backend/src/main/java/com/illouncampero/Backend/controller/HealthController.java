package com.illouncampero.Backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint ligero para el keep-alive (GitHub Actions) y el warm-up del frontend.
 * No toca Firestore ni ningún servicio: solo confirma que la app está viva,
 * para no gastar lecturas de la base de datos en cada ping.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
