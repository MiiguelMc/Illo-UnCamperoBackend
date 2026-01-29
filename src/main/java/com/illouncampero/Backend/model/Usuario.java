package com.illouncampero.Backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Usuario {
    private String uid; // Este debe ser el mismo UID que genera Firebase
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String rol; // "CLIENTE", "ADMIN", "REPARTIDOR"
}