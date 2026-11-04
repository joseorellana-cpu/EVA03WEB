package com.orellana.microservicios.ms_producto.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia ms-proveedor. Principio I de SOLID: una sola
 * operación (obtener por id), nada de métodos innecesarios.
 * La URL se resuelve por nombre de servicio en Eureka (lb), nunca hardcoded.
 * Errores: 404/409 los traduce FeignErrorDecoder; caída/timeout los
 * maneja el service convirtiendo la falla en ServicioNoDisponibleException.
 */
@FeignClient(name = "MS-PROVEEDOR")
public interface ProveedorClient {

    /** Contrato consumido: GET /api/proveedores/{id} -> ProveedorResponse */
    @GetMapping("/api/proveedores/{id}")
    ProveedorClientDto obtenerProveedor(@PathVariable("id") Long id);
}
