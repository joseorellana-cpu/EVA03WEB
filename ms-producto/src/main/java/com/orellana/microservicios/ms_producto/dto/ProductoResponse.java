package com.orellana.microservicios.ms_producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Contrato de salida EXACTO del laboratorio (consumido por ms-compra).
 * bajoStockMinimo se calcula en el mapper (stock <= stockMinimo):
 * nunca se guarda en la base.
 */
public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        Integer stockMinimo,
        String categoria,
        String estado,
        Long proveedorId,
        boolean bajoStockMinimo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        String creadoPor,
        String modificadoPor
) {
}
