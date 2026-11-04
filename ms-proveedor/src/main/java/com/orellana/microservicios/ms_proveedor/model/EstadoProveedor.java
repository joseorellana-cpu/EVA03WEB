package com.orellana.microservicios.ms_proveedor.model;

/**
 * Estados del proveedor. NO existe borrado físico: la eliminación
 * es baja lógica a INACTIVO (decisión fija del laboratorio).
 */
public enum EstadoProveedor {
    ACTIVO,
    INACTIVO
}
