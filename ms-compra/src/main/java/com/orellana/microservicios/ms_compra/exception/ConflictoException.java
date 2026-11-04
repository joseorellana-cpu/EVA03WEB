package com.orellana.microservicios.ms_compra.exception;

/**
 * Regla de negocio genérica violada (productos repetidos, producto de
 * otro proveedor) -> 409
 */
public class ConflictoException extends ExcepcionNegocio {

    public ConflictoException(String message) {
        super(message);
    }
}
