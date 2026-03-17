package com.illouncampero.Backend.model;

import lombok.Data;

@Data
public class Cupon {
    private String id;
    private String codigo;
    private double descuento;
    private String descripcion;
    private boolean activo;
}
