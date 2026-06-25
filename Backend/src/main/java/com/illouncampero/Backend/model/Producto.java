package com.illouncampero.Backend.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {
    @Id
    private String id;

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    private String descripcion;

    @Min(value = 0, message = "El precio no puede ser negativo")
    private Double precio;

    @Column(name = "imagen_url")
    private String imagenUrl;

    private String categoria;

    @NotBlank(message = "La subcategoría es obligatoria")
    private String subcategoria;

    private boolean disponible;

    @Column(name = "es_oferta")
    private boolean esOferta;
}
