/**
 * Cliente HTTP único del frontend. NO existen otras capas de red.
 *
 * Reglas de seguridad (F01/F09/F12/F13/F14):
 *  - Todas las peticiones salen al MISMO ORIGEN (baseUrl vacía) o,
 *    en desarrollo, vía corredor de proxy de Vite (/api → 8080). Nunca a 8081/8082/8083.
 *  - Timeout de 10 s con AbortController.
 *  - JSON parseado de forma segura: cuerpo vacío (204) o JSON inválido => null.
 *  - Errores NORMALIZADOS a ErrorApi { status, message, details[], esRed, esServicioNoDisponible };
 *    nunca se exponen trazas ni objetos crudos de red.
 */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''
const TIMEOUT_MS = 10_000
const MENSAJE_GENERICO = 'Ocurrió un error en el servidor. Intente nuevamente.'
const MENSAJE_RED = 'No se pudo conectar con el servidor. Verifique su conexión e intente de nuevo.'
const MENSAJE_TIEMPO =
  'La petición superó el tiempo de espera (10 s). Verifique su conexión e intente de nuevo.'

export class ErrorApi extends Error {
  constructor({ status, message, details = [], esRed = false, esServicioNoDisponible = false }) {
    super(message || MENSAJE_GENERICO)
    this.name = 'ErrorApi'
    this.status = status
    this.details = Array.isArray(details) ? details.map((d) => String(d)) : []
    this.esRed = esRed
    this.esServicioNoDisponible = esServicioNoDisponible
  }
}

async function parsearJsonSeguro(response) {
  const texto = await response.text()
  if (!texto) return null
  try {
    return JSON.parse(texto)
  } catch {
    // JSON inválido desde el servidor: se trata como error de servicio,
    // jamás como excepción cruda de sintaxis en la UI.
    return null
  }
}

function normalizarError(status, cuerpo) {
  const esServicioNoDisponible = status === 503
  const message =
    cuerpo && typeof cuerpo.message === 'string' && cuerpo.message.trim()
      ? cuerpo.message
      : esServicioNoDisponible
        ? 'El servicio no está disponible. Intente nuevamente.'
        : MENSAJE_GENERICO
  const details =
    cuerpo && Array.isArray(cuerpo.details) ? cuerpo.details.map((d) => String(d)) : []
  return new ErrorApi({ status, message, details, esRed: false, esServicioNoDisponible })
}

export async function http(path, { method = 'GET', body } = {}) {
  const controlador = new AbortController()
  const temporizador = setTimeout(() => controlador.abort(), TIMEOUT_MS)

  const encabezados = {
    Accept: 'application/json',
  }
  if (body !== undefined) {
    encabezados['Content-Type'] = 'application/json'
  }

  try {
    const respuesta = await fetch(`${API_BASE}${path}`, {
      method,
      credentials: 'same-origin',
      headers: encabezados,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controlador.signal,
    })

    const cuerpo = await parsearJsonSeguro(respuesta)
    if (!respuesta.ok) {
      throw normalizarError(respuesta.status, cuerpo)
    }
    return cuerpo
  } catch (error) {
    if (error instanceof ErrorApi) throw error
    if (error?.name === 'AbortError') {
      throw new ErrorApi({
        status: 0,
        message: MENSAJE_TIEMPO,
        details: [],
        esRed: true,
        esServicioNoDisponible: true,
      })
    }
    throw new ErrorApi({
      status: 0,
      message: MENSAJE_RED,
      details: [],
      esRed: true,
      esServicioNoDisponible: true,
    })
  } finally {
    clearTimeout(temporizador)
  }
}

export const get = (path) => http(path, { method: 'GET' })
export const post = (path, body) => http(path, { method: 'POST', body })
export const put = (path, body) => http(path, { method: 'PUT', body })
export const patch = (path, body) => http(path, { method: 'PATCH', body })
export const del = (path) => http(path, { method: 'DELETE' })
