import { get, post, put, patch, del } from './apiClient'

/**
 * Endpoints de productos (todo bajo /api, vía Gateway 8080).
 */
export const productoApi = {
  listar: () => get('/api/productos'),
  obtener: (id) => get(`/api/productos/${id}`),
  listarPorProveedor: (proveedorId) => get(`/api/productos/proveedor/${proveedorId}`),
  crear: (datos) => post('/api/productos', datos),
  actualizar: (id, datos) => put(`/api/productos/${id}`, datos),
  cambiarEstado: (id, estado) => patch(`/api/productos/${id}/estado`, { estado }),
  eliminar: (id) => del(`/api/productos/${id}`),
  ajustarStock: (id, cantidad, operacion, motivo) => {
    const consulta = motivo ? `?motivo=${encodeURIComponent(motivo)}` : ''
    return patch(`/api/productos/${id}/stock${consulta}`, { cantidad, operacion })
  },
  listarMovimientos: (id) => get(`/api/productos/${id}/movimientos`),
}
