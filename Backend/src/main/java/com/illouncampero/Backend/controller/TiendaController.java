package com.illouncampero.Backend.controller;

import com.google.cloud.firestore.Firestore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/tienda")
public class TiendaController {

    private final Firestore db;

    public TiendaController(Firestore db) {
        this.db = db;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() throws ExecutionException, InterruptedException {
        var doc = db.collection("config").document("tienda").get().get();
        Map<String, Object> respuesta = new HashMap<>();
        if (doc.exists() && doc.getBoolean("abierta") != null) {
            respuesta.put("abierta", doc.getBoolean("abierta"));
        } else {
            respuesta.put("abierta", true);
        }
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/estado")
    public ResponseEntity<Map<String, Object>> cambiarEstado(@RequestBody Map<String, Boolean> body)
            throws ExecutionException, InterruptedException {

        Boolean abierta = body.get("abierta");
        if (abierta == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("abierta", abierta);
        db.collection("config").document("tienda").set(datos).get();

        return ResponseEntity.ok(datos);
    }
}
