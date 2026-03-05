package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Listar todos los productos (público)
    @GetMapping
    public List<Producto> listar() throws Exception {
        return productoService.obtenerTodos();
    }

    // Crear producto (solo ADMIN - protegido en SecurityConfig)
    @PostMapping
    public String guardar(@Valid @RequestBody Producto producto) throws Exception {
        return productoService.guardarProducto(producto);
    }

    // ✅ NUEVO: Editar producto existente (solo ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<String> editar(
            @PathVariable String id,
            @Valid @RequestBody Producto producto) throws Exception {
        producto.setId(id); // Aseguramos que el ID del path se usa
        String resultado = productoService.guardarProducto(producto);
        return ResponseEntity.ok(resultado);
    }

    // Borrar producto (solo ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> borrar(@PathVariable String id) throws Exception {
        String resultado = productoService.eliminarProducto(id);
        return ResponseEntity.ok(resultado);
    }
}
