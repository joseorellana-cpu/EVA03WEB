package com.orellana.microservicios.ms_proveedor.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orellana.microservicios.ms_proveedor.dto.EstadoRequest;
import com.orellana.microservicios.ms_proveedor.dto.MensajeResponse;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorMapper;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorRequest;
import com.orellana.microservicios.ms_proveedor.dto.ProveedorResponse;
import com.orellana.microservicios.ms_proveedor.exception.ConflictoException;
import com.orellana.microservicios.ms_proveedor.exception.RecursoNoEncontradoException;
import com.orellana.microservicios.ms_proveedor.model.EstadoProveedor;
import com.orellana.microservicios.ms_proveedor.model.Proveedor;
import com.orellana.microservicios.ms_proveedor.repository.ProveedorRepository;
import com.orellana.microservicios.ms_proveedor.services.ProveedorService;

/**
 * Reglas de negocio de proveedores:
 *  - RUC único (409 al crear y al actualizar, excluyendo el propio id);
 *  - DELETE = baja lógica (estado INACTIVO), nunca borrado físico;
 *  - 404 si el proveedor no existe.
 */
@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository repository;

    public ProveedorServiceImpl(ProveedorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        if (repository.existsByRuc(request.ruc())) {
            throw new ConflictoException("Ya existe un proveedor con el RUC " + request.ruc());
        }
        Proveedor proveedor = ProveedorMapper.toEntity(request);
        return ProveedorMapper.toDto(repository.save(proveedor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return repository.findAll().stream()
                .map(ProveedorMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtenerPorId(Long id) {
        return ProveedorMapper.toDto(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtenerPorRuc(String ruc) {
        Proveedor proveedor = repository.findByRuc(ruc)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un proveedor con el RUC " + ruc));
        return ProveedorMapper.toDto(proveedor);
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscarPorId(id);
        if (repository.existsByRucAndIdNot(request.ruc(), id)) {
            throw new ConflictoException("El RUC " + request.ruc() + " ya pertenece a otro proveedor");
        }
        proveedor.setRuc(request.ruc());
        proveedor.setRazonSocial(request.razonSocial());
        proveedor.setNombreComercial(request.nombreComercial());
        proveedor.setEmail(request.email());
        proveedor.setTelefono(request.telefono());
        proveedor.setDireccion(request.direccion());
        return ProveedorMapper.toDto(repository.save(proveedor));
    }

    @Override
    @Transactional
    public ProveedorResponse cambiarEstado(Long id, EstadoRequest request) {
        Proveedor proveedor = buscarPorId(id);
        proveedor.setEstado(EstadoProveedor.valueOf(request.estado()));
        return ProveedorMapper.toDto(repository.save(proveedor));
    }

    @Override
    @Transactional
    public MensajeResponse eliminar(Long id) {
        Proveedor proveedor = buscarPorId(id);
        if (proveedor.getEstado() == EstadoProveedor.INACTIVO) {
            throw new ConflictoException("El proveedor " + id + " ya está inactivo");
        }
        proveedor.setEstado(EstadoProveedor.INACTIVO);
        repository.save(proveedor);
        return new MensajeResponse("Proveedor " + id + " inactivado (baja lógica)");
    }

    private Proveedor buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el proveedor con id " + id));
    }
}
