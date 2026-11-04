package com.orellana.microservicios.ms_compra.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orellana.microservicios.ms_compra.client.AjusteStockRequest;
import com.orellana.microservicios.ms_compra.client.ProductoClient;
import com.orellana.microservicios.ms_compra.client.ProductoClientDto;
import com.orellana.microservicios.ms_compra.client.ProveedorClient;
import com.orellana.microservicios.ms_compra.client.ProveedorClientDto;

/**
 * Test integrado de auditoria del ms-compra (perfil test, H2 + Feign
 * simulado):
 *  1) registrar NO llama a ms-producto (no descuenta stock);
 *  2) confirmar SI llama con DESCONTAR (DECISION J);
 *  3) anular sobre una CONFIRMADA SI llama con INCREMENTAR (devolucion);
 *  4) GET /auditoria muestra cada accion con QUIEN la hizo (header
 *     X-Usuario).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaCompraTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProveedorClient proveedorClient;

    @MockitoBean
    private ProductoClient productoClient;

    @Test
    @DisplayName("ciclo registrar-no-descuenta, confirmar-descuenta, anular-devuelve; auditoria registra al usuario del header")
    void cicloCompletoConAuditoria() throws Exception {
        when(proveedorClient.obtenerProveedor(1L))
                .thenReturn(new ProveedorClientDto(1L, "20100047110", "INNOVA DISTRIBUCIONES S.A.C.",
                        "Innova Distribuciones", "ventas@innova.pe", "01-2345678",
                        "Av. Argentina 1450, Callao", "ACTIVO",
                        LocalDateTime.now(), "sistema", "sistema"));
        when(productoClient.obtenerProducto(11L))
                .thenReturn(productoDe(11L, 100));
        when(productoClient.ajustarStock(any(), any()))
                .thenAnswer(inv -> productoDe(11L, 97));        /* ---- 1) registrar: no descuenta ---- */
        String cuerpo = """
                {
                  "proveedorId": 1,
                  "observaciones": "Compra para auditoria",
                  "detalles": [ { "productoId": 11, "cantidad": 3, "precioUnitario": 12.50 } ]
                }
                """;
        String respuesta = mockMvc.perform(post("/api/compras")
                        .header("X-Usuario", "jose.orellana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.total").value(37.50))
                .andExpect(jsonPath("$.creadoPor").value("jose.orellana"))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(respuesta).get("id").asLong();

        verify(productoClient, never()).ajustarStock(any(), any());

        /* ---- 2) confirmar: SI descuenta con DESCONTAR ---- */
        mockMvc.perform(post("/api/compras/" + id + "/confirmar")
                        .header("X-Usuario", "jose.orellana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        verify(productoClient).ajustarStock(eq(11L),
                eq(new AjusteStockRequest(3, "DESCONTAR")));

        /* ---- 3) anular una CONFIRMADA: SI devuelve con INCREMENTAR ---- */
        mockMvc.perform(post("/api/compras/" + id + "/anular")
                        .header("X-Usuario", "jose.orellana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));

        verify(productoClient).ajustarStock(eq(11L),
                eq(new AjusteStockRequest(3, "INCREMENTAR")));

        /* ---- 4) auditoria: cada fila dice QUIEN hizo la accion ---- */
        String historial = mockMvc.perform(get("/api/compras/" + id + "/auditoria")
                        .header("X-Usuario", "jose.orellana"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode auditoria = objectMapper.readTree(historial);
        assertEquals(3, auditoria.size(), "debe haber REGISTRADA, CONFIRMADA y ANULADA");

        JsonNode anulada = auditoria.get(0); // la mas reciente
        assertEquals("ANULADA", anulada.get("accion").asText());
        assertEquals("CONFIRMADA", anulada.get("estadoAnterior").asText());
        assertEquals("ANULADA", anulada.get("estadoNuevo").asText());
        assertEquals("jose.orellana", anulada.get("usuario").asText(),
                "la auditoria debe registrar QUIEN anulo la compra");
        assertTrue(anulada.get("detalle").asText().contains("stock devuelto"));

        JsonNode confirmada = auditoria.get(1);
        assertEquals("CONFIRMADA", confirmada.get("accion").asText());
        assertEquals("jose.orellana", confirmada.get("usuario").asText());

        JsonNode registrada = auditoria.get(2);
        assertEquals("REGISTRADA", registrada.get("accion").asText());
        assertEquals("jose.orellana", registrada.get("usuario").asText());
    }

    private ProductoClientDto productoDe(Long id, int stock) {
        return new ProductoClientDto(id, "PRD-200", "Aceite vegetal", "Bidon 1 L",
                new BigDecimal("12.50"), stock, 10, "Abarrotes", "ACTIVO", 1L, false,
                LocalDateTime.now(), LocalDateTime.now(), "sistema", "sistema");
    }
}
