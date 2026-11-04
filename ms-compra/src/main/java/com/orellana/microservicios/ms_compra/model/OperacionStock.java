package com.orellana.microservicios.ms_compra.model;

/**
 * Operaciones sobre el inventario en ms-producto (contrato
 * AjusteStockRequest: DESCONTAR | INCREMENTAR).
 */
public enum OperacionStock {
    DESCONTAR,
    INCREMENTAR;

    /** Devuelve la operación contraria (para la compensación y la anulación). */
    public OperacionStock inversa() {
        return this == DESCONTAR ? INCREMENTAR : DESCONTAR;
    }
}
