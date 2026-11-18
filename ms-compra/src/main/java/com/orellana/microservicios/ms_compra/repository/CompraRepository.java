package com.orellana.microservicios.ms_compra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orellana.microservicios.ms_compra.model.Compra;

/**
 * Persistencia de compras. existsByNumeroCompra valida que el numero
 * escrito por el usuario no se repita.
 */
@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** GET /api/compras/proveedor/{proveedorId} */
    List<Compra> findByProveedorId(Long proveedorId);

    /** Unicidad del número de compra (409 si ya existe) */
    boolean existsByNumeroCompra(String numeroCompra);

}
