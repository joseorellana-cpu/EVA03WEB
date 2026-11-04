package com.orellana.microservicios.ms_proveedor.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato global de error de la familia de excepciones de este ms.
 * (Los campos coinciden, campo a campo, con el formato pedido para
 * los 3 microservicios: timestamp, status, error, message, path, details).
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
