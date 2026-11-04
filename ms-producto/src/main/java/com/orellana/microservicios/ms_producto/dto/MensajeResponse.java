package com.orellana.microservicios.ms_producto.dto;

/**
 * Respuesta simple para operaciones que no devuelven el recurso
 * (DELETE con baja lógica).
 */
public record MensajeResponse(String message) {
}
