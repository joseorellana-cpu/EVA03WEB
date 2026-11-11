import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { get, post, del, ErrorApi } from '@/api/apiClient'

function respuestaJson(status, body) {
  return new Response(body === undefined ? '' : JSON.stringify(body), {
    status,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
  })
}

describe('apiClient — contrato de red seguro', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('no envía cabecera de usuario (tienda sin login)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(respuestaJson(200, []))
    await get('/api/compras')
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/compras')
    expect(init.credentials).toBe('same-origin')
    expect(init.method).toBe('GET')
    expect(init.headers['X-Usuario']).toBeUndefined()
    expect(init.headers.Accept).toBe('application/json')
    expect(init.headers['Content-Type']).toBeUndefined()
  })


  it('POST envía Content-Type application/json y el body serializado', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(respuestaJson(201, { id: 1 }))
    await post('/api/compras', { proveedorId: 1, detalles: [] })
    const init = vi.mocked(fetch).mock.calls[0][1]
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(init.body)).toEqual({ proveedorId: 1, detalles: [] })
  })

  it('400: lanza ErrorApi con details del backend como texto y mensaje en español', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      respuestaJson(400, {
        timestamp: '2026-11-04T10:00:00',
        status: 400,
        error: 'Bad Request',
        message: 'La petición no cumple las validaciones',
        path: '/api/compras',
        details: ['cantidad: la cantidad debe ser mayor que cero'],
      }),
    )
    const error = await post('/api/compras', {}).catch((e) => e)
    expect(error).toBeInstanceOf(ErrorApi)
    expect(error.status).toBe(400)
    expect(error.details).toEqual(['cantidad: la cantidad debe ser mayor que cero'])
    expect(error.message).toBe('La petición no cumple las validaciones')
  })

  it('409: lanza ErrorApi con el mensaje del backend (p.ej. Stock insuficiente)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      respuestaJson(409, {
        status: 409,
        message: 'Stock insuficiente para el producto X (disponible 3, solicitado 5)',
        path: '/api/compras',
        details: [],
      }),
    )
    const error = await post('/api/compras', {}).catch((e) => e)
    expect(error.status).toBe(409)
    expect(error.message).toContain('Stock insuficiente')
  })

  it('503: marca esServicioNoDisponible y mantiene el mensaje del backend', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      respuestaJson(503, {
        status: 503,
        message: 'El servicio MS-PRODUCTO no está disponible en este momento. Intente nuevamente.',
        path: '/api/compras',
        details: [],
      }),
    )
    const error = await post('/api/compras', {}).catch((e) => e)
    expect(error.esServicioNoDisponible).toBe(true)
    expect(error.status).toBe(503)
    expect(error.message).toContain('MS-PRODUCTO')
  })

  it('timeout (AbortError) se normaliza a error de red con mensaje claro', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new DOMException('The operation was aborted', 'AbortError'))
    const error = await get('/api/productos').catch((e) => e)
    expect(error).toBeInstanceOf(ErrorApi)
    expect(error.esRed).toBe(true)
    expect(error.message).toContain('tiempo de espera')
  })

  it('fallo de red (fetch rechaza) se normaliza sin exponer objetos crudos', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('Failed to fetch'))
    const error = await get('/api/productos').catch((e) => e)
    expect(error).toBeInstanceOf(ErrorApi)
    expect(error.esRed).toBe(true)
    expect(error.message).toBe('No se pudo conectar con el servidor. Verifique su conexión e intente de nuevo.')
  })

  it('DELETE 204 (sin cuerpo) devuelve null y no lanza', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))
    const resultado = await del('/api/compras/1')
    expect(resultado).toBeNull()
  })

  it('cuerpo vacío en 200 devuelve null (sin JSON inválido crudo)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response('', { status: 200 }))
    expect(await get('/api/compras')).toBeNull()
  })
})
