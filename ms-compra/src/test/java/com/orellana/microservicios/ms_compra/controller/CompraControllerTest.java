package com.orellana.microservicios.ms_compra.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.orellana.microservicios.ms_compra.client.ProveedorClient;

import static org.mockito.Mockito.when;

import feign.Request;
import feign.Request.HttpMethod;
import feign.RetryableException;

/**
 * Tests de controlador con MockMvc (perfil test: H2 en memoria, sin
 * Eureka). Los clientes Feign se SIMULAN con @MockitoBean:
 *  1) cuerpo invalido (sin detalles) -> 400 con el formato global ApiError;
 *  2) ms-proveedor caido -> 503 con el formato global ApiError;
 *  3) proveedor INACTIVO -> 409;
 *  4) compra inexistente -> 404 con el formato global.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorClient proveedorClient;

    @Test
    @DisplayName("registrar sin detalles lanza 400 con el formato global ApiError")
    void registrarSinDetalles() throws Exception {
        String cuerpo = """
                {
                  "numeroCompra": "C-2026-000099",
                  "proveedorId": 1,
                  "observaciones": "Compra invalida",
                  "detalles": []
                }
                """;

        mockMvc.perform(post("/api/compras")
                        .header("X-Usuario", "jose.orellana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/compras"))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("registrar con ms-proveedor caido lanza 503 con el formato global ApiError")
    void servicioRemotoCaido() throws Exception {
        Request peticion = Request.create(HttpMethod.GET, "http://ms-proveedor/api/proveedores/1",
                java.util.Collections.emptyMap(), null,
                java.nio.charset.StandardCharsets.UTF_8);
        when(proveedorClient.obtenerProveedor(1L)).thenThrow(
                new RetryableException(-1, "Connection refused", HttpMethod.GET, (Long) null, peticion));

        String cuerpo = """
                {
                  "numeroCompra": "C-2026-000099",
                  "proveedorId": 1,
                  "observaciones": "Compra con servicio caido",
                  "detalles": [ { "productoId": 10, "cantidad": 3, "precioUnitario": 15.50 } ]
                }
                """;

        mockMvc.perform(post("/api/compras")
                        .header("X-Usuario", "jose.orellana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("El servicio MS-PROVEEDOR no esta disponible en este momento. Intente nuevamente."))
                .andExpect(jsonPath("$.path").value("/api/compras"));
    }

    @Test
    @DisplayName("registrar con proveedor INACTIVO lanza 409 con el formato global ApiError")
    void registrarConProveedorInactivo() throws Exception {
        when(proveedorClient.obtenerProveedor(2L)).thenReturn(
                new com.orellana.microservicios.ms_compra.client.ProveedorClientDto(
                        2L, "20487654321", "DISTRIBUIDORA LOS ANDES E.I.R.L.", null, null,
                        null, null, "INACTIVO", java.time.LocalDateTime.now(), "sistema", "sistema"));

        String cuerpo = """
                {
                  "numeroCompra": "C-2026-000099",
                  "proveedorId": 2,
                  "observaciones": "Compra con proveedor inactivo",
                  "detalles": [ { "productoId": 10, "cantidad": 3, "precioUnitario": 15.50 } ]
                }
                """;

        mockMvc.perform(post("/api/compras")
                        .header("X-Usuario", "jose.orellana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("El proveedor 2 no esta ACTIVO"))
                .andExpect(jsonPath("$.path").value("/api/compras"));
    }

    @Test
    @DisplayName("consultar una compra inexistente lanza 404 con el formato global ApiError")
    void compraInexistente() throws Exception {
        mockMvc.perform(get("/api/compras/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/compras/999"));
    }
}
