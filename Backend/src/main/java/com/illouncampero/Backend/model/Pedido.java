package com.illouncampero.Backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    private String id;

    @Column(name = "id_usuario")
    private String idUsuario;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    private String direccion;
    private String telefono;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<LineaPedido> productos = new ArrayList<>();

    private double total;
    private String estado;
    private long fecha;

    @Column(name = "notas_generales")
    private String notasGenerales;

    @Column(name = "metodo_pago")
    private String metodoPago;

    private String cupon;      // Para guardar "LOLO"
    private Double descuento;

    private boolean valorado;

    /** Mantiene la relacion bidireccional al asignar las lineas. */
    public void setProductos(List<LineaPedido> productos) {
        this.productos = productos;
        if (productos != null) {
            for (LineaPedido linea : productos) {
                linea.setPedido(this);
            }
        }
    }
}
