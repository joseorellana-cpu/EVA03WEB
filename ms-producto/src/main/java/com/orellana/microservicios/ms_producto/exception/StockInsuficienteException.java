package com.orellana.microservicios.ms_producto.exception;

/** Regla de negocio de inventario: no alcanza el stock -> 409 */
public class StockInsuficienteException extends ExcepcionNegocio {

    public StockInsuficienteException(String message) {
        super(message);
    }
}
