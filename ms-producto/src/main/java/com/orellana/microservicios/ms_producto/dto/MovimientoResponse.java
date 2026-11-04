package com.orellana.microservicios.ms_producto.dto;

import java.time.LocalDateTime;

/**
 * Salida de GET /{id}/movimientos: historial del inventario.
 */
public record MovimientoResponse(
        Long id,
        Long productoId,
        String operacion,
        Integer cantidad,
        Integer stockAnterior,
        Integer stockNuevo,
        String usuario,
        LocalDateTime fecha,
        String motivo
) {
}
