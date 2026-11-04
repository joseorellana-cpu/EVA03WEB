package com.orellana.microservicios.ms_proveedor.controller;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.orellana.microservicios.ms_proveedor.dto.EstadoRequest;
import com.orellana.microservicios.ms_proveedor.dto.MensajeResponse;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorRequest;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorResponse;
import com.orellana.microservicios.ms_proveedor.services.ProveedorService;

import jakarta.validation.Valid;

/**
 * Capa web de proveedores. Delgada: valida sintaxis (@Valid),
 * delega la lógica en ProveedorService y arma la respuesta HTTP.
 * Ruta /api/proveedores: coincide con la ruta del gateway SIN StripPrefix.
 */
@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProveedorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @GetMapping("/ruc/{ruc}")
    public ProveedorResponse obtenerPorRuc(@PathVariable String ruc) {
        return service.obtenerPorRuc(ruc);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse creado = service.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id,
                                        @Valid @RequestBody ProveedorRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ProveedorResponse cambiarEstado(@PathVariable Long id,
                                           @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    public MensajeResponse eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }
}
