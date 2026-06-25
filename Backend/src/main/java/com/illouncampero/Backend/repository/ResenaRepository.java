package com.illouncampero.Backend.repository;

import com.illouncampero.Backend.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, String> {

    List<Resena> findTop50ByOrderByFechaDesc();
}
