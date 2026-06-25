package com.illouncampero.Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "resenas")
public class Resena {
    @Id
    private String id;

    @Column(name = "id_pedido")
    private String idPedido;

    @Column(name = "id_usuario")
    private String idUsuario;

    private int puntuacion;
    private String comentario;
    private long fecha;
}
