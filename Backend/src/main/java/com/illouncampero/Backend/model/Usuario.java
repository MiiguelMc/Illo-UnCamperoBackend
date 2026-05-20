package com.illouncampero.Backend.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Usuario {
    private String uid;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
    private String apellidos;

    @Email(message = "El email no tiene un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "El teléfono no tiene un formato válido")
    private String telefono;

    @Size(max = 300, message = "La dirección no puede superar 300 caracteres")
    private String direccion;

    private String rol;
}