package com.orellana.microservicios.ms_proveedor.dto;

/**
 * Respuesta simple para operaciones que no devuelven el recurso
 * (DELETE con baja lógica).
 */
public record MensajeResponse(String message) {
}
