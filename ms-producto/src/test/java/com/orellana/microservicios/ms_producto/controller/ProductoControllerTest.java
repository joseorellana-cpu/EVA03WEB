package com.orellana.microservicios.ms_producto.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.data.domain.AuditorAware;

import com.orellana.microservicios.ms_producto.client.ProveedorClient;
import com.orellana.microservicios.ms_producto.client.ProveedorClientDto;

import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Test de controlador con MockMvc + cliente Feign simulado con
 * @MockitoBean (sin red): valida el surtido HTTP del contrato de
 * creación de producto contra el estado del proveedor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorClient proveedorClient;

    @MockitoBean
    private AuditorAware<String> auditorAware;

    @Test
    @DisplayName("POST con proveedor ACTIVO responde 201 y Location")
    void crearConProveedorActivoRetorna201() throws Exception {
        when(proveedorClient.obtenerProveedor(1L))
                .thenReturn(new ProveedorClientDto(1L, "20100047110", "INNOVA DISTRIBUCIONES S.A.C.",
                        "Innova Distribuciones", "ventas@innova.pe", "01-2345678",
                        "Av. Argentina 1450, Callao", "ACTIVO",
                        LocalDateTime.now(), "sistema", "sistema"));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test"));

        String cuerpo = """
                {
                  "codigo": "PRD-C100",
                  "nombre": "Producto de controlador test",
                  "precio": 10.50,
                  "stock": 30,
                  "stockMinimo": 5,
                  "categoria": "Abarrotes",
                  "proveedorId": 1
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .header("X-Usuario", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/productos/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.bajoStockMinimo").value(false))
                .andExpect(jsonPath("$.creadoPor").value("test"))
                .andExpect(jsonPath("$.proveedorId").value(1));
    }

    @Test
    @DisplayName("POST con proveedor INACTIVO responde 409 con formato global de error")
    void crearConProveedorInactivoRetorna409() throws Exception {
        when(proveedorClient.obtenerProveedor(3L))
                .thenReturn(new ProveedorClientDto(3L, "20551234567", "COMERCIAL PACIFICO SUR S.A.",
                        "Pacifico Sur", "ventas@pacificosur.pe", "01-1112223",
                        "Av. Espana 500, Lima", "INACTIVO",
                        LocalDateTime.now(), "sistema", "sistema"));

        String cuerpo = """
                {
                  "codigo": "PRD-C200",
                  "nombre": "Producto con proveedor inactivo",
                  "precio": 10.50,
                  "stock": 30,
                  "stockMinimo": 5,
                  "proveedorId": 3
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .header("X-Usuario", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INACTIVO")))
                .andExpect(jsonPath("$.path").value("/api/productos"));
    }

    @Test
    @DisplayName("POST con precio negativo responde 400 con details campo: motivo")
    void crearConPrecioNegativoRetorna400() throws Exception {
        String cuerpo = """
                {
                  "codigo": "PRD-C300",
                  "nombre": "Producto con precio negativo",
                  "precio": -5.00,
                  "stock": 30,
                  "stockMinimo": 5,
                  "proveedorId": 1
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(
                        org.hamcrest.Matchers.containsString("precio: el precio debe ser mayor que cero")))
                .andExpect(jsonPath("$.path").value("/api/productos"));
    }
}
