package com.orellana.microservicios.ms_compra.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada EXACTO del laboratorio:
 * { proveedorId, observaciones, detalles: [ { productoId, cantidad, precioUnitario } ] }.
 * El precioUnitario de compra lo envía el cliente (> 0, máx 2 decimales) y
 * puede diferir del precio de catálogo de ms-producto (precio de referencia).
 */
public record CompraRequest(
        @NotNull(message = "el proveedorId es obligatorio")
        Long proveedorId,

        @Size(max = 300, message = "las observaciones no deben exceder 300 caracteres")
        String observaciones,

        @Size(min = 1, message = "la compra debe tener al menos un detalle")
        @Valid // cascada: valida cada DetalleRequest (productoId y cantidad >= 1)
        List<DetalleRequest> detalles
) {
}
