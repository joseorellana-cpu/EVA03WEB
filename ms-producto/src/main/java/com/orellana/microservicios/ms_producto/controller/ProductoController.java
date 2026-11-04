package com.orellana.microservicios.ms_producto.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.orellana.microservicios.ms_producto.dto.AjusteStockRequest;
import com.orellana.microservicios.ms_producto.dto.EstadoRequest;
import com.orellana.microservicios.ms_producto.dto.MensajeResponse;
import com.orellana.microservicios.ms_producto.dto.MovimientoResponse;
import com.orellana.microservicios.ms_producto.dto.ProductoRequest;
import com.orellana.microservicios.ms_producto.dto.ProductoResponse;
import com.orellana.microservicios.ms_producto.services.ProductoService;

import jakarta.validation.Valid;

/**
 * Capa web de productos (9 endpoints). Delgada: valida (@Valid),
 * delega en ProductoService y arma la respuesta HTTP.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @GetMapping("/proveedor/{proveedorId}")
    public List<ProductoResponse> listarPorProveedor(@PathVariable Long proveedorId) {
        return service.listarPorProveedor(proveedorId);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = service.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ProductoResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    public MensajeResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    /** Contrato usado por ms-compra para descontar/incrementar inventario. */
    @PatchMapping("/{id}/stock")
    public ProductoResponse ajustarStock(@PathVariable Long id,
                                         @Valid @RequestBody AjusteStockRequest request,
                                         @RequestParam(required = false) String motivo) {
        return service.ajustarStock(id, request, motivo);
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoResponse> listarMovimientos(@PathVariable Long id) {
        return service.listarMovimientos(id);
    }
}
