package com.orellana.microservicios.ms_proveedor.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orellana.microservicios.ms_proveedor.dto.ProveedorRequest;
import com.orellana.microservicios.ms_proveedor.exception.ConflictoException;
import com.orellana.microservicios.ms_proveedor.model.Proveedor;
import com.orellana.microservicios.ms_proveedor.repository.ProveedorRepository;

/**
 * Test de servicio con Mockito: prueba SOLO la regla de negocio del
 * RUC único, sin base de datos ni servidor web.
 */
@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    @Mock
    private ProveedorRepository repository;

    private ProveedorServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new ProveedorServiceImpl(repository);
    }

    @Test
    @DisplayName("crear con RUC duplicado lanza ConflictoException y no guarda")
    void crearConRucDuplicadoLanzaConflicto() {
        ProveedorRequest request = new ProveedorRequest(
                "20100047110", "DISTRIBUIDORA DUPLICADA S.A.", null,
                "ventas@duplicada.pe", "999888777", "Av. Prueba 123");
        when(repository.existsByRuc("20100047110")).thenReturn(true);

        ConflictoException excepcion = assertThrows(ConflictoException.class,
                () -> service.crear(request));

        assertTrue(excepcion.getMessage().contains("RUC"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("crear con RUC único guarda y devuelve el DTO con estado ACTIVO")
    void crearConRucUnicoGuardaYDevuelveDto() {
        ProveedorRequest request = new ProveedorRequest(
                "20678901234", "PROVEEDOR NUEVO S.A.C.", "Proveedor Nuevo",
                "contacto@nuevo.pe", "013334444", "Av. Nueva 456");
        when(repository.existsByRuc("20678901234")).thenReturn(false);
        when(repository.save(any(Proveedor.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = service.crear(request);

        assertEquals("20678901234", respuesta.ruc());
        assertEquals("ACTIVO", respuesta.estado());
        verify(repository).save(any(Proveedor.class));
    }
}
