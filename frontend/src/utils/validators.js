import { esMontoValido } from './money'

/**
 * Validaciones del lado cliente = ESPEJO de las del backend, SOLO como
 * ayuda de UX. La autoridad sigue siendo el servidor; sus errores 400
 * se muestran tal cual en el formulario.
 */
export const LIMITES = {
  ruc: 11,
  codigoMax: 30,
  nombreMax: 150,
  razonSocialMax: 150,
  emailMax: 120,
  telefonoMax: 20,
  direccionMax: 200,
  categoriaMax: 80,
  observacionesMax: 300,
  nombreProductoMax: 150,
  descripcionMax: 500,
  stockMax: 1_000_000,
  cantidadMax: 1_000_000,
}

const RUC_RE = /^\d{11}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

export function validarRuc(valor) {
  const s = String(valor ?? '').trim()
  if (!RUC_RE.test(s)) return { ok: false, mensaje: 'El RUC debe tener exactamente 11 dígitos numéricos' }
  return { ok: true, mensaje: null }
}

export function validarEmail(valor, { opcional = true } = {}) {
  const s = String(valor ?? '').trim()
  if (!s) return opcional ? { ok: true, mensaje: null } : { ok: false, mensaje: 'El email es obligatorio' }
  if (s.length > LIMITES.emailMax) return { ok: false, mensaje: `El email no debe exceder ${LIMITES.emailMax} caracteres` }
  if (!EMAIL_RE.test(s)) return { ok: false, mensaje: 'El email no tiene un formato válido' }
  return { ok: true, mensaje: null }
}

export function validarPrecio(valor) {
  const s = String(valor ?? '').trim()
  if (!esMontoValido(s)) {
    return { ok: false, mensaje: 'El precio debe ser mayor que cero con hasta 2 decimales' }
  }
  const { centavos } = parsearCentavos(s)
  if (centavos <= 0) return { ok: false, mensaje: 'El precio debe ser mayor que cero' }
  return { ok: true, mensaje: null }
}

function parsearCentavos(s) {
  const normalizado = s.trim().replace(',', '.')
  const [entero, fraccion = ''] = normalizado.split('.')
  return { centavos: Number(entero) * 100 + Number((fraccion + '00').slice(0, 2)) }
}

export function validarEnteroNoNegativo(valor, { max = LIMITES.stockMax, campo = 'stock' } = {}) {
  const n = typeof valor === 'string' ? Number(valor.trim()) : valor
  if (!Number.isInteger(n) || n < 0) {
    return { ok: false, mensaje: `El ${campo} debe ser un entero >= 0` }
  }
  if (n > max) return { ok: false, mensaje: `El ${campo} no puede exceder ${max}` }
  return { ok: true, mensaje: null }
}

export function validarCantidad(valor, { min = 1, max = LIMITES.cantidadMax } = {}) {
  const n = typeof valor === 'string' ? Number(valor.trim()) : valor
  if (!Number.isInteger(n)) return { ok: false, mensaje: 'La cantidad debe ser un entero' }
  if (n < min) return { ok: false, mensaje: `La cantidad debe ser >= ${min}` }
  if (n > max) return { ok: false, mensaje: `La cantidad no puede exceder ${max}` }
  return { ok: true, mensaje: null }
}

export function longitudObligatoria(valor, max, campo) {
  const s = String(valor ?? '').trim()
  if (!s) return { ok: false, mensaje: `El ${campo} es obligatorio` }
  if (s.length > max) return { ok: false, mensaje: `El ${campo} no debe exceder ${max} caracteres` }
  return { ok: true, mensaje: null }
}

export function longitudOpcional(valor, max, campo) {
  const s = String(valor ?? '').trim()
  if (!s) return { ok: true, mensaje: null }
  if (s.length > max) return { ok: false, mensaje: `El ${campo} no debe exceder ${max} caracteres` }
  return { ok: true, mensaje: null }
}

export function validarUsuarioSimulado(valor) {
  const s = String(valor ?? '').trim().toLowerCase()
  return /^[a-z0-9][a-z0-9._-]{2,39}$/.test(s)
}
