package com.illouncampero.Backend.model;

import lombok.Data;

@Data
public class LineaPedido {
    private String productoId;
    private String nombre;
    private int cantidad;
    private double precioUnidad;
}