package com.illouncampero.Backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tienda_config")
public class TiendaConfig {
    @Id
    private String id;       // siempre "tienda"
    private boolean abierta;
}
