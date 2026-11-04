package com.orellana.microservicios.ms_proveedor.exception;

/** Duplicados y reglas de estado violadas -> 409 */
public class ConflictoException extends ExcepcionNegocio {

    public ConflictoException(String message) {
        super(message);
    }
}
