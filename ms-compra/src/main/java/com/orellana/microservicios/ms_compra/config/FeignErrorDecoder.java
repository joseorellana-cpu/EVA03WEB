package com.orellana.microservicios.ms_compra.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orellana.microservicios.ms_compra.client.ProductoClient;
import com.orellana.microservicios.ms_compra.client.ProveedorClient;
import com.orellana.microservicios.ms_compra.exception.ConflictoException;
import com.orellana.microservicios.ms_compra.exception.ProveedorInactivoException;
import com.orellana.microservicios.ms_compra.exception.RecursoNoEncontradoException;
import com.orellana.microservicios.ms_compra.exception.StockInsuficienteException;
import com.orellana.microservicios.ms_compra.exception.ServicioNoDisponibleException;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Traduce las respuestas de error de los servicios remotos a excepciones
 * de dominio con mensaje en español:
 *  - 404 -> RecursoNoEncontradoException -> 404 local
 *  - 409:
 *      * mensaje con "Stock insuficiente" -> StockInsuficienteException -> 409
 *      * mensaje con "INACTIVO" en MS-PROVEEDOR -> ProveedorInactivoException -> 409
 *      * resto -> ConflictoException -> 409
 *  - resto de 4xx/5xx -> ServicioNoDisponibleException -> 503 local
 * Nunca se expone el stack trace remoto al cliente.
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String mensajeRemoto = extraerMensaje(response);
        String servicio = nombreDeServicio(methodKey);

        if (status == 404) {
            return new RecursoNoEncontradoException(
                    mensajeRemoto != null ? mensajeRemoto : "El recurso solicitado no existe en " + servicio);
        }
        if (status == 409) {
            String mensaje = mensajeRemoto != null ? mensajeRemoto : "La operación fue rechazada por " + servicio;
            if (mensaje.contains("Stock insuficiente")) {
                return new StockInsuficienteException(mensaje);
            }
            if (mensaje.contains("INACTIVO") && servicio.equals("MS-PROVEEDOR")) {
                return new ProveedorInactivoException(mensaje);
            }
            return new ConflictoException(mensaje);
        }
        return new ServicioNoDisponibleException(
                "El servicio " + servicio + " no está disponible en este momento (HTTP " + status
                        + "). Intente nuevamente.");
    }

    /** Extrae el campo "message" del JSON de error del servicio remoto (si viene). */
    private String extraerMensaje(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (Response.Body cuerpo = response.body()) {
            // NO se usa body().length(): con respuestas chunked (sin
            // content-length) devuelve null y provocaria un NPE.
            String json = new String(cuerpo.asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode nodo = objectMapper.readTree(json);
            if (nodo.hasNonNull("message")) {
                return nodo.get("message").asText();
            }
        } catch (IOException error) {
            // el cuerpo no es JSON legible: se usa el mensaje por defecto
        }
        return null;
    }

    private String nombreDeServicio(String methodKey) {
        if (methodKey != null && methodKey.contains("#")) {
            String cliente = methodKey.substring(0, methodKey.indexOf('#'));
            if (cliente.contains(ProductoClient.class.getSimpleName())) {
                return "MS-PRODUCTO";
            }
            if (cliente.contains(ProveedorClient.class.getSimpleName())) {
                return "MS-PROVEEDOR";
            }
        }
        return "dependiente";
    }
}
