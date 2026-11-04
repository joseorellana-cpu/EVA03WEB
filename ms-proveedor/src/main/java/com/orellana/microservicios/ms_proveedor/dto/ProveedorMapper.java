package com.orellana.microservicios.ms_proveedor.dto;

import com.orellana.microservicios.ms_proveedor.model.EstadoProveedor;
import com.orellana.microservicios.ms_proveedor.model.Proveedor;

/**
 * Mapper manual (sin MapStruct: no es parte del stack del laboratorio).
 * Único punto donde se traduce entidad <-> DTO.
 */
public final class ProveedorMapper {

    private ProveedorMapper() {
        // clase utilitaria: no se instancia
    }

    public static Proveedor toEntity(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        proveedor.setRuc(request.ruc());
        proveedor.setRazonSocial(request.razonSocial());
        proveedor.setNombreComercial(request.nombreComercial());
        proveedor.setEmail(request.email());
        proveedor.setTelefono(request.telefono());
        proveedor.setDireccion(request.direccion());
        proveedor.setEstado(EstadoProveedor.ACTIVO);
        return proveedor;
    }

    public static ProveedorResponse toDto(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getRuc(),
                proveedor.getRazonSocial(),
                proveedor.getNombreComercial(),
                proveedor.getEmail(),
                proveedor.getTelefono(),
                proveedor.getDireccion(),
                proveedor.getEstado() != null ? proveedor.getEstado().name() : null,
                proveedor.getFechaCreacion(),
                proveedor.getCreadoPor(),
                proveedor.getModificadoPor()
        );
    }
}
