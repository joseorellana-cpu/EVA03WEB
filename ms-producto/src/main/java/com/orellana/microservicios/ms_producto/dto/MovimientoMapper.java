package com.orellana.microservicios.ms_producto.dto;

import com.orellana.microservicios.ms_producto.model.StockMovimiento;

/**
 * Mapper manual movimiento <-> DTO.
 */
public final class MovimientoMapper {

    private MovimientoMapper() {
        // clase utilitaria: no se instancia
    }

    public static MovimientoResponse toDto(StockMovimiento movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getProductoId(),
                movimiento.getOperacion() != null ? movimiento.getOperacion().name() : null,
                movimiento.getCantidad(),
                movimiento.getStockAnterior(),
                movimiento.getStockNuevo(),
                movimiento.getUsuario(),
                movimiento.getFecha(),
                movimiento.getMotivo()
        );
    }
}
