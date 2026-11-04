package com.orellana.microservicios.ms_proveedor.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test de auditoría: comprueba que creadoPor toma el valor del header
 * X-Usuario y cae a "sistema" cuando el header no viene.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaProveedorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("creadoPor es el usuario del header X-Usuario")
    void creadoPorVieneDelHeader() throws Exception {
        String cuerpo = """
                {
                  "ruc": "20611111111",
                  "razonSocial": "AUDITORIA CON USUARIO S.A.",
                  "email": "auditoria1@test.pe"
                }
                """;

        String respuesta = mockMvc.perform(post("/api/proveedores")
                        .header("X-Usuario", "jose.orellana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(respuesta).get("id").asLong();

        String dto = mockMvc.perform(get("/api/proveedores/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(dto);
        assertEquals("jose.orellana", json.get("creadoPor").asText());
        assertEquals("jose.orellana", json.get("modificadoPor").asText());
    }

    @Test
    @DisplayName("creadoPor es 'sistema' cuando el header no viene")
    void creadoPorEsSistemaSinHeader() throws Exception {
        String cuerpo = """
                {
                  "ruc": "20622222222",
                  "razonSocial": "AUDITORIA SIN USUARIO S.A.",
                  "email": "auditoria2@test.pe"
                }
                """;

        String respuesta = mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        assertEquals("sistema", json.get("creadoPor").asText());
    }
}
