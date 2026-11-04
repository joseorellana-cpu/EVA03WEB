package com.orellana.microservicios.ms_compra.exception;

/**
 * Regla del ciclo de vida: una compra ANULADA no se modifica, y una
 * compra solo se confirma desde REGISTRADA -> 409
 */
public class CompraNoModificableException extends ExcepcionNegocio {

    public CompraNoModificableException(String message) {
        super(message);
    }
}
