package com.orellana.microservicios.ms_compra.dto;

import com.orellana.microservicios.ms_compra.model.Compra;
import com.orellana.microservicios.ms_compra.model.CompraAuditoria;
import com.orellana.microservicios.ms_compra.model.CompraDetalle;

/**
 * Mapper manual compra <-> DTO (sin MapStruct por decisión del stack).
 */
public final class CompraMapper {

    private CompraMapper() {
        // clase utilitaria: no se instancia
    }

    public static CompraResponse toDto(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getNumeroCompra(),
                compra.getProveedorId(),
                compra.getFecha(),
                compra.getEstado() != null ? compra.getEstado().name() : null,
                compra.getTotal(),
                compra.getObservaciones(),
                compra.getCreadoPor(),
                compra.getModificadoPor(),
                compra.getDetalles().stream()
                        .map(CompraMapper::detalleToDto)
                        .toList());
    }

    public static DetalleResponse detalleToDto(CompraDetalle detalle) {
        return new DetalleResponse(
                detalle.getId(),
                detalle.getProductoId(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }

    public static AuditoriaResponse auditoriaToDto(CompraAuditoria auditoria) {
        return new AuditoriaResponse(
                auditoria.getId(),
                auditoria.getCompraId(),
                auditoria.getAccion() != null ? auditoria.getAccion().name() : null,
                auditoria.getEstadoAnterior(),
                auditoria.getEstadoNuevo(),
                auditoria.getUsuario(),
                auditoria.getFecha(),
                auditoria.getDetalle());
    }
}
