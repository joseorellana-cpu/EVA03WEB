package com.orellana.microservicios.ms_proveedor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada de creación y actualización de proveedor.
 * El estado NO llega por aquí: se gestiona con PATCH /estado
 * y al crear siempre inicia en ACTIVO.
 */
public record ProveedorRequest(
        @NotBlank(message = "el RUC es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "el RUC debe tener exactamente 11 dígitos numéricos")
        String ruc,

        @NotBlank(message = "la razón social es obligatoria")
        @Size(max = 150, message = "la razón social no debe exceder 150 caracteres")
        String razonSocial,

        @Size(max = 150, message = "el nombre comercial no debe exceder 150 caracteres")
        String nombreComercial,

        @Email(message = "el email no tiene un formato válido")
        @Size(max = 120, message = "el email no debe exceder 120 caracteres")
        String email,

        @Size(max = 20, message = "el teléfono no debe exceder 20 caracteres")
        String telefono,

        @Size(max = 200, message = "la dirección no debe exceder 200 caracteres")
        String direccion
) {
}
