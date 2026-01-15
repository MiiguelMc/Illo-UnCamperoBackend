package com.illouncampero.Backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Esto es lo que genera el setId y getId
@NoArgsConstructor // Necesario para que Firebase pueda crear el objeto
@AllArgsConstructor
public class Producto {
    private String id;
    private String nombre;
    private String descripcion;
    private Double precio;
    private String imagenUrl;
}