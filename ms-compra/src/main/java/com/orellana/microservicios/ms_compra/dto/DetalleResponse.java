package com.orellana.microservicios.ms_compra.dto;

import java.math.BigDecimal;

/**
 * Línea registrada de la compra (contrato EXACTO).
 */
public record DetalleResponse(
        Long id,
        Long productoId,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
