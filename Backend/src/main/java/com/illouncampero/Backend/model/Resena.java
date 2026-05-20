package com.illouncampero.Backend.model;

import lombok.Data;

@Data
public class Resena {
    private String id;
    private String idPedido;
    private String idUsuario;
    private int puntuacion;
    private String comentario;
    private long fecha;
}
