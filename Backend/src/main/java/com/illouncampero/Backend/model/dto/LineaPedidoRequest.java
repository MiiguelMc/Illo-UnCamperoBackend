package com.illouncampero.Backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LineaPedidoRequest {

    @NotBlank(message = "El ID del producto es obligatorio")
    private String productoId;

    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 50, message = "La cantidad máxima por producto es 50")
    private int cantidad;

    private String notas;
}
