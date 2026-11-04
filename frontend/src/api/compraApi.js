import { get, post, put, del } from './apiClient'

/**
 * Endpoints de compras (todo bajo /api, vía Gateway 8080).
 * El servidor es la autoridad: sólo REGISTRADA se edita/confirma; anular
 * devuelve stock si estaba CONFIRMADA; eliminar sólo si ANULADA.
 */
export const compraApi = {
  listar: () => get('/api/compras'),
  obtener: (id) => get(`/api/compras/${id}`),
  listarPorProveedor: (proveedorId) => get(`/api/compras/proveedor/${proveedorId}`),
  crear: (datos) => post('/api/compras', datos),
  actualizar: (id, datos) => put(`/api/compras/${id}`, datos),
  confirmar: (id) => post(`/api/compras/${id}/confirmar`),
  anular: (id) => post(`/api/compras/${id}/anular`),
  eliminar: (id) => del(`/api/compras/${id}`),
  auditoria: (id) => get(`/api/compras/${id}/auditoria`),
}
