package com.illouncampero.Backend.service;

import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto obtenerPorId(String id) {
        return productoRepository.findById(id).orElse(null);
    }

    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    public String guardarProducto(Producto producto) {
        if (producto.getId() == null || producto.getId().isEmpty()) {
            producto.setId(UUID.randomUUID().toString());
        }
        productoRepository.save(producto);
        return "Producto guardado con éxito";
    }

    public String eliminarProducto(String id) {
        String idLimpio = id.trim();
        if (productoRepository.existsById(idLimpio)) {
            productoRepository.deleteById(idLimpio);
            return "Producto eliminado con éxito";
        }
        return "El producto no existe o ya fue eliminado.";
    }
}
