package com.orellana.microservicios.ms_producto.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduce cada excepción al formato global ApiError.
 * Extras de este microservicio:
 *  - StockInsuficienteException -> 409
 *  - ServicioNoDisponibleException -> 503
 * Nunca se exponen stack traces al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final List<String> SIN_DETALLES = List.of();

    /** Validación fallida (precio <= 0, código vacío, operacion inválida) -> 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "La petición no cumple las validaciones",
                        request.getRequestURI(),
                        detalles));
    }

    /** JSON mal formado -> 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarJsonInvalido(HttpMessageNotReadableException ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "El cuerpo de la petición no es un JSON válido",
                        request.getRequestURI(),
                        SIN_DETALLES));
    }

    /** Tipos incorrectos en la URL (por ejemplo /productos/abc) -> 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> manejarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "El valor '" + ex.getValue() + "' no es válido para '" + ex.getName() + "'",
                        request.getRequestURI(),
                        SIN_DETALLES));
    }

    /** No encontrado -> 404 */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(RecursoNoEncontradoException ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        SIN_DETALLES));
    }

    /** Duplicados, producto/proveedor inactivo, stock insuficiente -> 409 */
    @ExceptionHandler({ConflictoException.class, StockInsuficienteException.class})
    public ResponseEntity<ApiError> manejarConflicto(RuntimeException ex,
                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        SIN_DETALLES));
    }

    /** Servicio dependiente caído o sin respuesta -> 503 */
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ApiError> manejarServicioNoDisponible(ServicioNoDisponibleException ex,
                                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        SIN_DETALLES));
    }

    /** Cualquier otro error -> 500 genérico, SIN stack trace */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarGenerico(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Error interno del servicio. Revise el log del servidor.",
                        request.getRequestURI(),
                        SIN_DETALLES));
    }
}
