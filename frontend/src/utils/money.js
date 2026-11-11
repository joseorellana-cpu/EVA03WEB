/**
 * Aritmética monetaria segura para el laboratorio.
 * NUNCA se opera con coma flotante: todo se convierte a CENTAVOS enteros,
 * se suma en enteros y se formatea de vuelta.
 *
 * Formato de entrada aceptado: "12.50" (punto) o "12,50" (coma).
 * Máximo 2 decimales; hasta 10 dígitos enteros; rechaza negativos y
 * valores fuera del entero seguro (Number.isSafeInteger).
 */

const RE_MONTO = /^(\d{1,10})([.,]\d{1,2})?$/

function agruparMiles(valorEntero) {
  return String(valorEntero).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function normalizarCadena(valor) {
  return String(valor ?? '')
    .trim()
    .replace(',', '.')
}

export function esMontoValido(valor) {
  const s = normalizarCadena(valor)
  if (!s) return false
  const m = RE_MONTO.exec(s)
  if (!m) return false
  const entero = Number(m[1])
  const decimales = m[2] ? Number('0' + m[2].replace('.', '')) : 0 // '.50' -> 50
  const centavos = entero * 100 + decimales
  return Number.isSafeInteger(centavos)
}

/** Devuelve { ok, centavos, mensaje } — nunca lanza. */
export function parsearMonto(valor) {
  const s = normalizarCadena(valor)
  const m = RE_MONTO.exec(s)
  if (!m) {
    return { ok: false, centavos: 0, mensaje: 'Monto inválido (use hasta 2 decimales, ej. 12.50)' }
  }
  const entero = Number(m[1])
  const fraccion = m[2] ? m[2].slice(1) : '00' // "50" o ""
  const centavos = entero * 100 + Number(fraccion.padEnd(2, '0'))
  if (!Number.isSafeInteger(centavos)) {
    return { ok: false, centavos: 0, mensaje: 'Monto fuera de rango seguro' }
  }
  return { ok: true, centavos, mensaje: null }
}

/** Suma en centavos enteros; devuelve centavos enteros. */
export function sumarCentavos(a, b) {
  const total = a + b
  if (!Number.isSafeInteger(total)) throw new RangeError('Suma monetaria fuera de rango seguro')
  return total
}

export function sumarMontoCentavos(lista) {
  return lista.reduce((acc, n) => sumarCentavos(acc, n), 0)
}

/** cantidad (entero >= 0) × precioUnitario en centavos; entero seguro. */
export function multiplicarPorCantidad(centavos, cantidad) {
  if (!Number.isSafeInteger(cantidad) || cantidad < 0) {
    throw new RangeError('La cantidad debe ser un entero >= 0')
  }
  const total = centavos * cantidad
  if (!Number.isSafeInteger(total)) throw new RangeError('Total fuera de rango seguro')
  return total
}

/** subtotal en centavos */
export function subtotal(cantidad, precioCentavos) {
  return multiplicarPorCantidad(precioCentavos, cantidad)
}

/** Formatea centavos a "1,234.56" (es-PE). */
export function formatoMonto(centavos) {
  if (!Number.isSafeInteger(centavos)) throw new RangeError('Monto fuera de rango seguro')
  const negativo = centavos < 0
  const abs = Math.abs(centavos)
  const entero = Math.floor(abs / 100)
  const dec = abs % 100
  const texto = `${agruparMiles(entero)}.${String(dec).padStart(2, '0')}`
  return negativo ? `-${texto}` : texto
}

/** "125.00" (estilo salida de API). */
export function formatoApi(centavos) {
  if (!Number.isSafeInteger(centavos)) throw new RangeError('Monto fuera de rango seguro')
  const negativo = centavos < 0
  const abs = Math.abs(centavos)
  return `${negativo ? '-' : ''}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`
}
