package com.orellana.microservicios.ms_compra.exception;

/** Regla de inventario: no alcanza el stock del producto -> 409 */
public class StockInsuficienteException extends ExcepcionNegocio {

    public StockInsuficienteException(String message) {
        super(message);
    }
}
