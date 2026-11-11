<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { productoApi } from '@/api/productoApi'
import { proveedorApi } from '@/api/proveedorApi'
import { etiquetaEstado, claseEstado } from '@/utils/formatters'
import BotonAccion from '@/components/BotonAccion.vue'

const productos = ref([])
const proveedores = ref([])
const cargando = ref(true)
const error = ref(null)
const buscando = ref('')
const filtroEstado = ref('')
const filtroProveedor = ref('')
const ajustandoId = ref(null)
const stockMovido = ref(null)

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [ps, pr] = await Promise.all([productoApi.listar(), proveedorApi.listar()])
    productos.value = ps
    proveedores.value = pr
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

const visibles = computed(() => {
  const texto = buscando.value.trim().toUpperCase()
  return productos.value.filter((p) => {
    if (filtroEstado.value && p.estado !== filtroEstado.value) return false
    if (filtroProveedor.value && String(p.proveedorId) !== String(filtroProveedor.value)) return false
    if (!texto) return true
    return [p.codigo, p.nombre, p.categoria, p.estado]
      .filter(Boolean)
      .join(' ')
      .toUpperCase()
      .includes(texto)
  })
})

async function cambiarEstado(p) {
  ajustandoId.value = p.id
  const nuevo = p.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
  try {
    const r = await productoApi.cambiarEstado(p.id, nuevo)
    p.estado = r?.estado ?? nuevo
  } catch (e) {
    error.value = e
  } finally {
    ajustandoId.value = null
  }
}
</script>

<template>
  <section class="pagina">
    <div class="pagina__encabezado">
      <h1>Productos</h1>
      <RouterLink to="/productos/nuevo" class="btn btn--primary">+ Nuevo producto</RouterLink>
    </div>

    <div class="filtros">
      <label class="filtros__campo">
        <span>Buscar</span>
        <input v-model="buscando" type="search" placeholder="Código, nombre, categoría…" />
      </label>
      <label class="filtros__campo">
        <span>Proveedor</span>
        <select v-model="filtroProveedor">
          <option value="">Todos</option>
          <option v-for="prov in proveedores" :key="prov.id" :value="prov.id">{{ prov.nombreComercial || prov.razonSocial }} ({{ prov.id }})</option>
        </select>
      </label>
      <label class="filtros__campo">
        <span>Estado</span>
        <select v-model="filtroEstado">
          <option value="">Todos</option>
          <option>ACTIVO</option>
          <option>INACTIVO</option>
        </select>
      </label>
      <button type="button" @click="cargar">Recargar</button>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando productos…</p>
    <p v-else-if="error" class="alerta alerta--error" role="alert">
      {{ error.message }}
      <span v-if="error.details?.length"> — {{ error.details.join(' · ') }}</span>
    </p>
    <p v-else-if="!visibles.length" class="estado-vacio">Sin resultados.</p>

    <div v-else class="tabla-wrap">
      <table class="tabla">
        <thead>
          <tr>
            <th>Código</th>
            <th>Nombre</th>
            <th>Categoría</th>
            <th>Precio</th>
            <th>Stock</th>
            <th>Bajo stock mínimo</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in visibles" :key="p.id">
            <td>{{ p.codigo }}</td>
            <td>{{ p.nombre }}</td>
            <td>{{ p.categoria || '—' }}</td>
            <td>{{ p.precio }}</td>
            <td>{{ p.stock }}</td>
            <td><span v-if="p.bajoStockMinimo" class="badge badge--warn">Sí</span><span v-else>—</span></td>
            <td><span :class="claseEstado(p.estado)">{{ etiquetaEstado(p.estado) }}</span></td>
            <td class="acciones">
              <RouterLink :to="`/productos/${p.id}/editar`">Editar</RouterLink>
              <RouterLink :to="`/productos/${p.id}/movimientos`">Movimientos</RouterLink>
              <BotonAccion
                :texto="p.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'"
                :varianta="p.estado === 'ACTIVO' ? 'warn' : 'primary'"
                :cargando="ajustandoId === p.id"
                class="btn--sm"
                @accion="cambiarEstado(p)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
