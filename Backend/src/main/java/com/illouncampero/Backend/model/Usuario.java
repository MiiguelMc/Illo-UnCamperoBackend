package com.illouncampero.Backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Usuario {
    private String uid;
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String rol;
}