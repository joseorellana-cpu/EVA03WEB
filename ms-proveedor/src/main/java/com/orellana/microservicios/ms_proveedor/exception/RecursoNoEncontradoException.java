package com.orellana.microservicios.ms_proveedor.exception;

/** Recurso inexistente -> 404 */
public class RecursoNoEncontradoException extends ExcepcionNegocio {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
