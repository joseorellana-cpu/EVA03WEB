package com.orellana.microservicios.ms_compra.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de salida EXACTO de la compra.
 * fecha = fecha de negocio (no la de auditoría).
 */
public record CompraResponse(
        Long id,
        String numeroCompra,
        Long proveedorId,
        LocalDateTime fecha,
        String estado,
        BigDecimal total,
        String observaciones,
        String creadoPor,
        String modificadoPor,
        List<DetalleResponse> detalles
) {
}
