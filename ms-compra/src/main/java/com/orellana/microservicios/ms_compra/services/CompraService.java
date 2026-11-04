package com.orellana.microservicios.ms_compra.services;

import java.util.List;

import com.orellana.microservicios.ms_compra.dto.AuditoriaResponse;
import com.orellana.microservicios.ms_compra.dto.CompraRequest;
import com.orellana.microservicios.ms_compra.dto.CompraResponse;

/**
 * Casos de uso del proceso de compra (BD propia bd_compra).
 * Toda modificacion es transaccional y escribe su auditoria en la
 * MISMA transaccion del cambio de estado.
 */
public interface CompraService {

    /** POST /api/compras: crea REGISTRADA, NO descuenta stock. */
    CompraResponse registrar(CompraRequest request);

    /** PUT /api/compras/{id}: solo si REGISTRADA; revalida y recalcula total. */
    CompraResponse actualizar(Long id, CompraRequest request);

    /** POST /api/compras/{id}/confirmar: descuenta stock con compensacion. */
    CompraResponse confirmar(Long id);

    /** POST /api/compras/{id}/anular: si CONFIRMADA devuelve el stock. */
    CompraResponse anular(Long id);

    /** DELETE /api/compras/{id}: solo si ANULADA (borrado fisico). */
    void eliminar(Long id);

    /** GET /api/compras/{id} */
    CompraResponse obtenerPorId(Long id);

    /** GET /api/compras */
    List<CompraResponse> listar();

    /** GET /api/compras/proveedor/{proveedorId} */
    List<CompraResponse> listarPorProveedor(Long proveedorId);

    /** GET /api/compras/{id}/auditoria: historial de estados y usuarios. */
    List<AuditoriaResponse> auditoria(Long id);
}
