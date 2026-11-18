import { createRouter, createWebHistory } from 'vue-router'

// Cada :id se valida como entero positivo ANTES de permitir la ruta
// (defensa contra path traversal y números basura en la URL).
const ID_POSITIVO = /^[1-9]\d*$/
const requiereId = (to) => {
  const id = to.params.id
  return typeof id === 'string' && ID_POSITIVO.test(id)
}

const NOMBRE = {
  home: 'Tienda',
  proveedores: 'Proveedores',
  'proveedores-nuevo': 'Nuevo proveedor',
  'proveedores-editar': 'Editar proveedor',
  productos: 'Productos',
  'productos-nuevo': 'Nuevo producto',
  'productos-editar': 'Editar producto',
  'productos-movimientos': 'Movimientos de stock',
  compras: 'Compras',
  'compras-nueva': 'Nueva compra',
  'compras-detalle': 'Detalle de compra',
  'compras-editar': 'Editar compra',
  'not-found': '404',
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/proveedores', name: 'proveedores', component: () => import('../views/proveedores/ProveedorListView.vue') },
    { path: '/proveedores/nuevo', name: 'proveedores-nuevo', component: () => import('../views/proveedores/ProveedorFormView.vue') },
    { path: '/proveedores/:id/editar', name: 'proveedores-editar', component: () => import('../views/proveedores/ProveedorFormView.vue'), props: true, beforeEnter: (to) => (requiereId(to) ? true : { name: 'not-found' }) },
    { path: '/productos', name: 'productos', component: () => import('../views/productos/ProductoListView.vue') },
    { path: '/productos/nuevo', name: 'productos-nuevo', component: () => import('../views/productos/ProductoFormView.vue') },
    { path: '/productos/:id/editar', name: 'productos-editar', component: () => import('../views/productos/ProductoFormView.vue'), props: true, beforeEnter: (to) => (requiereId(to) ? true : { name: 'not-found' }) },
    { path: '/productos/:id/movimientos', name: 'productos-movimientos', component: () => import('../views/productos/ProductoMovimientosView.vue'), props: true, beforeEnter: (to) => (requiereId(to) ? true : { name: 'not-found' }) },
    { path: '/compras', name: 'compras', component: () => import('../views/compras/CompraListView.vue') },
    { path: '/compras/nueva', name: 'compras-nueva', component: () => import('../views/compras/CompraNuevaView.vue') },
    { path: '/compras/:id', name: 'compras-detalle', component: () => import('../views/compras/CompraDetalleView.vue'), props: true, beforeEnter: (to) => (requiereId(to) ? true : { name: 'not-found' }) },
    { path: '/compras/:id/editar', name: 'compras-editar', component: () => import('../views/compras/CompraEditarView.vue'), props: true, beforeEnter: (to) => (requiereId(to) ? true : { name: 'not-found' }) },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFoundView.vue') },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.afterEach((to) => {
  document.title = `${NOMBRE[to.name] ?? 'La Tiendita'} · La Tiendita`
})

export default router
