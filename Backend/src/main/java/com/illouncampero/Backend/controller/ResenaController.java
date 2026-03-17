package com.illouncampero.Backend.controller;

import com.google.cloud.firestore.Firestore;
import com.illouncampero.Backend.model.Resena;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/resenas")
@CrossOrigin(origins = "*")
public class ResenaController {

    private final Firestore db;

    public ResenaController(Firestore db) {
        this.db = db;
    }

    @PostMapping
    public ResponseEntity<String> crearResena(
            @RequestBody Resena resena,
            Authentication auth) throws ExecutionException, InterruptedException {

        String uid = auth.getName();
        resena.setIdUsuario(uid);
        resena.setFecha(System.currentTimeMillis());

        if (resena.getPuntuacion() < 1 || resena.getPuntuacion() > 5) {
            return ResponseEntity.badRequest().body("La puntuación debe ser entre 1 y 5.");
        }
        if (resena.getIdPedido() == null || resena.getIdPedido().isBlank()) {
            return ResponseEntity.badRequest().body("El ID del pedido es obligatorio.");
        }

        var pedidoDoc = db.collection("pedidos").document(resena.getIdPedido()).get().get();
        if (!pedidoDoc.exists()) {
            return ResponseEntity.badRequest().body("El pedido no existe.");
        }

        String estadoPedido = pedidoDoc.getString("estado");
        if (!"ENTREGADO".equals(estadoPedido)) {
            return ResponseEntity.badRequest().body("Solo puedes valorar pedidos entregados.");
        }

        String idUsuarioPedido = pedidoDoc.getString("idUsuario");
        if (!uid.equals(idUsuarioPedido)) {
            return ResponseEntity.status(403).body("No puedes valorar un pedido que no es tuyo.");
        }

        String id = UUID.randomUUID().toString();
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", id);
        datos.put("idPedido", resena.getIdPedido());
        datos.put("idUsuario", uid);
        datos.put("puntuacion", resena.getPuntuacion());
        datos.put("comentario", resena.getComentario() != null ? resena.getComentario() : "");
        datos.put("fecha", resena.getFecha());

        db.collection("resenas").document(id).set(datos).get();
        db.collection("pedidos").document(resena.getIdPedido()).update("valorado", true).get();

        return ResponseEntity.ok("Reseña guardada. Gracias por tu opinión.");
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarResenas() throws ExecutionException, InterruptedException {
        var docs = db.collection("resenas")
                .orderBy("fecha", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get().get().getDocuments();

        List<Map<String, Object>> lista = new ArrayList<>();
        for (var doc : docs) {
            Map<String, Object> r = new HashMap<>(doc.getData());
            try {
                String idUsuario = (String) r.get("idUsuario");
                var userDoc = db.collection("usuarios").document(idUsuario).get().get();
                if (userDoc.exists()) {
                    r.put("nombreUsuario", userDoc.getString("nombre"));
                }
            } catch (Exception e) {
                r.put("nombreUsuario", "Cliente");
            }
            lista.add(r);
        }
        return ResponseEntity.ok(lista);
    }
}
