package com.orellana.microservicios.ms_proveedor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orellana.microservicios.ms_proveedor.model.Proveedor;

/**
 * Persistencia de proveedores. Derivación de consultas por nombre
 * de método: sin SQL manual para los casos simples.
 */
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** ¿Existe ya un proveedor con este RUC? (crear -> 409) */
    boolean existsByRuc(String ruc);

    /** Cargar por RUC para GET /api/proveedores/ruc/{ruc} */
    Optional<Proveedor> findByRuc(String ruc);

    /** ¿El RUC lo usa OTRO proveedor? (actualizar -> 409, excluye el propio id) */
    boolean existsByRucAndIdNot(String ruc, Long id);
}
