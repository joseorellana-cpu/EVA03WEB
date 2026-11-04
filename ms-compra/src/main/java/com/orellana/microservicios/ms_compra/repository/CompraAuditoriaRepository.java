package com.orellana.microservicios.ms_compra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orellana.microservicios.ms_compra.model.CompraAuditoria;

/**
 * Historial de compras (tabla compra_auditoria). deleteByCompraId se
 * usa SOLO al eliminar una compra ANULADA: se borra primero el
 * historial para respetar la FK interna compra_auditoria ->
 * compras(id) y luego la compra (los detalles se borran por cascade).
 */
@Repository
public interface CompraAuditoriaRepository extends JpaRepository<CompraAuditoria, Long> {

    /** GET /{id}/auditoria, del más reciente al más antiguo */
    List<CompraAuditoria> findByCompraIdOrderByFechaDesc(Long compraId);

    /** Limpieza previa al borrado físico de una compra ANULADA */
    void deleteByCompraId(Long compraId);
}
