package com.orellana.microservicios.ms_compra.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Línea de la compra que envía el cliente: productoId, cantidad
 * (entero >= 1) y precioUnitario (> 0, máximo 2 decimales).
 *
 * El precioUnitario NO se toma de ms-producto: el precio de compra puede
 * diferir del precio de catálogo (promociones, negociación); el precio
 * de Producto.precio queda solo como precio de referencia.
 */
public record DetalleRequest(
        @NotNull(message = "el productoId es obligatorio")
        Long productoId,

        @NotNull(message = "la cantidad es obligatoria")
        @Min(value = 1, message = "la cantidad debe ser mayor que cero")
        Integer cantidad,

        @NotNull(message = "el precioUnitario es obligatorio")
        @DecimalMin(value = "0.01", message = "el precioUnitario debe ser mayor que cero")
        @Digits(integer = 9, fraction = 2, message = "el precioUnitario admite como maximo 2 decimales")
        BigDecimal precioUnitario
) {
}
