package com.orellana.microservicios.ms_compra.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO con el que este microservicio lee un producto de ms-producto.
 * Copia fiel del contrato ProductoResponse (incluye bajoStockMinimo).
 */
public record ProductoClientDto(
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
