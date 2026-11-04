package com.orellana.microservicios.ms_producto.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import com.orellana.microservicios.ms_producto.client.ProveedorClient;
import com.orellana.microservicios.ms_producto.client.ProveedorClientDto;
import com.orellana.microservicios.ms_producto.dto.AjusteStockRequest;
import com.orellana.microservicios.ms_producto.dto.ProductoRequest;
import com.orellana.microservicios.ms_producto.exception.ConflictoException;
import com.orellana.microservicios.ms_producto.exception.ServicioNoDisponibleException;
import com.orellana.microservicios.ms_producto.exception.StockInsuficienteException;
import com.orellana.microservicios.ms_producto.model.OperacionStock;
import com.orellana.microservicios.ms_producto.model.Producto;
import com.orellana.microservicios.ms_producto.model.StockMovimiento;
import com.orellana.microservicios.ms_producto.repository.ProductoRepository;
import com.orellana.microservicios.ms_producto.repository.StockMovimientoRepository;

import feign.RetryableException;
import feign.Request;
import feign.Request.HttpMethod;

/**
 * Tests de servicio con Mockito (sin BD ni servidor): cubren las tres
 * reglas criticas del estado COMUN:
 *  1) stock insuficiente al DESCONTAR -> 409, sin guardar ni mover;
 *  2) proveedor INACTIVO -> 409, sin crear el producto;
 *  3) ms-proveedor caido (Retryable Feign) -> 503, sin crear el producto;
 *  4) ajuste valido deja un StockMovimiento coherente en la misma transaccion.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private StockMovimientoRepository movimientosRepository;

    @Mock
    private ProveedorClient proveedorClient;

    @Mock
    private AuditorAware<String> auditorAware;

    private ProductoServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new ProductoServiceImpl(
                productoRepository, movimientosRepository, proveedorClient, auditorAware);
    }

    @Test
    @DisplayName("DESCONTAR sin stock suficiente lanza 409 y no guarda ni registra movimiento")
    void stockInsuficienteAlDescontar() {
        Producto producto = productoDeInventario("PRD-100", 10, 3, 1L);
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));

        StockInsuficienteException excepcion = assertThrows(StockInsuficienteException.class,
                () -> service.ajustarStock(1L, new AjusteStockRequest(15, "DESCONTAR"), "prueba"));

        assertTrue(excepcion.getMessage().contains("disponible 10"));
        assertTrue(excepcion.getMessage().contains("solicitado 15"));
        verify(productoRepository, never()).save(any());
        verify(movimientosRepository, never()).save(any());
    }

    @Test
    @DisplayName("ajuste valido de stock guarda el producto y el movimiento coherente")
    void ajusteValidoRegistraMovimiento() {
        Producto producto = productoDeInventario("PRD-101", 10, 3, 1L);
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("jose.orellana"));

        var respuesta = service.ajustarStock(1L, new AjusteStockRequest(5, "DESCONTAR"), "venta manual");

        assertEquals(5, respuesta.stock());
        ArgumentCaptor<StockMovimiento> captor = ArgumentCaptor.forClass(StockMovimiento.class);
        verify(movimientosRepository).save(captor.capture());
        StockMovimiento movimiento = captor.getValue();
        assertEquals(OperacionStock.DESCONTAR, movimiento.getOperacion());
        assertEquals(5, movimiento.getCantidad());
        assertEquals(10, movimiento.getStockAnterior());
        assertEquals(5, movimiento.getStockNuevo());
        assertEquals("jose.orellana", movimiento.getUsuario());
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("crear con proveedor INACTIVO lanza 409 y no guarda el producto")
    void crearConProveedorInactivo() {
        ProveedorClientDto proveedor = new ProveedorClientDto(
                3L, "20551234567", "COMERCIAL PACIFICO SUR S.A.", null, null,
                null, null, "INACTIVO", LocalDateTime.now(), "sistema", "sistema");
        when(proveedorClient.obtenerProveedor(3L)).thenReturn(proveedor);

        ConflictoException excepcion = assertThrows(ConflictoException.class,
                () -> service.crear(requestDe("PRD-200", 3L)));

        assertTrue(excepcion.getMessage().contains("INACTIVO"));
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear con ms-proveedor caido lanza 503 y no guarda el producto")
    void crearConProveedorCaido() {
        Request peticion = Request.create(HttpMethod.GET,
                "http://ms-proveedor/api/proveedores/9",
                java.util.Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8);
        when(proveedorClient.obtenerProveedor(9L))
                .thenThrow(new RetryableException(-1, "Connection refused", HttpMethod.GET, (Long) null, peticion));

        var excepcion = assertThrows(ServicioNoDisponibleException.class,
                () -> service.crear(requestDe("PRD-300", 9L)));

        assertTrue(excepcion.getMessage().contains("MS-PROVEEDOR"));
        verify(productoRepository, never()).save(any());
    }

    private Producto productoDeInventario(String codigo, int stock, int stockMinimo, Long proveedorId) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setCodigo(codigo);
        producto.setNombre("Producto de prueba");
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStock(stock);
        producto.setStockMinimo(stockMinimo);
        producto.setEstado(com.orellana.microservicios.ms_producto.model.EstadoProducto.ACTIVO);
        producto.setProveedorId(proveedorId);
        return producto;
    }

    private ProductoRequest requestDe(String codigo, Long proveedorId) {
        return new ProductoRequest(codigo, "Producto de prueba", "desc",
                new BigDecimal("10.00"), 5, 3, "Abarrotes", proveedorId);
    }
}
