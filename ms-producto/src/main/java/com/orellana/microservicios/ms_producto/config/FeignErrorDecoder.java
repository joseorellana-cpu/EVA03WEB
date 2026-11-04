package com.orellana.microservicios.ms_producto.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orellana.microservicios.ms_producto.client.ProveedorClient;
import com.orellana.microservicios.ms_producto.exception.ConflictoException;
import com.orellana.microservicios.ms_producto.exception.RecursoNoEncontradoException;
import com.orellana.microservicios.ms_producto.exception.ServicioNoDisponibleException;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Traduce las respuestas de error de ms-proveedor a excepciones de dominio:
 *  - 404 -> RecursoNoEncontradoException (mensaje del remoto o
 *           "El proveedor no existe en ms-proveedor") -> 404 local
 *  - 409 -> ConflictoException con el mensaje del servicio remoto -> 409 local
 *  - resto de 4xx/5xx -> ServicioNoDisponibleException -> 503 local
 * Nunca se expone el stack trace remoto al cliente, solo el mensaje claro
 * (formato global de error del servicio llamado).
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String mensajeRemoto = extraerMensaje(response);

        if (status == 404) {
            String mensaje = mensajeRemoto != null
                    ? mensajeRemoto
                    : "El proveedor solicitado no existe en ms-proveedor";
            return new RecursoNoEncontradoException(mensaje);
        }
        if (status == 409) {
            return new ConflictoException(
                    mensajeRemoto != null ? mensajeRemoto : "La operación fue rechazada por ms-proveedor");
        }
        return new ServicioNoDisponibleException(
                "El servicio " + nombreDeServicio(methodKey) + " no está disponible en este momento "
                        + "(HTTP " + status + "). Intente nuevamente.");
    }

    /** Extrae el campo "message" del JSON de error del servicio remoto (si viene). */
    private String extraerMensaje(Response response) {
        if (response.body() == null || response.body().length() <= 0) {
            return null;
        }
        try (Response.Body cuerpo = response.body()) {
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
            if (cliente.contains(ProveedorClient.class.getSimpleName())) {
                return "MS-PROVEEDOR";
            }
        }
        return "dependiente";
    }
}
