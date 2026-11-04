package com.orellana.microservicios.ms_compra.dto;

import java.time.LocalDateTime;

/**
 * Salida de GET /{id}/auditoria: historial completo de la compra.
 */
public record AuditoriaResponse(
        Long id,
        Long compraId,
        String accion,
        String estadoAnterior,
        String estadoNuevo,
        String usuario,
        LocalDateTime fecha,
        String detalle
) {
}
