package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.service.FirebaseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*") // Permite que Angular y el móvil se conecten sin bloqueos
public class ProductoController {

    private final FirebaseService firebaseService;

    public ProductoController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping
    public List<Producto> listar() throws Exception {
        return firebaseService.getProductos();
    }

    @PostMapping
    public String guardar(@RequestBody Producto producto) throws Exception {
        return firebaseService.saveProducto(producto);
    }

    @DeleteMapping("/{id}")
    public String borrar(@PathVariable String id) {
        return firebaseService.deleteProducto(id);
    }
}