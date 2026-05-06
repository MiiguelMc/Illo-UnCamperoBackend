package com.illouncampero.Backend.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class CrearPedidoRequest {

    @NotEmpty(message = "El pedido debe tener al menos un producto")
    @Valid
    private List<LineaPedidoRequest> productos;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombreCliente;

    private String direccion;
    private String telefono;
    private String notasGenerales;

    @NotBlank(message = "El método de pago es obligatorio")
    @Pattern(regexp = "EFECTIVO|TARJETA", message = "Método de pago inválido: usa EFECTIVO o TARJETA")
    private String metodoPago;

    private String cupon;
    private Double descuento;
}
