package com.orellana.microservicios.ms_compra.exception;

/**
 * Base de las excepciones de negocio de este microservicio.
 */
public abstract class ExcepcionNegocio extends RuntimeException {

    public ExcepcionNegocio(String message) {
        super(message);
    }
}
