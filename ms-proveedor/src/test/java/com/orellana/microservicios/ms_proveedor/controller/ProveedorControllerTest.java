package com.orellana.microservicios.ms_proveedor.controller;

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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test de controlador con MockMvc sobre el perfil test (H2, sin Eureka).
 * Comprueba el contrato HTTP: 400 con el formato global de error.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST con RUC inválido responde 400 con details campo: motivo")
    void crearConRucInvalidoRetorna400() throws Exception {
        String cuerpo = """
                {
                  "ruc": "20123",
                  "razonSocial": "PROVEEDOR INVALIDO S.A.",
                  "email": "ventas@invalido.pe"
                }
                """;

        mockMvc.perform(post("/api/proveedores")
                        .header("X-Usuario", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("La petición no cumple las validaciones"))
                .andExpect(jsonPath("$.details[0]").value(
                        "ruc: el RUC debe tener exactamente 11 dígitos numéricos"))
                .andExpect(jsonPath("$.path").value("/api/proveedores"));
    }

    @Test
    @DisplayName("POST válido responde 201 con estado ACTIVO y creadoPor del header")
    void crearValidoRetorna201() throws Exception {
        String cuerpo = """
                {
                  "ruc": "20712456789",
                  "razonSocial": "PROVEEDOR DE CONTROLADOR TEST S.A.",
                  "email": "ventas@test.pe"
                }
                """;

        mockMvc.perform(post("/api/proveedores")
                        .header("X-Usuario", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.creadoPor").value("test"));
    }
}
