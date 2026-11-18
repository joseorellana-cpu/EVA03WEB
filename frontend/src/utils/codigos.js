/**
 * Valores POR DEFECTO para los formularios de alta: el siguiente número
 * de compra y el siguiente código de producto, calculados a partir de
 * los que ya existen. Son solo una sugerencia que el usuario puede
 * cambiar; el backend sigue validando que no se repitan (409).
 */

/**
 * Siguiente código con formato PREFIJO + número con ceros a la izquierda.
 * Toma el mayor número existente con ese prefijo y le suma 1.
 *   siguienteCodigo(['PRD-001', 'PRD-006'], 'PRD-', 3) -> 'PRD-007'
 */
export function siguienteCodigo(existentes, prefijo, ancho) {
  const escapado = prefijo.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const patron = new RegExp(`^${escapado}(\\d+)$`, 'i')
  let mayor = 0
  for (const valor of existentes ?? []) {
    const m = patron.exec(String(valor ?? '').trim())
    if (m) mayor = Math.max(mayor, Number(m[1]))
  }
  return prefijo + String(mayor + 1).padStart(ancho, '0')
}

/** Número de compra por defecto: C-AAAA-NNNNNN (secuencia del año actual). */
export function numeroCompraPorDefecto(compras, anio = new Date().getFullYear()) {
  return siguienteCodigo((compras ?? []).map((c) => c.numeroCompra), `C-${anio}-`, 6)
}

/** Código de producto por defecto: PRD-NNN. */
export function codigoProductoPorDefecto(productos) {
  return siguienteCodigo((productos ?? []).map((p) => p.codigo), 'PRD-', 3)
}
