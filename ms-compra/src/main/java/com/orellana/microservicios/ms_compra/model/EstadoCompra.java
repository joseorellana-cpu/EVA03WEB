package com.orellana.microservicios.ms_compra.model;

/**
 * Ciclo de vida de una compra:
 * REGISTRADA (crea, valida, NO descuenta stock)
 *   -> CONFIRMADA (descuenta stock en ms-producto)
 *   -> ANULADA (devuelve stock si estaba CONFIRMADA).
 */
public enum EstadoCompra {
    REGISTRADA,
    CONFIRMADA,
    ANULADA
}
