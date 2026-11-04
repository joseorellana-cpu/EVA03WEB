package com.orellana.microservicios.ms_proveedor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Entrada del PATCH /{id}/estado: un solo campo con valores controlados.
 */
public record EstadoRequest(
        @NotBlank(message = "el estado es obligatorio")
        @Pattern(regexp = "ACTIVO|INACTIVO", message = "el estado solo admite ACTIVO o INACTIVO")
        String estado
) {
}
