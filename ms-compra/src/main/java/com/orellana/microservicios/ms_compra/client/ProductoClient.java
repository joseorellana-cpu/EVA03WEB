package com.orellana.microservicios.ms_compra.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign hacia ms-producto. Principio I: solo las 2 operaciones
 * que el proceso de compra necesita (leer producto y ajustar stock).
 */
@FeignClient(name = "MS-PRODUCTO")
public interface ProductoClient {

    /** GET /api/productos/{id} -> ProductoResponse */
    @GetMapping("/api/productos/{id}")
    ProductoClientDto obtenerProducto(@PathVariable("id") Long id);

    /** PATCH /api/productos/{id}/stock -> ProductoResponse (contrato AjusteStockRequest) */
    @PatchMapping("/api/productos/{id}/stock")
    ProductoClientDto ajustarStock(@PathVariable("id") Long id,
                                   @RequestBody AjusteStockRequest request);
}
