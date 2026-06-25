package com.illouncampero.Backend.repository;

import com.illouncampero.Backend.model.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, String> {

    Optional<Cupon> findByCodigoAndActivoTrue(String codigo);

    Optional<Cupon> findByCodigo(String codigo);
}
