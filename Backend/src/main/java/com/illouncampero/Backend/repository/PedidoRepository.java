package com.illouncampero.Backend.repository;

import com.illouncampero.Backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, String> {

    List<Pedido> findByIdUsuario(String idUsuario);

    List<Pedido> findByEstadoIn(List<String> estados);

    List<Pedido> findByFechaGreaterThanEqual(long desde);

    List<Pedido> findByFechaLessThanEqual(long hasta);

    List<Pedido> findByFechaGreaterThanEqualAndFechaLessThanEqual(long desde, long hasta);
}
