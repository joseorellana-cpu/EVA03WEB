package com.orellana.microservicios.ms_compra.model;

import java.time.LocalDateTime;

/**
 * Generador del numero de compra: formato C-AAAA-NNNNNN
 * (ejemplo: C-2026-000002). El secuencial corre por año, tomando
 * como base las compras existentes iniciadas en "C-AAAA-"; si el
 * numero resultante ya existe (colision en concurrencia), avanza
 * hasta hallar uno libre. La unique de la tabla (compras.numero_compra)
 * es el respaldo final contra duplicados.
 */
public final class NumeroCompraGenerator {

    private NumeroCompraGenerator() {
        // clase utilitaria: no se instancia
    }

    public static final String PREFIJO = "C-";

    /**
     * Genera el siguiente numero tomando como base el contador de
     * compras del año actual (que recibe como parámetro para no acoplar
     * el generador al repositorio: principio D).
     */
    public static String siguiente(String _anio, long secuencialBase) {
        return String.format("C-%s-%06d", _anio, secuencialBase);
    }

    public static String anioActual() {
        return String.valueOf(LocalDateTime.now().getYear());
    }
}
