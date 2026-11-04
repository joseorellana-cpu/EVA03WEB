package com.orellana.microservicios.ms_producto.exception;

/** Duplicados, proveedor INACTIVO y reglas de estado -> 409 */
public class ConflictoException extends ExcepcionNegocio {

    public ConflictoException(String message) {
        super(message);
    }
}
