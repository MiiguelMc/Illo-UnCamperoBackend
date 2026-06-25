package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.TiendaConfig;
import com.illouncampero.Backend.repository.TiendaConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tienda")
public class TiendaController {

    private static final String CONFIG_ID = "tienda";

    private final TiendaConfigRepository tiendaConfigRepository;

    public TiendaController(TiendaConfigRepository tiendaConfigRepository) {
        this.tiendaConfigRepository = tiendaConfigRepository;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        boolean abierta = tiendaConfigRepository.findById(CONFIG_ID)
                .map(TiendaConfig::isAbierta)
                .orElse(true);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("abierta", abierta);
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/estado")
    public ResponseEntity<Map<String, Object>> cambiarEstado(@RequestBody Map<String, Boolean> body) {
        Boolean abierta = body.get("abierta");
        if (abierta == null) {
            return ResponseEntity.badRequest().build();
        }

        TiendaConfig config = tiendaConfigRepository.findById(CONFIG_ID).orElseGet(() -> {
            TiendaConfig c = new TiendaConfig();
            c.setId(CONFIG_ID);
            return c;
        });
        config.setAbierta(abierta);
        tiendaConfigRepository.save(config);

        Map<String, Object> datos = new HashMap<>();
        datos.put("abierta", abierta);
        return ResponseEntity.ok(datos);
    }
}
