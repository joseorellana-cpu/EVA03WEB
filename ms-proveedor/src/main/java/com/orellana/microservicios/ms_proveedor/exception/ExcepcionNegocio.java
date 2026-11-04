package com.orellana.microservicios.ms_proveedor.exception;

/**
 * Base de las excepciones de negocio: permite agregar familias nuevas
 * (regla nueva -> nueva subclase) sin tocar los controladores (SOLID: O).
 */
public abstract class ExcepcionNegocio extends RuntimeException {

    public ExcepcionNegocio(String message) {
        super(message);
    }
}
