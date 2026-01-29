package com.illouncampero.Backend.model;


import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class Producto {
    private String id;
    private String nombre;
    private String descripcion;
    private Double precio;
    private String imagenUrl;
    private String categoria; // "camperos", "bebidas", "entrantes"
    private boolean disponible;
}
