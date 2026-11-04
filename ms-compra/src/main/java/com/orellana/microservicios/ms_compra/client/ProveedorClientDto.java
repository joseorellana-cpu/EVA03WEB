package com.orellana.microservicios.ms_compra.client;

import java.time.LocalDateTime;

/**
 * DTO con el que este microservicio lee a un proveedor de ms-proveedor.
 * Copia fiel del contrato ProveedorResponse.
 */
public record ProveedorClientDto(
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
