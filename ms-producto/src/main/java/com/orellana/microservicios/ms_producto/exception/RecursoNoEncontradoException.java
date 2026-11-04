package com.orellana.microservicios.ms_producto.exception;

/** Recurso inexistente (producto o proveedor remoto) -> 404 */
public class RecursoNoEncontradoException extends ExcepcionNegocio {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
