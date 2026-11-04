package com.orellana.microservicios.ms_compra.exception;

/** El proveedor de la compra está INACTIVO -> 409 */
public class ProveedorInactivoException extends ExcepcionNegocio {

    public ProveedorInactivoException(String message) {
        super(message);
    }
}
