/**
 * Formato para presentación. Fechas con Intl (nunca concatenación manual).
 * Estados de negocio → etiqueta legible + clave para estilos (css).
 */

const fechaHora = new Intl.DateTimeFormat('es-PE', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

export function formatearFecha(valor) {
  if (!valor) return '—'
  const fecha = valor instanceof Date ? valor : new Date(valor)
  return Number.isNaN(fecha.getTime()) ? '—' : fechaHora.format(fecha)
}

const ETIQUETAS = {
  REGISTRADA: 'REGISTRADA',
  CONFIRMADA: 'CONFIRMADA',
  ANULADA: 'ANULADA',
  ACTIVO: 'ACTIVO',
  INACTIVO: 'INACTIVO',
}

export function etiquetaEstado(estado) {
  return ETIQUETAS[estado] ?? estado ?? '—'
}

/** Clase CSS de badge por estado (ver assets/main.css → .badge--*) */
export function claseEstado(estado) {
  switch (estado) {
    case 'REGISTRADA':
      return 'badge badge--info'
    case 'CONFIRMADA':
      return 'badge badge--ok'
    case 'ANULADA':
      return 'badge badge--danger'
    case 'ACTIVO':
      return 'badge badge--ok'
    case 'INACTIVO':
      return 'badge badge--warn'
    default:
      return 'badge'
  }
}
