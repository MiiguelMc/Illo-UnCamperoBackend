package com.illouncampero.Backend.model;

import lombok.Data;

@Data
public class Cupon {
    private String id;
    private String codigo;
    private double descuento; // porcentaje: 10 = 10%
    private String descripcion;
    private boolean activo;
}
