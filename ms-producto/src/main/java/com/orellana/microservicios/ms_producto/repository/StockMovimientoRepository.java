package com.orellana.microservicios.ms_producto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orellana.microservicios.ms_producto.model.StockMovimiento;

/**
 * Historial de inventario: leer los movimientos del producto,
 * más recientes primero (orden por id descendente).
 */
@Repository
public interface StockMovimientoRepository extends JpaRepository<StockMovimiento, Long> {

    /** GET /api/productos/{id}/movimientos */
    List<StockMovimiento> findByProductoIdOrderByIdDesc(Long productoId);
}
