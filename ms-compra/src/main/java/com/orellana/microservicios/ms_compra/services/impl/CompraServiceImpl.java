package com.orellana.microservicios.ms_compra.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.orellana.microservicios.ms_compra.client.AjusteStockRequest;
import com.orellana.microservicios.ms_compra.client.ProductoClient;
import com.orellana.microservicios.ms_compra.client.ProductoClientDto;
import com.orellana.microservicios.ms_compra.client.ProveedorClient;
import com.orellana.microservicios.ms_compra.client.ProveedorClientDto;
import com.orellana.microservicios.ms_compra.config.UsuarioAuditoriaAware;
import com.orellana.microservicios.ms_compra.dto.AuditoriaResponse;
import com.orellana.microservicios.ms_compra.dto.CompraMapper;
import com.orellana.microservicios.ms_compra.dto.CompraRequest;
import com.orellana.microservicios.ms_compra.dto.CompraResponse;
import com.orellana.microservicios.ms_compra.exception.CompraNoModificableException;
import com.orellana.microservicios.ms_compra.exception.ConflictoException;
import com.orellana.microservicios.ms_compra.exception.ExcepcionNegocio;
import com.orellana.microservicios.ms_compra.exception.ProveedorInactivoException;
import com.orellana.microservicios.ms_compra.exception.RecursoNoEncontradoException;
import com.orellana.microservicios.ms_compra.exception.ServicioNoDisponibleException;
import com.orellana.microservicios.ms_compra.exception.StockInsuficienteException;
import com.orellana.microservicios.ms_compra.model.AccionCompra;
import com.orellana.microservicios.ms_compra.model.Compra;
import com.orellana.microservicios.ms_compra.model.CompraAuditoria;
import com.orellana.microservicios.ms_compra.model.CompraDetalle;
import com.orellana.microservicios.ms_compra.model.EstadoCompra;
import com.orellana.microservicios.ms_compra.model.OperacionStock;
import com.orellana.microservicios.ms_compra.repository.CompraAuditoriaRepository;
import com.orellana.microservicios.ms_compra.repository.CompraRepository;
import com.orellana.microservicios.ms_compra.services.CompraService;

import feign.FeignException;

/**
 * Implementacion del proceso de compra.
 *
 * DECISION J ( ciclo de vida ): una compra se crea REGISTRADA (valida todo,
 * pero NO descuenta stock); /confirmar aplica la OPERACION_AL_CONFIRMAR
 * (DESCONTAR) en ms-producto; /anular devuelve el stock solo si estaba
 * CONFIRMADA.
 *
 * CLAVE DEL NEGOCIO: el rollback local NO deshace llamadas remotas. Si al
 * descontar el segundo producto el tercero falla (stock insuficiente), la
 * transaccion de este microservicio podria revertir sus propios cambios,
 * pero el stock YA descontado en ms-producto sigue descontado. Por eso
 * confirmar aplica la COMPENSACION: si alguna llamada remota falla, se
 * vuelve a llamar a ms-producto con la operacion INVERSA
 * (OPERACION_AL_CONFIRMAR.inversa() = INCREMENTAR) para dejar el stock
 * como estaba, y se responde 409.
 */
@Service
public class CompraServiceImpl implements CompraService {

    private static final Logger LOGGER = Logger.getLogger(CompraServiceImpl.class.getName());

    /**
     * Operacion que aplica /confirmar en ms-producto. ESTA EN UN SOLO
     * LUGAR para poder invertirla como cambio en vivo (por ejemplo
     * DESCONTAR -> INCREMENTAR) tocando esta unica linea.
     */
    static final OperacionStock OPERACION_AL_CONFIRMAR = OperacionStock.DESCONTAR;

    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final CompraRepository compraRepository;
    private final CompraAuditoriaRepository compraAuditoriaRepository;
    private final ProveedorClient proveedorClient;
    private final ProductoClient productoClient;

    public CompraServiceImpl(CompraRepository compraRepository,
                             CompraAuditoriaRepository compraAuditoriaRepository,
                             ProveedorClient proveedorClient,
                             ProductoClient productoClient) {
        this.compraRepository = compraRepository;
        this.compraAuditoriaRepository = compraAuditoriaRepository;
        this.proveedorClient = proveedorClient;
        this.productoClient = productoClient;
    }

    /* ==================== REGISTRAR ==================== */

    @Override
    @Transactional
    public CompraResponse registrar(CompraRequest request) {
        String numero = normalizarNumero(request.numeroCompra());
        validarNumeroLibre(numero);
        validarProveedor(request.proveedorId());
        List<CompraDetalle> detalles = construirDetalles(request.proveedorId(), request.detalles());

        Compra compra = new Compra();
        compra.setNumeroCompra(numero);
        compra.setProveedorId(request.proveedorId());
        compra.setFecha(LocalDateTime.now());
        compra.setEstado(EstadoCompra.REGISTRADA);
        compra.setObservaciones(request.observaciones());
        detalles.forEach(compra::agregarDetalle);
        compra.setTotal(calcularTotal(detalles));

        Compra guardada = compraRepository.save(compra);
        registrarAuditoria(guardada, AccionCompra.REGISTRADA, null, EstadoCompra.REGISTRADA.name(),
                "Compra registrada con " + detalles.size() + " detalle(s). El stock aun NO esta descontado.");
        return CompraMapper.toDto(guardada);
    }

    /* ==================== ACTUALIZAR (PUT) ==================== */

    @Override
    @Transactional
    public CompraResponse actualizar(Long id, CompraRequest request) {
        Compra compra = obtenerEntidad(id);
        if (compra.getEstado() != EstadoCompra.REGISTRADA) {
            throw new CompraNoModificableException(
                    "Solo una compra REGISTRADA puede modificarse (estado actual: " + compra.getEstado() + ")");
        }
        String numero = normalizarNumero(request.numeroCompra());
        if (!numero.equals(compra.getNumeroCompra())) {
            validarNumeroLibre(numero);
        }
        validarProveedor(request.proveedorId());
        List<CompraDetalle> detalles = construirDetalles(request.proveedorId(), request.detalles());

        compra.setNumeroCompra(numero);
        compra.setProveedorId(request.proveedorId());
        compra.setObservaciones(request.observaciones());
        compra.limpiarDetalles();
        detalles.forEach(compra::agregarDetalle);
        compra.setTotal(calcularTotal(detalles));

        Compra guardada = compraRepository.save(compra);
        registrarAuditoria(guardada, AccionCompra.ACTUALIZADA, EstadoCompra.REGISTRADA.name(),
                EstadoCompra.REGISTRADA.name(),
                "Compra modificada: " + detalles.size() + " detalle(s), total recalculado.");
        return CompraMapper.toDto(guardada);
    }

    /* ==================== CONFIRMAR ==================== */

    @Override
    @Transactional
    public CompraResponse confirmar(Long id) {
        Compra compra = obtenerEntidad(id);
        if (compra.getEstado() != EstadoCompra.REGISTRADA) {
            throw new CompraNoModificableException(
                    "Solo una compra REGISTRADA puede confirmarse (estado actual: " + compra.getEstado() + ")");
        }

        // 1) Revalida TODOS los productos ANTES de descontar: existencia,
        //    ACTIVO, mismo proveedor. Si algo falla, no se toca el stock.
        List<ProductoClientDto> productosValidados = new ArrayList<>();
        for (CompraDetalle detalle : compra.getDetalles()) {
            productosValidados.add(
                    obtenerProductoActivoDelProveedor(detalle.getProductoId(), compra.getProveedorId()));
        }

        // 2) Descuenta producto por producto con la constante aislada. Si
        //    alguno falla, COMPENSA: re-llama a ms-producto con la operacion
        //    INVERSA por cada cantidad ya descontada (el rollback local no
        //    puede deshacer llamadas remotas).
        List<CompraDetalle> descontadas = new ArrayList<>();
        OperacionStock operacion = OPERACION_AL_CONFIRMAR;
        try {
            for (CompraDetalle detalle : compra.getDetalles()) {
                ProductoClientDto ajustado = llamarRemoto(
                        () -> productoClient.ajustarStock(detalle.getProductoId(),
                                new AjusteStockRequest(detalle.getCantidad(), operacion.name())),
                        "MS-PRODUCTO");
                LOGGER.info("Stock " + operacion + " aplicado al producto "
                        + detalle.getProductoId() + ": nueva existencia " + ajustado.stock());
                descontadas.add(detalle);
            }
        } catch (RuntimeException fallo) {
            LOGGER.warning("Confirmacion de la compra " + id + " fallo: "
                    + fallo.getClass().getSimpleName() + (fallo.getMessage() != null ? (": " + fallo.getMessage()) : ""));
            compensar(descontadas);
            if (fallo instanceof ExcepcionNegocio negocio) {
                throw negocio;
            }
            throw new ServicioNoDisponibleException(
                    "La confirmacion fallo y el stock fue compensado. Intente nuevamente.");
        }

        // 3) Cambia el estado y escribe la auditoria en la MISMA transaccion.
        compra.setEstado(EstadoCompra.CONFIRMADA);
        Compra guardada = compraRepository.save(compra);
        registrarAuditoria(guardada, AccionCompra.CONFIRMADA, EstadoCompra.REGISTRADA.name(),
                EstadoCompra.CONFIRMADA.name(),
                "Stock " + operacion.name() + " aplicado en " + descontadas.size() + " producto(s).");
        return CompraMapper.toDto(guardada);
    }

    /** Compensacion: devuelve lo ya descontado con la operacion inversa. */
    private void compensar(List<CompraDetalle> descontadas) {
        if (descontadas.isEmpty()) {
            return;
        }
        OperacionStock compensacion = OPERACION_AL_CONFIRMAR.inversa();
        LOGGER.warning("COMPENSACION: aplicando " + compensacion +
                " sobre " + descontadas.size() + " producto(s) ya descontado(s).");
        for (CompraDetalle detalle : descontadas) {
            try {
                llamarRemoto(() -> productoClient.ajustarStock(detalle.getProductoId(),
                        new AjusteStockRequest(detalle.getCantidad(), compensacion.name())), "MS-PRODUCTO");
            } catch (RuntimeException falloCompensacion) {
                // best-effort: si incluso la compensacion cae, se deja
                // registrado en el log para revision manual.
                LOGGER.severe("No se pudo compensar el producto " + detalle.getProductoId()
                        + ": " + falloCompensacion.getMessage());
            }
        }
    }

    /* ==================== ANULAR ==================== */

    @Override
    @Transactional
    public CompraResponse anular(Long id) {
        Compra compra = obtenerEntidad(id);
        EstadoCompra anterior = compra.getEstado();
        if (anterior == EstadoCompra.ANULADA) {
            throw new CompraNoModificableException("La compra ya esta ANULADA");
        }

        if (anterior == EstadoCompra.CONFIRMADA) {
            // Devuelve el stock: operacion INVERSA a la de la confirmacion.
            OperacionStock devolucion = OPERACION_AL_CONFIRMAR.inversa();
            for (CompraDetalle detalle : compra.getDetalles()) {
                llamarRemoto(() -> productoClient.ajustarStock(detalle.getProductoId(),
                        new AjusteStockRequest(detalle.getCantidad(), devolucion.name())), "MS-PRODUCTO");
            }
        }
        // Si estaba REGISTRADA: solo cambia estado (nunca desconto stock).

        compra.setEstado(EstadoCompra.ANULADA);
        Compra guardada = compraRepository.save(compra);
        registrarAuditoria(guardada, AccionCompra.ANULADA, anterior.name(), EstadoCompra.ANULADA.name(),
                anterior == EstadoCompra.CONFIRMADA
                        ? "Compra anulada; stock devuelto en " + compra.getDetalles().size() + " producto(s)."
                        : "Compra anulada (estaba REGISTRADA: no desconto stock, no hay devolucion).");
        return CompraMapper.toDto(guardada);
    }

    /* ==================== ELIMINAR ==================== */

    @Override
    @Transactional
    public void eliminar(Long id) {
        Compra compra = obtenerEntidad(id);
        if (compra.getEstado() != EstadoCompra.ANULADA) {
            throw new CompraNoModificableException(
                    "Solo una compra ANULADA puede eliminarse (estado actual: " + compra.getEstado() + ")");
        }
        // 1) Primero el historial (FK interna compra_auditoria -> compras).
        compraAuditoriaRepository.deleteByCompraId(id);
        // 2) Luego la compra; los detalles se borran por cascade ALL.
        compraRepository.deleteById(id);
    }

    /* ==================== CONSULTAS ==================== */

    @Override
    @Transactional(readOnly = true)
    public CompraResponse obtenerPorId(Long id) {
        return CompraMapper.toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponse> listar() {
        return compraRepository.findAll().stream().map(CompraMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponse> listarPorProveedor(Long proveedorId) {
        return compraRepository.findByProveedorId(proveedorId).stream()
                .map(CompraMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaResponse> auditoria(Long id) {
        obtenerEntidad(id); // verifica existencia (404 si no existe)
        return compraAuditoriaRepository.findByCompraIdOrderByFechaDesc(id).stream()
                .map(CompraMapper::auditoriaToDto).toList();
    }

    /* ==================== VALIDACIONES ==================== */

    /** El proveedor debe existir (ms-proveedor responde 404 si no) y estar ACTIVO. */
    private ProveedorClientDto validarProveedor(Long proveedorId) {
        ProveedorClientDto proveedor = llamarRemoto(
                () -> proveedorClient.obtenerProveedor(proveedorId), "MS-PROVEEDOR");
        if (proveedor == null || !ESTADO_ACTIVO.equals(proveedor.estado())) {
            throw new ProveedorInactivoException(
                    "El proveedor " + proveedorId + " no esta ACTIVO");
        }
        return proveedor;
    }

    private ProductoClientDto obtenerProductoActivoDelProveedor(Long productoId, Long proveedorId) {
        ProductoClientDto producto = llamarRemoto(
                () -> productoClient.obtenerProducto(productoId), "MS-PRODUCTO");
        if (!ESTADO_ACTIVO.equals(producto.estado())) {
            throw new ConflictoException(
                    "El producto " + productoId + " no esta ACTIVO");
        }
        if (!Objects.equals(producto.proveedorId(), proveedorId)) {
            throw new ConflictoException(
                    "El producto " + productoId + " pertenece a otro proveedor");
        }
        return producto;
    }

    private List<CompraDetalle> construirDetalles(Long proveedorId, List<com.orellana.microservicios.ms_compra.dto.DetalleRequest> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new ConflictoException("La compra debe tener al menos un detalle");
        }
        Set<Long> productosVistos = new HashSet<>();
        List<CompraDetalle> detalles = new ArrayList<>();
        for (com.orellana.microservicios.ms_compra.dto.DetalleRequest pedido : pedidos) {
            if (!productosVistos.add(pedido.productoId())) {
                throw new ConflictoException(
                        "El producto " + pedido.productoId() + " esta repetido en la compra");
            }
            ProductoClientDto producto =
                    obtenerProductoActivoDelProveedor(pedido.productoId(), proveedorId);
            if (producto.stock() == null || producto.stock() < pedido.cantidad()) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para el producto " + producto.nombre()
                                + " (stock disponible " + producto.stock()
                                + ", solicitado " + pedido.cantidad() + ")");
            }
            CompraDetalle detalle = new CompraDetalle();
            detalle.setProductoId(pedido.productoId());
            detalle.setCantidad(pedido.cantidad());
            // El precio_unitario de compra lo envía el cliente (> 0, 2 decimales);
            // puede diferir del precio de catálogo (Producto.precio = referencia).
            BigDecimal precio = pedido.precioUnitario().setScale(2, RoundingMode.HALF_UP);
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(precio.multiply(BigDecimal.valueOf(pedido.cantidad()))
                    .setScale(2, RoundingMode.HALF_UP));
            detalles.add(detalle);
        }
        return detalles;
    }

    /* ==================== AYUDANTES ==================== */

    private Compra obtenerEntidad(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La compra " + id + " no existe"));
    }

    /** El numero de compra lo escribe el usuario: se guarda sin espacios y en mayusculas. */
    private String normalizarNumero(String numero) {
        return numero == null ? "" : numero.trim().toUpperCase();
    }

    /** Rechaza con 409 un numero de compra que ya existe (la unique de la tabla respalda). */
    private void validarNumeroLibre(String numero) {
        if (compraRepository.existsByNumeroCompra(numero)) {
            throw new ConflictoException("El numero de compra " + numero + " ya existe");
        }
    }

    private BigDecimal calcularTotal(List<CompraDetalle> detalles) {
        BigDecimal total = BigDecimal.ZERO;
        for (CompraDetalle detalle : detalles) {
            total = total.add(detalle.getSubtotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Escribe la fila de compra_auditoria en la MISMA transaccion del
     * cambio de estado: si la transaccion revierte, el historial tambien.
     */
    private void registrarAuditoria(Compra compra, AccionCompra accion,
                                    String estadoAnterior, String estadoNuevo, String detalle) {
        CompraAuditoria auditoria = new CompraAuditoria();
        auditoria.setCompraId(compra.getId());
        auditoria.setAccion(accion);
        auditoria.setEstadoAnterior(estadoAnterior);
        auditoria.setEstadoNuevo(estadoNuevo);
        auditoria.setUsuario(usuarioActual());
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setDetalle(detalle);
        compraAuditoriaRepository.save(auditoria);
    }

    /** Usuario del header X-Usuario; "sistema" como respaldo. */
    private String usuarioActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String usuario = atributos.getRequest().getHeader(UsuarioAuditoriaAware.HEADER_USUARIO);
            if (usuario != null && !usuario.isBlank()) {
                return usuario.trim();
            }
        }
        return UsuarioAuditoriaAware.USUARIO_DEFECTO;
    }

    /**
     * Llamada remota unificada: los errores HTTP 404/409 ya llegan traducidos
     * por el FeignErrorDecoder (dominio); aqui se capturan los fallos de
     * CONEXION / TIMEOUT (FeignException) y se convierten en 503 local.
     */
    private <T> T llamarRemoto(Supplier<T> llamada, String servicio) {
        try {
            return llamada.get();
        } catch (ExcepcionNegocio negocio) {
            // 404/409 ya traducidos por el FeignErrorDecoder: se dejan pasar
            throw negocio;
        } catch (FeignException ex) {
            LOGGER.warning("Llamada remota a " + servicio + " fallo: HTTP " + ex.status());
            throw new ServicioNoDisponibleException(
                    "El servicio " + servicio + " no esta disponible en este momento. Intente nuevamente.");
        } catch (RuntimeException ex) {
            // fallo inesperado del cliente HTTP (por ejemplo metodo no
            // soportado, serializacion, etc.): tambien es indisponibilidad
            LOGGER.warning("Llamada remota a " + servicio + " fallo inesperado: "
                    + ex.getClass().getSimpleName() + (ex.getMessage() != null ? (": " + ex.getMessage()) : ""));
            throw new ServicioNoDisponibleException(
                    "El servicio " + servicio + " no esta disponible en este momento. Intente nuevamente.");
        }
    }
}
