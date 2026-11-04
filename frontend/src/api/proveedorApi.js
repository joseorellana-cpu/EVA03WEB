import { get, post, put, patch, del } from './apiClient'

/**
 * Endpoints de proveedores (todo bajo /api, vía Gateway 8080).
 * El cliente JAMÁS llama directo a 8081/8082/8083.
 */
export const proveedorApi = {
  listar: () => get('/api/proveedores'),
  obtener: (id) => get(`/api/proveedores/${id}`),
  obtenerPorRuc: (ruc) => get(`/api/proveedores/ruc/${encodeURIComponent(ruc)}`),
  crear: (datos) => post('/api/proveedores', datos),
  actualizar: (id, datos) => put(`/api/proveedores/${id}`, datos),
  cambiarEstado: (id, estado) => patch(`/api/proveedores/${id}/estado`, { estado }),
  eliminar: (id) => del(`/api/proveedores/${id}`),
}
