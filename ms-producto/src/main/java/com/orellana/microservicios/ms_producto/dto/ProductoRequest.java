package com.orellana.microservicios.ms_producto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada de creación/actualización de producto.
 * El proveedorId se valida via Feign contra ms-proveedor (existencia
 * + estado ACTIVO): esa validación es de negocio, no de formato.
 */
public record ProductoRequest(
        @NotBlank(message = "el código es obligatorio")
        @Size(max = 30, message = "el código no debe exceder 30 caracteres")
        String codigo,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no debe exceder 150 caracteres")
        String nombre,

        @Size(max = 500, message = "la descripción no debe exceder 500 caracteres")
        String descripcion,

        @NotNull(message = "el precio es obligatorio")
        @DecimalMin(value = "0.01", message = "el precio debe ser mayor que cero")
        BigDecimal precio,

        @NotNull(message = "el stock es obligatorio")
        @Min(value = 0, message = "el stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "el stock mínimo es obligatorio")
        @Min(value = 0, message = "el stock mínimo no puede ser negativo")
        Integer stockMinimo,

        @Size(max = 80, message = "la categoría no debe exceder 80 caracteres")
        String categoria,

        @NotNull(message = "el proveedorId es obligatorio")
        Long proveedorId
) {
}
