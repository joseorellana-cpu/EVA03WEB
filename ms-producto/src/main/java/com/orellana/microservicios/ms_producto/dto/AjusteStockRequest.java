package com.orellana.microservicios.ms_producto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Entrada de PATCH /{id}/stock (contrato EXACTO consumido por ms-compra):
 * { "cantidad": int > 0, "operacion": "DESCONTAR" | "INCREMENTAR" }.
 * Si el stock no alcanza al descontar -> 409 
 */
public record AjusteStockRequest(
        @NotNull(message = "la cantidad es obligatoria")
        @Min(value = 1, message = "la cantidad debe ser mayor que cero")
        Integer cantidad,

        @NotBlank(message = "la operacion es obligatoria")
        @Pattern(regexp = "DESCONTAR|INCREMENTAR",
                message = "la operacion solo admite DESCONTAR o INCREMENTAR")
        String operacion
) {
}
