package com.orellana.microservicios.ms_producto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orellana.microservicios.ms_producto.model.Producto;

/**
 * Persistencia de productos. findByIdForUpdate usa bloqueo pesimista
 * (SELECT ... FOR UPDATE en MySQL) para que dos confirmaciones de
 * compra simultáneas no dejen el stock en un estado inválido (regla:
 * nunca stock negativo por concurrencia).
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** ¿Existe ya un producto con este código? (crear -> 409) */
    boolean existsByCodigo(String codigo);

    /** ¿El código lo usa OTRO producto? (actualizar -> 409) */
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /** Productos de un proveedor para GET /api/productos/proveedor/{proveedorId} */
    java.util.List<Producto> findByProveedorId(Long proveedorId);

    /**
     * Carga con candado de fila para el ajuste de stock transaccional.
     * Se usa SQL NATIVO "FOR UPDATE" (sin la clausula "OF") porque MariaDB
     * no soporta "FOR UPDATE OF alias" (error 1064) y el JPQL con
     * PESSIMISTIC_WRITE lo generaba; el SELECT nativo bloquea la fila igual
     * en MySQL y MariaDB.
     */
    @Query(value = "SELECT * FROM productos WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
