package com.orellana.microservicios.ms_producto.model;

/**
 * Operaciones permitidas sobre el inventario. El ajuste de stock es el
 * punto único de entrada del inventario (usado también por ms-compra
 * a traves de PATCH /{id}/stock).
 */
public enum OperacionStock {
    DESCONTAR,
    INCREMENTAR
}
