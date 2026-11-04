package com.orellana.microservicios.ms_compra.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia ms-proveedor. Principio I: una sola operación.
 */
@FeignClient(name = "MS-PROVEEDOR")
public interface ProveedorClient {

    /** GET /api/proveedores/{id} -> ProveedorResponse */
    @GetMapping("/api/proveedores/{id}")
    ProveedorClientDto obtenerProveedor(@PathVariable("id") Long id);
}
