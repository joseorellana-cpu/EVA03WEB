package com.orellana.microservicios.ms_producto.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orellana.microservicios.ms_producto.client.ProveedorClient;
import com.orellana.microservicios.ms_producto.client.ProveedorClientDto;

import static org.mockito.Mockito.when;

/**
 * Test de auditoría del ms-producto:
 * 1) creadoPor/modificadoPor toman el header X-Usuario (y "sistema" sin header);
 * 2) cada ajuste de stock deja un StockMovimiento con el usuario del header.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaProductoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProveedorClient proveedorClient;

    @Test
    @DisplayName("creadoPor es el header X-Usuario y el ajuste registra movimiento con ese usuario")
    void auditoriaYMovimientoDeStock() throws Exception {
        when(proveedorClient.obtenerProveedor(1L))
                .thenReturn(new ProveedorClientDto(1L, "20100047110", "INNOVA DISTRIBUCIONES S.A.C.",
                        "Innova Distribuciones", "ventas@innova.pe", "01-2345678",
                        "Av. Argentina 1450, Callao", "ACTIVO",
                        LocalDateTime.now(), "sistema", "sistema"));

        String cuerpo = """
                {
                  "codigo": "PRD-A100",
                  "nombre": "Producto de auditoria test",
                  "precio": 20.00,
                  "stock": 20,
                  "stockMinimo": 10,
                  "proveedorId": 1
                }
                """;

        String respuesta = mockMvc.perform(post("/api/productos")
                        .header("X-Usuario", "jose.orellana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(respuesta).get("id").asLong();

        // GET confirma la auditoría del header
        mockMvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadoPor").value("jose.orellana"))
                .andExpect(jsonPath("$.modificadoPor").value("jose.orellana"));

        // PATCH de stock: descontar 5 sobre stock 20 -> stock 15
        mockMvc.perform(patch("/api/productos/" + id + "/stock")
                        .header("X-Usuario", "jose.orellana")
                        .param("motivo", "venta de prueba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\": 5, \"operacion\": \"DESCONTAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15))
                .andExpect(jsonPath("$.bajoStockMinimo").value(false))
                .andExpect(jsonPath("$.modificadoPor").value("jose.orellana"));

        // El historial debe registrar el movimiento con el usuario del header
        String historial = mockMvc.perform(get("/api/productos/" + id + "/movimientos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode movimientos = objectMapper.readTree(historial);
        assertEquals(1, movimientos.size());
        JsonNode movimiento = movimientos.get(0);
        assertEquals("DESCONTAR", movimiento.get("operacion").asText());
        assertEquals(5, movimiento.get("cantidad").asInt());
        assertEquals(20, movimiento.get("stockAnterior").asInt());
        assertEquals(15, movimiento.get("stockNuevo").asInt());
        assertEquals("jose.orellana", movimiento.get("usuario").asText());
        assertEquals("venta de prueba", movimiento.get("motivo").asText());
    }

    @Test
    @DisplayName("sin header X-Usuario la auditoria usa 'sistema'")
    void creadoPorEsSistemaSinHeader() throws Exception {
        when(proveedorClient.obtenerProveedor(1L))
                .thenReturn(new ProveedorClientDto(1L, "20100047110", "INNOVA DISTRIBUCIONES S.A.C.",
                        "Innova Distribuciones", "ventas@innova.pe", "01-2345678",
                        "Av. Argentina 1450, Callao", "ACTIVO",
                        LocalDateTime.now(), "sistema", "sistema"));

        String cuerpo = """
                {
                  "codigo": "PRD-A200",
                  "nombre": "Producto de auditoria sin header",
                  "precio": 20.00,
                  "stock": 20,
                  "stockMinimo": 10,
                  "proveedorId": 1
                }
                """;

        String respuesta = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        assertEquals("sistema", json.get("creadoPor").asText());
    }
}
