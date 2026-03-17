package com.illouncampero.Backend.model;

import lombok.Data;
import java.util.List;

@Data
public class Pedido {
    private String id;
    private String idUsuario;
    private String nombreCliente;
    private String direccion;
    private String telefono;
    private List<LineaPedido> productos;
    private double total;
    private String estado;
    private long fecha;
    private String notasGenerales;
    private String metodoPago;   
}
