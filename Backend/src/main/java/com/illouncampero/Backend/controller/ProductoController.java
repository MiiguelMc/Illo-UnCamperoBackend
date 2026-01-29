package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.service.ProductoService; // 1. Importamos el nuevo servicio
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    // 2. Cambiamos la variable al servicio específico de productos
    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listar() throws Exception {
        // 3. Llamamos al método del nuevo servicio
        return productoService.obtenerTodos();
    }

    @PostMapping
    public String guardar(@RequestBody Producto producto) throws Exception {
        System.out.println("Recibido desde el móvil: " + producto.toString());
        // 4. Llamamos al método guardar del nuevo servicio
        return productoService.guardarProducto(producto);
    }

    @DeleteMapping("/{id}")
    public String borrar(@PathVariable String id) {
        // 5. Llamamos al método eliminar del nuevo servicio
        return productoService.eliminarProducto(id);
    }
}