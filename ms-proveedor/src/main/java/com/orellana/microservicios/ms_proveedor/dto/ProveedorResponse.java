package com.orellana.microservicios.ms_proveedor.dto;

import java.time.LocalDateTime;

/**
 * Contrato de salida según el contrato EXACTO del laboratorio.
 * fechaRegistro viene de la auditoría (fechaCreacion): el cliente
 * no lo envía ni el service lo calcula.
 */
public record ProveedorResponse(
        Long id,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String email,
        String telefono,
        String direccion,
        String estado,
        LocalDateTime fechaRegistro,
        String creadoPor,
        String modificadoPor
) {
}
