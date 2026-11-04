package com.orellana.microservicios.ms_producto.dto;

import com.orellana.microservicios.ms_producto.model.EstadoProducto;
import com.orellana.microservicios.ms_producto.model.Producto;

/**
 * Mapper manual producto <-> DTO (sin MapStruct por decisión del stack).
 */
public final class ProductoMapper {

    private ProductoMapper() {
        // clase utilitaria: no se instancia
    }

    public static Producto toEntity(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setCodigo(request.codigo());
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setStockMinimo(request.stockMinimo());
        producto.setCategoria(request.categoria());
        producto.setEstado(EstadoProducto.ACTIVO);
        producto.setProveedorId(request.proveedorId());
        return producto;
    }

    public static ProductoResponse toDto(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getStockMinimo(),
                producto.getCategoria(),
                producto.getEstado() != null ? producto.getEstado().name() : null,
                producto.getProveedorId(),
                esBajoStockMinimo(producto),
                producto.getFechaCreacion(),
                producto.getFechaModificacion(),
                producto.getCreadoPor(),
                producto.getModificadoPor()
        );
    }

    /** Regla de negocio de solo lectura: stock <= stockMinimo. */
    public static boolean esBajoStockMinimo(Producto producto) {
        return producto.getStock() != null
                && producto.getStockMinimo() != null
                && producto.getStock() <= producto.getStockMinimo();
    }
}
