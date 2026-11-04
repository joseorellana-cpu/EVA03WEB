package com.orellana.microservicios.ms_compra.exception;

/** Recurso inexistente (compra, producto o proveedor remoto) -> 404 */
public class RecursoNoEncontradoException extends ExcepcionNegocio {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
