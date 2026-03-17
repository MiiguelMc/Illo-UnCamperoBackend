package com.illouncampero.Backend.model;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class Producto {
    private String id;

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    private String descripcion;

    @Min(value = 0, message = "El precio no puede ser negativo")
    private Double precio;

    private String imagenUrl;

    private String categoria;

    @NotBlank(message = "La subcategoría es obligatoria")
    private String subcategoria;
    
    private boolean disponible;
}
