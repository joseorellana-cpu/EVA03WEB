package com.orellana.microservicios.ms_compra.model;

/**
 * Acciones que se registran en compra_auditoria, insertadas en la MISMA
 * transacción de cada cambio de estado de la compra.
 */
public enum AccionCompra {
    REGISTRADA,
    ACTUALIZADA,
    CONFIRMADA,
    ANULADA
}
