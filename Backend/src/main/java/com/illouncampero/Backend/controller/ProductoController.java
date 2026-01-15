package com.illouncampero.Backend.controller;

import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.service.FirebaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/productos") // Esta será la URL: localhost:8080/api/productos
public class ProductoController {

    private final FirebaseService firebaseService;

    public ProductoController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping
    public List<Producto> listarProductos() throws Exception {
        return firebaseService.getProductos();
    }
}