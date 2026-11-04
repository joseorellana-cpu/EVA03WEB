package com.orellana.microservicios.ms_compra.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Cabeza de la compra (tabla compras, BD propia bd_compra).
 *  - proveedorId y (en los detalles) productoId son SOLO ids: referencias
 *    entre microservicios por id, sin FK entre bases (decisión de diseño;
 *    MySQL sí las permitiría en el mismo servidor, no las usamos).
 *  - fecha es la fecha de NEGOCIO, DISTINTA de fechaCreacion (auditoría).
 *  - detalles: relación interna a esta MISMA base (FK compra_detalles ->
 *    compras), cascade ALL + orphanRemoval para que la compra controle
 *    el ciclo de vida de sus líneas.
 *  - Hereda las columnas de auditoría (fecha_creacion, fecha_modificacion,
 *    creado_por, modificado_por) de AuditoriaBase.
 */
@Entity
@Table(name = "compras")
public class Compra extends AuditoriaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_compra", nullable = false, length = 20, unique = true)
    private String numeroCompra;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCompra estado = EstadoCompra.REGISTRADA;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraDetalle> detalles = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCompra() { return numeroCompra; }
    public void setNumeroCompra(String numeroCompra) { this.numeroCompra = numeroCompra; }

    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public EstadoCompra getEstado() { return estado; }
    public void setEstado(EstadoCompra estado) { this.estado = estado; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<CompraDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<CompraDetalle> detalles) {
        this.detalles = detalles;
        if (detalles != null) {
            detalles.forEach(d -> d.setCompra(this));
        }
    }

    public void agregarDetalle(CompraDetalle detalle) {
        detalle.setCompra(this);
        this.detalles.add(detalle);
    }

    public void limpiarDetalles() {
        this.detalles.clear();
    }
}
