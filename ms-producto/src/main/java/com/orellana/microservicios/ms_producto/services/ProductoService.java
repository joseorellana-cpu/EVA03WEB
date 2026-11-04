package com.orellana.microservicios.ms_producto.services;

import java.util.List;

import com.orellana.microservicios.ms_producto.dto.AjusteStockRequest;
import com.orellana.microservicios.ms_producto.dto.EstadoRequest;
import com.orellana.microservicios.ms_producto.dto.MensajeResponse;
import com.orellana.microservicios.ms_producto.dto.MovimientoResponse;
import com.orellana.microservicios.ms_producto.dto.ProductoRequest;
import com.orellana.microservicios.ms_producto.dto.ProductoResponse;

/**
 * Contrato del caso de uso de productos. El controlador depende de
 * esta interfaz, no de la implementación (principio D de SOLID).
 */
public interface ProductoService {

    ProductoResponse crear(ProductoRequest request);

    List<ProductoResponse> listar();

    ProductoResponse obtenerPorId(Long id);

    List<ProductoResponse> listarPorProveedor(Long proveedorId);

    ProductoResponse actualizar(Long id, ProductoRequest request);

    ProductoResponse cambiarEstado(Long id, EstadoRequest request);

    MensajeResponse eliminar(Long id);

    /** PATCH /{id}/stock: ajuste transaccional con bloqueo pesimista. */
    ProductoResponse ajustarStock(Long id, AjusteStockRequest request, String motivo);

    /** GET /{id}/movimientos: historial de inventario. */
    List<MovimientoResponse> listarMovimientos(Long id);
}
