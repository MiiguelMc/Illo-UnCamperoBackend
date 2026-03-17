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

    @GetMapping
    public List<Producto> listar() throws Exception {
        return productoService.obtenerTodos();
    }

    @PostMapping
    public String guardar(@Valid @RequestBody Producto producto) throws Exception {
        return productoService.guardarProducto(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> editar(
            @PathVariable String id,
            @Valid @RequestBody Producto producto) throws Exception {
        producto.setId(id);
        String resultado = productoService.guardarProducto(producto);
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> borrar(@PathVariable String id) throws Exception {
        String resultado = productoService.eliminarProducto(id);
        return ResponseEntity.ok(resultado);
    }
}
