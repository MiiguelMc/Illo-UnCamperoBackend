package com.illouncampero.Backend.controller;


import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.service.ProductoService; // 1. Importamos el nuevo servicio
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public String guardar(@Valid @RequestBody Producto producto) throws Exception {
        System.out.println("Recibido desde el móvil: " + producto.toString());
        // 4. Llamamos al método guardar del nuevo servicio
        return productoService.guardarProducto(producto);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> borrar(@PathVariable String id) throws Exception {
        System.out.println("LOG CONTROLLER: Petición de BORRADO recibida para ID: " + id); // Log para ver qué ID llega
        String resultado = productoService.eliminarProducto(id);
        // Devuelve un mensaje de éxito con un estado 200 OK
        return ResponseEntity.ok(resultado);
    }
}
