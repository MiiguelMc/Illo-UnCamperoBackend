package com.illouncampero.Backend.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint ligero para keep-alive y warm-up. No toca Firestore.
 * DEBUG TEMPORAL: expone project_id/client_email de la credencial cargada
 * (NO el private_key) para diagnosticar qué cuenta usa el backend. Revertir después.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        try {
            String json = System.getenv("FIREBASE_JSON");
            if (json == null || json.isEmpty()) {
                body.put("cred", "FIREBASE_JSON AUSENTE");
            } else {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                body.put("project_id", o.has("project_id") ? o.get("project_id").getAsString() : "(falta)");
                body.put("client_email", o.has("client_email") ? o.get("client_email").getAsString() : "(falta)");
            }
        } catch (Exception e) {
            body.put("cred_error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return ResponseEntity.ok(body);
    }
}
