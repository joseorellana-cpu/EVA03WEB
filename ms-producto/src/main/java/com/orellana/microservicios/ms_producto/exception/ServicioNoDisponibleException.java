package com.orellana.microservicios.ms_producto.exception;

/** Servicio dependiente caído o sin respuesta útil -> 503 */
public class ServicioNoDisponibleException extends ExcepcionNegocio {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }
}
