package com.illouncampero.Backend.controller;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.illouncampero.Backend.model.Cupon;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/cupones")
@CrossOrigin(origins = "*")
public class CuponController {

    private final Firestore db;

    public CuponController(Firestore db) {
        this.db = db;
    }

    // Validar cupón - autenticado
    @PostMapping("/validar")
    public ResponseEntity<Map<String, Object>> validarCupon(@RequestBody Map<String, String> body)
            throws ExecutionException, InterruptedException {

        String codigo = body.get("codigo");
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Buscamos el cupón por código (campo "codigo" en Firestore)
        var docs = db.collection("cupones")
                .whereEqualTo("codigo", codigo.toUpperCase().trim())
                .whereEqualTo("activo", true)
                .get().get().getDocuments();

        if (docs.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("valido", false);
            error.put("mensaje", "Cupón no válido o expirado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        DocumentSnapshot doc = docs.get(0);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("valido", true);
        respuesta.put("descuento", doc.getDouble("descuento")); // porcentaje, ej: 10.0
        respuesta.put("descripcion", doc.getString("descripcion"));
        respuesta.put("codigo", doc.getString("codigo"));

        return ResponseEntity.ok(respuesta);
    }

    // Crear cupón - solo ADMIN
    @PostMapping
    public ResponseEntity<String> crearCupon(@RequestBody Cupon cupon)
            throws ExecutionException, InterruptedException {

        if (cupon.getCodigo() == null || cupon.getCodigo().isBlank()) {
            return ResponseEntity.badRequest().body("El código del cupón es obligatorio.");
        }
        if (cupon.getDescuento() <= 0 || cupon.getDescuento() > 100) {
            return ResponseEntity.badRequest().body("El descuento debe estar entre 1 y 100.");
        }

        cupon.setCodigo(cupon.getCodigo().toUpperCase().trim());
        cupon.setActivo(true);

        String id = UUID.randomUUID().toString();
        Map<String, Object> datos = new HashMap<>();
        datos.put("codigo", cupon.getCodigo());
        datos.put("descuento", cupon.getDescuento());
        datos.put("descripcion", cupon.getDescuento() + "% de descuento");
        datos.put("activo", true);

        db.collection("cupones").document(id).set(datos).get();
        System.out.println("LOG: Cupón creado: " + cupon.getCodigo() + " (" + cupon.getDescuento() + "%)");

        return ResponseEntity.ok("Cupón '" + cupon.getCodigo() + "' creado con éxito.");
    }

    // Desactivar cupón - solo ADMIN
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<String> desactivarCupon(@PathVariable String id)
            throws ExecutionException, InterruptedException {

        var doc = db.collection("cupones").document(id).get().get();
        if (!doc.exists()) {
            return ResponseEntity.notFound().build();
        }
        db.collection("cupones").document(id).update("activo", false).get();
        return ResponseEntity.ok("Cupón desactivado.");
    }

    // Listar todos los cupones - solo ADMIN
    @GetMapping
    public ResponseEntity<?> listarCupones() throws ExecutionException, InterruptedException {
        var docs = db.collection("cupones").get().get().getDocuments();
        var lista = docs.stream().map(doc -> {
            Map<String, Object> c = new HashMap<>(doc.getData());
            c.put("id", doc.getId());
            return c;
        }).toList();
        return ResponseEntity.ok(lista);
    }
}
