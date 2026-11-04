package com.orellana.microservicios.ms_proveedor.services;

import java.util.List;

import com.orellana.microservicios.ms_proveedor.dto.EstadoRequest;
import com.orellana.microservicios.ms_proveedor.dto.MensajeResponse;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorRequest;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorResponse;

/**
 * Contrato del caso de uso de proveedores. El controlador depende
 * de esta interfaz, no de la implementación (principio D de SOLID).
 */
public interface ProveedorService {

    ProveedorResponse crear(ProveedorRequest request);

    List<ProveedorResponse> listar();

    ProveedorResponse obtenerPorId(Long id);

    ProveedorResponse obtenerPorRuc(String ruc);

    ProveedorResponse actualizar(Long id, ProveedorRequest request);

    ProveedorResponse cambiarEstado(Long id, EstadoRequest request);

    MensajeResponse eliminar(Long id);
}
