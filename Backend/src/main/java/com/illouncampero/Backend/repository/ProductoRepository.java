package com.illouncampero.Backend.repository;

import com.illouncampero.Backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, String> {
}
