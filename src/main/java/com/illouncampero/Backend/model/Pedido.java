package com.illouncampero.Backend.model;

import lombok.Data;
import java.util.List;

@Data
public class Pedido {
    private String id;               // ID único del pedido
    private String idUsuario;        // Quién lo pide (UID de Firebase)
    private String nombreCliente;    // Para que el repartidor sepa a quién llamar
    private String direccion;        // Calle, número, etc.
    private String telefono;         // Por si el repartidor se pierde
    private List<LineaPedido> productos; // La lista de lo que ha comprado
    private double total;            // Suma de todo
    private String estado;           // "PENDIENTE", "COCINANDO", "REPARTO", "ENTREGADO"
    private long fecha;              // Momento exacto del pedido (Timestamp)
}