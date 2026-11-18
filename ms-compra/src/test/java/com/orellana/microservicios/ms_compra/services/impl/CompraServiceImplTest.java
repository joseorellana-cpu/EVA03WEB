package com.orellana.microservicios.ms_compra.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orellana.microservicios.ms_compra.client.AjusteStockRequest;
import com.orellana.microservicios.ms_compra.client.ProductoClient;
import com.orellana.microservicios.ms_compra.client.ProductoClientDto;
import com.orellana.microservicios.ms_compra.client.ProveedorClient;
import com.orellana.microservicios.ms_compra.client.ProveedorClientDto;
import com.orellana.microservicios.ms_compra.dto.CompraRequest;
import com.orellana.microservicios.ms_compra.dto.DetalleRequest;
import com.orellana.microservicios.ms_compra.exception.ConflictoException;
import com.orellana.microservicios.ms_compra.exception.ProveedorInactivoException;
import com.orellana.microservicios.ms_compra.exception.ServicioNoDisponibleException;
import com.orellana.microservicios.ms_compra.exception.StockInsuficienteException;
import com.orellana.microservicios.ms_compra.model.AccionCompra;
import com.orellana.microservicios.ms_compra.model.Compra;
import com.orellana.microservicios.ms_compra.model.CompraAuditoria;
import com.orellana.microservicios.ms_compra.model.CompraDetalle;
import com.orellana.microservicios.ms_compra.model.EstadoCompra;
import com.orellana.microservicios.ms_compra.repository.CompraAuditoriaRepository;
import com.orellana.microservicios.ms_compra.repository.CompraRepository;

import feign.Request;
import feign.Request.HttpMethod;
import feign.RetryableException;

/**
 * Tests de servicio con Mockito (sin BD ni servidor): cubren las reglas
 * mas delicadas del proceso de compra:
 *  1) COMPENSACION al confirmar: si el segundo producto no alcanza stock,
 *     el primero ya descontado en ms-producto se DEVUELVE con la operacion
 *     INVERSA (INCREMENTAR) y la compra NO cambia de estado;
 *  2) proveedor INACTIVO -> 409 sin crear la compra;
 *  3) productos repetidos en la compra -> 409 sin llamar a ms-producto;
 *  4) ms-producto caido al confirmar -> 503;
 *  5) registrar NO descuenta stock y escribe la auditoria en la misma tx.
 */
@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private CompraAuditoriaRepository compraAuditoriaRepository;

    @Mock
    private ProveedorClient proveedorClient;

    @Mock
    private ProductoClient productoClient;

    private CompraServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new CompraServiceImpl(
                compraRepository, compraAuditoriaRepository, proveedorClient, productoClient);
    }

    @Test
    @DisplayName("confirmar: si el 2do producto no tiene stock, el 1ro YA descontado se compensa con INCREMENTAR")
    void confirmarConCompensacion() {
        Compra compra = compraConDetalles();
        when(compraRepository.findById(7L)).thenReturn(Optional.of(compra));
        when(productoClient.obtenerProducto(10L)).thenReturn(productoDe(10L, 50));
        when(productoClient.obtenerProducto(20L)).thenReturn(productoDe(20L, 25));
        when(productoClient.ajustarStock(eq(10L), eq(new AjusteStockRequest(3, "DESCONTAR"))))
                .thenReturn(productoDe(10L, 47));
        // el decoder de Feign traduce el 409 remoto en esta excepcion de dominio
        when(productoClient.ajustarStock(eq(20L), any()))
                .thenThrow(new StockInsuficienteException(
                        "Stock insuficiente para el producto Legumbres Andinas (stock disponible 25, solicitado 28)"));

        StockInsuficienteException excepcion = assertThrows(StockInsuficienteException.class,
                () -> service.confirmar(7L));

        assertTrue(excepcion.getMessage().contains("Stock insuficiente"));
        // COMPENSACION: se devuelve lo ya descontado del producto 10
        verify(productoClient).ajustarStock(eq(10L), eq(new AjusteStockRequest(3, "INCREMENTAR")));
        // el rollback local no deshizo la llamada remota: la compramos manualmente
        // la compra queda REGISTRADA (no se confirma ni se anula)
        verify(compraRepository, never()).save(any());
        verify(compraAuditoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar con proveedor INACTIVO lanza 409 y no crea la compra")
    void registrarConProveedorInactivo() {
        ProveedorClientDto proveedor = new ProveedorClientDto(
                2L, "20487654321", "DISTRIBUIDORA LOS ANDES E.I.R.L.", null, null, null,
                null, "INACTIVO", LocalDateTime.now(), "sistema", "sistema");
        when(proveedorClient.obtenerProveedor(2L)).thenReturn(proveedor);

        ProveedorInactivoException excepcion = assertThrows(ProveedorInactivoException.class,
                () -> service.registrar(peticionDe(2L, new BigDecimal("15.50"), 3)));

        assertTrue(excepcion.getMessage().contains("no esta ACTIVO"));
        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar con productos repetidos lanza 409 sin crear la compra")
    void registrarConProductosRepetidos() {
        when(proveedorClient.obtenerProveedor(1L)).thenReturn(proveedorActivo());
        when(productoClient.obtenerProducto(10L)).thenReturn(productoDe(10L, 50));

        var peticion = new CompraRequest("C-2026-000001", 1L, null,
                List.of(new DetalleRequest(10L, 3, new BigDecimal("15.50")),
                        new DetalleRequest(10L, 5, new BigDecimal("15.50"))));

        ConflictoException excepcion = assertThrows(ConflictoException.class,
                () -> service.registrar(peticion));

        assertTrue(excepcion.getMessage().contains("repetido"));
        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmar con ms-producto caido lanza 503 y no compensa ni confirma")
    void confirmarConProductoCaido() {
        Compra compra = compraConDetalles();
        when(compraRepository.findById(7L)).thenReturn(Optional.of(compra));
        when(productoClient.obtenerProducto(10L))
                .thenThrow(excepcionDeConexion());

        ServicioNoDisponibleException excepcion = assertThrows(ServicioNoDisponibleException.class,
                () -> service.confirmar(7L));

        assertTrue(excepcion.getMessage().contains("MS-PRODUCTO"));
        verify(productoClient, never()).ajustarStock(any(), any());
        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar valido NO descuenta stock, usa el precio enviado por el cliente y audita en la misma tx")
    void registrarValidoNoDescuentaYAudita() {
        when(proveedorClient.obtenerProveedor(1L)).thenReturn(proveedorActivo());
        when(productoClient.obtenerProducto(10L)).thenReturn(productoDe(10L, 50));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocacion -> {
            Compra guardada = invocacion.getArgument(0);
            guardada.setId(7L);
            return guardada;
        });

        var respuesta = service.registrar(peticionDe(1L, new BigDecimal("15.50"), 3));

        assertEquals(EstadoCompra.REGISTRADA.name(), respuesta.estado());
        assertEquals("C-2026-000001", respuesta.numeroCompra());
        assertEquals(new BigDecimal("46.50"), respuesta.total());
        assertEquals(new BigDecimal("15.50"), respuesta.detalles().get(0).precioUnitario());
        // la compra recien creada NO toca el stock de ms-producto
        verify(productoClient, never()).ajustarStock(any(), any());
        // auditoria en la MISMA transaccion
        ArgumentCaptor<CompraAuditoria> captor = ArgumentCaptor.forClass(CompraAuditoria.class);
        verify(compraAuditoriaRepository).save(captor.capture());
        assertEquals(AccionCompra.REGISTRADA, captor.getValue().getAccion());
        assertEquals(EstadoCompra.REGISTRADA.name(), captor.getValue().getEstadoNuevo());
    }

    @Test
    @DisplayName("registrar con un numero de compra ya usado lanza 409 sin crear la compra")
    void registrarConNumeroRepetido() {
        when(compraRepository.existsByNumeroCompra("C-2026-000001")).thenReturn(true);

        var peticion = new CompraRequest(" c-2026-000001 ", 1L, null,
                List.of(new DetalleRequest(10L, 3, new BigDecimal("15.50"))));

        ConflictoException excepcion = assertThrows(ConflictoException.class,
                () -> service.registrar(peticion));

        assertTrue(excepcion.getMessage().contains("ya existe"));
        verify(compraRepository, never()).save(any());
    }

    /* ==================== datos de prueba ==================== */

    private CompraRequest peticionDe(Long proveedorId, BigDecimal precio, int cantidad) {
        return new CompraRequest("C-2026-000001", proveedorId, "Compra de prueba",
                List.of(new DetalleRequest(10L, cantidad, precio)));
    }

    private Compra compraConDetalles() {
        Compra compra = new Compra();
        compra.setId(7L);
        compra.setNumeroCompra("C-2026-000001");
        compra.setProveedorId(1L);
        compra.setFecha(LocalDateTime.now());
        compra.setEstado(EstadoCompra.REGISTRADA);

        CompraDetalle d1 = new CompraDetalle();
        d1.setProductoId(10L);
        d1.setCantidad(3);
        d1.setPrecioUnitario(new BigDecimal("15.50"));
        d1.setSubtotal(new BigDecimal("46.50"));
        compra.agregarDetalle(d1);

        CompraDetalle d2 = new CompraDetalle();
        d2.setProductoId(20L);
        d2.setCantidad(2);
        d2.setPrecioUnitario(new BigDecimal("6.00"));
        d2.setSubtotal(new BigDecimal("12.00"));
        compra.agregarDetalle(d2);
        compra.setTotal(new BigDecimal("58.50"));
        return compra;
    }

    private ProveedorClientDto proveedorActivo() {
        return new ProveedorClientDto(1L, "20100047110", "INNOVA DISTRIBUCIONES S.A.C.",
                "Innova Distribuciones", "ventas@innova.pe", "01-2345678",
                "Av. Argentina 1450, Callao", "ACTIVO",
                LocalDateTime.now(), "sistema", "sistema");
    }

    private ProductoClientDto productoDe(Long id, int stock) {
        return new ProductoClientDto(id, "PRD-100", "Arroz superior", "Bolsa 5 kg",
                new BigDecimal("15.50"), stock, 5, "Abarrotes", "ACTIVO", 1L, false,
                LocalDateTime.now(), LocalDateTime.now(), "sistema", "sistema");
    }

    private RetryableException excepcionDeConexion() {
        Request peticion = Request.create(HttpMethod.GET, "http://ms-producto/api/productos/10",
                java.util.Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8);
        return new RetryableException(-1, "Connection refused", HttpMethod.GET, (Long) null, peticion);
    }
}
