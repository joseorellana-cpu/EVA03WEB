package com.orellana.microservicios.ms_compra.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orellana.microservicios.ms_compra.dto.AuditoriaResponse;
import com.orellana.microservicios.ms_compra.dto.CompraRequest;
import com.orellana.microservicios.ms_compra.dto.CompraResponse;
import com.orellana.microservicios.ms_compra.services.CompraService;

import jakarta.validation.Valid;

/**
 * API REST de compras. Los 9 endpoints EXACTOS del laboratorio.
 * Header "X-Usuario": identifica al usuario para la auditoria
 * (fallback "sistema"); el servicio lo usa y el interceptor Feign
 * lo propaga hacia ms-proveedor y ms-producto.
 */
@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    /** 1) GET /api/compras — listar todas. */
    @GetMapping
    public ResponseEntity<List<CompraResponse>> listar() {
        return ResponseEntity.ok(compraService.listar());
    }

    /** 2) GET /api/compras/{id} — obtener por id. */
    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerPorId(id));
    }

    /** 3) GET /api/compras/proveedor/{proveedorId} — compras de un proveedor. */
    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<CompraResponse>> listarPorProveedor(@PathVariable Long proveedorId) {
        return ResponseEntity.ok(compraService.listarPorProveedor(proveedorId));
    }

    /** 4) POST /api/compras — registrar (NO descuenta stock) -> 201. */
    @PostMapping
    public ResponseEntity<CompraResponse> registrar(
            @Valid @RequestBody CompraRequest request,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrar(request));
    }

    /** 5) PUT /api/compras/{id} — modificar solo si REGISTRADA. */
    @PutMapping("/{id}")
    public ResponseEntity<CompraResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CompraRequest request,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        return ResponseEntity.ok(compraService.actualizar(id, request));
    }

    /** 6) POST /api/compras/{id}/confirmar — descuenta stock (DECISION J). */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<CompraResponse> confirmar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        return ResponseEntity.ok(compraService.confirmar(id));
    }

    /** 7) POST /api/compras/{id}/anular — devuelve stock si estaba CONFIRMADA. */
    @PostMapping("/{id}/anular")
    public ResponseEntity<CompraResponse> anular(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        return ResponseEntity.ok(compraService.anular(id));
    }

    /** 8) DELETE /api/compras/{id} — solo si ANULADA -> 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        compraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** 9) GET /api/compras/{id}/auditoria — quién hizo cada acción. */
    @GetMapping("/{id}/auditoria")
    public ResponseEntity<List<AuditoriaResponse>> auditoria(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.auditoria(id));
    }
}
