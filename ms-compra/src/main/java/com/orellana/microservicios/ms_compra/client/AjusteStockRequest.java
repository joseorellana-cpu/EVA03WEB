package com.orellana.microservicios.ms_compra.client;

/**
 * Cuerpo de PATCH /api/productos/{id}/stock (contrato EXACTO
 * AjusteStockRequest de ms-producto): { cantidad > 0, operacion }.
 */
public record AjusteStockRequest(
        Integer cantidad,
        String operacion
) {
}
