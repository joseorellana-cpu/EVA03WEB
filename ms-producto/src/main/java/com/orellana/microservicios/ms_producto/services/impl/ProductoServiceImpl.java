package com.orellana.microservicios.ms_producto.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orellana.microservicios.ms_producto.client.ProveedorClient;
import com.orellana.microservicios.ms_producto.client.ProveedorClientDto;
import com.orellana.microservicios.ms_producto.config.UsuarioAuditoriaAware;
import com.orellana.microservicios.ms_producto.dto.AjusteStockRequest;
import com.orellana.microservicios.ms_producto.dto.EstadoRequest;
import com.orellana.microservicios.ms_producto.dto.MensajeResponse;
import com.orellana.microservicios.ms_producto.dto.MovimientoMapper;
import com.orellana.microservicios.ms_producto.dto.MovimientoResponse;
import com.orellana.microservicios.ms_producto.dto.ProductoMapper;
import com.orellana.microservicios.ms_producto.dto.ProductoRequest;
import com.orellana.microservicios.ms_producto.dto.ProductoResponse;
import com.orellana.microservicios.ms_producto.exception.ConflictoException;
import com.orellana.microservicios.ms_producto.exception.RecursoNoEncontradoException;
import com.orellana.microservicios.ms_producto.exception.StockInsuficienteException;
import com.orellana.microservicios.ms_producto.exception.ServicioNoDisponibleException;
import com.orellana.microservicios.ms_producto.model.EstadoProducto;
import com.orellana.microservicios.ms_producto.model.OperacionStock;
import com.orellana.microservicios.ms_producto.model.Producto;
import com.orellana.microservicios.ms_producto.model.StockMovimiento;
import com.orellana.microservicios.ms_producto.repository.ProductoRepository;
import com.orellana.microservicios.ms_producto.repository.StockMovimientoRepository;
import com.orellana.microservicios.ms_producto.services.ProductoService;

import feign.FeignException;

/**
 * Reglas de negocio de productos:
 *  - código único (409);
 *  - al crear y actualizar se valida por Feign que el proveedor exista
 *    (404) y esté ACTIVO (409); si el servicio de proveedor está caído
 *    -> 503 y NO se persiste el producto;
 *  - DELETE = baja lógica;
 *  - ajustarStock: transaccional, con bloqueo pesimista de fila
 *    (findByIdForUpdate), registra aqui un StockMovimiento (stock
 *    anterior/nuevo, usuario, motivo) en la MISMA transacción;
 *    DESCONTAR sin stock suficiente -> 409 con stock disponible.
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repository;
    private final StockMovimientoRepository movimientosRepository;
    private final ProveedorClient proveedorClient;
    private final AuditorAware<String> auditorAware;

    public ProductoServiceImpl(ProductoRepository repository,
                               StockMovimientoRepository movimientosRepository,
                               ProveedorClient proveedorClient,
                               AuditorAware<String> auditorAware) {
        this.repository = repository;
        this.movimientosRepository = movimientosRepository;
        this.proveedorClient = proveedorClient;
        this.auditorAware = auditorAware;
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (repository.existsByCodigo(request.codigo())) {
            throw new ConflictoException("Ya existe un producto con el código " + request.codigo());
        }
        validarProveedorActivo(request.proveedorId());
        Producto producto = ProductoMapper.toEntity(request);
        return ProductoMapper.toDto(repository.save(producto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return repository.findAll().stream()
                .map(ProductoMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return ProductoMapper.toDto(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorProveedor(Long proveedorId) {
        return repository.findByProveedorId(proveedorId).stream()
                .map(ProductoMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarPorId(id);
        if (repository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflictoException("El código " + request.codigo() + " ya pertenece a otro producto");
        }
        validarProveedorActivo(request.proveedorId());
        producto.setCodigo(request.codigo());
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setStockMinimo(request.stockMinimo());
        producto.setCategoria(request.categoria());
        producto.setProveedorId(request.proveedorId());
        return ProductoMapper.toDto(repository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse cambiarEstado(Long id, EstadoRequest request) {
        Producto producto = buscarPorId(id);
        producto.setEstado(EstadoProducto.valueOf(request.estado()));
        return ProductoMapper.toDto(repository.save(producto));
    }

    @Override
    @Transactional
    public MensajeResponse eliminar(Long id) {
        Producto producto = buscarPorId(id);
        if (producto.getEstado() == EstadoProducto.INACTIVO) {
            throw new ConflictoException("El producto " + id + " ya está inactivo");
        }
        producto.setEstado(EstadoProducto.INACTIVO);
        repository.save(producto);
        return new MensajeResponse("Producto " + id + " inactivado (baja lógica)");
    }

    @Override
    @Transactional
    public ProductoResponse ajustarStock(Long id, AjusteStockRequest request, String motivo) {
        // Bloqueo pesimista de la fila: evita stock negativo por concurrencia
        Producto producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + id));

        OperacionStock operacion = OperacionStock.valueOf(request.operacion());
        int stockAnterior = producto.getStock();
        int stockNuevo;

        if (operacion == OperacionStock.DESCONTAR) {
            stockNuevo = stockAnterior - request.cantidad();
            if (stockNuevo < 0) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para el producto " + producto.getCodigo()
                                + ": disponible " + stockAnterior + ", solicitado " + request.cantidad());
            }
        } else {
            stockNuevo = stockAnterior + request.cantidad();
        }

        producto.setStock(stockNuevo);
        repository.save(producto);

        // Movimiento de inventario en la MISMA transacción del ajuste
        StockMovimiento movimiento = new StockMovimiento();
        movimiento.setProductoId(producto.getId());
        movimiento.setOperacion(operacion);
        movimiento.setCantidad(request.cantidad());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setUsuario(auditorAware.getCurrentAuditor().orElse(UsuarioAuditoriaAware.USUARIO_DEFECTO));
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setMotivo(motivo != null ? motivo : "Ajuste de stock manual");
        movimientosRepository.save(movimiento);

        return ProductoMapper.toDto(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponse> listarMovimientos(Long id) {
        buscarPorId(id); // 404 si el producto no existe
        return movimientosRepository.findByProductoIdOrderByIdDesc(id).stream()
                .map(MovimientoMapper::toDto)
                .toList();
    }

    /**
     * Valida via Feign que el proveedor exista (404) y esté ACTIVO (409).
     * Si no responde (caído / timeout) -> 503 y el producto NO se guarda.
     */
    private void validarProveedorActivo(Long proveedorId) {
        ProveedorClientDto proveedor;
        try {
            proveedor = proveedorClient.obtenerProveedor(proveedorId);
        } catch (RecursoNoEncontradoException | ConflictoException | ServicioNoDisponibleException excepcion) {
            throw excepcion; // ya traducidas por el ErrorDecoder
        } catch (FeignException fallo) {
            // servicio caído, timeout u otro HTTP no convertido: -> 503
            throw new ServicioNoDisponibleException(
                    "El servicio MS-PROVEEDOR no está disponible en este momento. "
                            + "No se puede validar al proveedor. Intente nuevamente.");
        }
        if (!"ACTIVO".equalsIgnoreCase(proveedor.estado())) {
            throw new ConflictoException(
                    "El proveedor " + proveedorId + " está INACTIVO: no se puede operar con él");
        }
    }

    private Producto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + id));
    }
}
