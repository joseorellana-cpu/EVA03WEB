package com.orellana.microservicios.ms_compra.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato global de error (idéntico en los 3 microservicios):
 * timestamp, status, error, message, path, details.
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
