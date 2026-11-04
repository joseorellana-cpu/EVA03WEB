package com.orellana.microservicios.ms_compra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orellana.microservicios.ms_compra.model.Compra;

/**
 * Persistencia de compras. countByNumeroCompraStartingWith alimenta
 * NumeroCompraGenerator (C-AAAA-NNNNNN).
 */
@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** GET /api/compras/proveedor/{proveedorId} */
    List<Compra> findByProveedorId(Long proveedorId);

    /** Respaldos del generador de números */
    boolean existsByNumeroCompra(String numeroCompra);

    long countByNumeroCompraStartingWith(String prefijo);
}
