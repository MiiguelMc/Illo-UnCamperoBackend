package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Cupon;
import com.illouncampero.Backend.repository.CuponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    private final CuponRepository cuponRepository;

    public CuponController(CuponRepository cuponRepository) {
        this.cuponRepository = cuponRepository;
    }

    @PostMapping("/validar")
    public ResponseEntity<Map<String, Object>> validarCupon(@RequestBody Map<String, String> body) {
        String codigo = body.get("codigo");
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Cupon> cuponOpt = cuponRepository.findByCodigoAndActivoTrue(codigo.toUpperCase().trim());

        if (cuponOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("valido", false);
            error.put("mensaje", "Cupón no válido o expirado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Cupon cupon = cuponOpt.get();
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("valido", true);
        respuesta.put("descuento", cupon.getDescuento());
        respuesta.put("descripcion", cupon.getDescripcion());
        respuesta.put("codigo", cupon.getCodigo());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping
    public ResponseEntity<String> crearCupon(@RequestBody Cupon cupon) {
        if (cupon.getCodigo() == null || cupon.getCodigo().isBlank()) {
            return ResponseEntity.badRequest().body("El código del cupón es obligatorio.");
        }
        if (cupon.getDescuento() <= 0 || cupon.getDescuento() > 100) {
            return ResponseEntity.badRequest().body("El descuento debe estar entre 1 y 100.");
        }

        cupon.setId(UUID.randomUUID().toString());
        cupon.setCodigo(cupon.getCodigo().toUpperCase().trim());
        cupon.setDescripcion(((int) cupon.getDescuento()) + "% de descuento");
        cupon.setActivo(true);

        cuponRepository.save(cupon);
        return ResponseEntity.ok("Cupón '" + cupon.getCodigo() + "' creado con éxito.");
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<String> desactivarCupon(@PathVariable String id) {
        Optional<Cupon> cuponOpt = cuponRepository.findById(id);
        if (cuponOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Cupon cupon = cuponOpt.get();
        cupon.setActivo(false);
        cuponRepository.save(cupon);
        return ResponseEntity.ok("Cupón desactivado.");
    }

    @GetMapping
    public ResponseEntity<List<Cupon>> listarCupones() {
        return ResponseEntity.ok(cuponRepository.findAll());
    }
}
