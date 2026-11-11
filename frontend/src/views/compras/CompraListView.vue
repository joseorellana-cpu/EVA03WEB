<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { compraApi } from '@/api/compraApi'
import { proveedorApi } from '@/api/proveedorApi'
import { etiquetaEstado, claseEstado, formatearFecha } from '@/utils/formatters'
import BotonAccion from '@/components/BotonAccion.vue'

const compras = ref([])
const proveedores = ref([])
const cargando = ref(true)
const error = ref(null)
const buscando = ref('')
const filtroEstado = ref('')
const filtroProveedor = ref('')
const accionId = ref(null)

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [c, p] = await Promise.all([compraApi.listar(), proveedorApi.listar()])
    compras.value = c
    proveedores.value = p
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

const visibles = computed(() => {
  const texto = buscando.value.trim().toUpperCase()
  return compras.value.filter((c) => {
    if (filtroEstado.value && c.estado !== filtroEstado.value) return false
    if (filtroProveedor.value && String(c.proveedorId) !== String(filtroProveedor.value)) return false
    if (!texto) return true
    return [c.numeroCompra, String(c.proveedorId), c.estado]
      .filter(Boolean)
      .join(' ')
      .toUpperCase()
      .includes(texto)
  })
})

async function confirmar(c) {
  accionId.value = c.id
  try {
    const r = await compraApi.confirmar(c.id)
    c.estado = r?.estado ?? 'CONFIRMADA'
  } catch (e) {
    error.value = e
  } finally {
    accionId.value = null
  }
}

async function anular(c) {
  accionId.value = c.id
  try {
    const r = await compraApi.anular(c.id)
    c.estado = r?.estado ?? 'ANULADA'
  } catch (e) {
    error.value = e
  } finally {
    accionId.value = null
  }
}
</script>

<template>
  <section class="pagina">
    <div class="pagina__encabezado">
      <h1>Compras</h1>
      <RouterLink to="/compras/nueva" class="btn btn--primary">+ Nueva compra</RouterLink>
    </div>

    <div class="filtros">
      <label class="filtros__campo">
        <span>Buscar</span>
        <input v-model="buscando" type="search" placeholder="Número, proveedor…" />
      </label>
      <label class="filtros__campo">
        <span>Proveedor</span>
        <select v-model="filtroProveedor">
          <option value="">Todos</option>
          <option v-for="p in proveedores" :key="p.id" :value="p.id">{{ p.nombreComercial || p.razonSocial }} ({{ p.id }})</option>
        </select>
      </label>
      <label class="filtros__campo">
        <span>Estado</span>
        <select v-model="filtroEstado">
          <option value="">Todos</option>
          <option>REGISTRADA</option>
          <option>CONFIRMADA</option>
          <option>ANULADA</option>
        </select>
      </label>
      <button type="button" @click="cargar">Recargar</button>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando compras…</p>
    <p v-else-if="error" class="alerta alerta--error" role="alert">
      {{ error.message }}
      <span v-if="error.details?.length"> — {{ error.details.join(' · ') }}</span>
    </p>
    <p v-else-if="!visibles.length" class="estado-vacio">Sin resultados.</p>

    <div v-else class="tabla-wrap">
      <table class="tabla">
        <thead>
          <tr>
            <th>Número</th>
            <th>Proveedor</th>
            <th>Fecha</th>
            <th>Estado</th>
            <th>Total</th>
            <th>Creado por</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in visibles" :key="c.id">
            <td><RouterLink :to="`/compras/${c.id}`">{{ c.numeroCompra }}</RouterLink></td>
            <td>{{ c.proveedorId }}</td>
            <td>{{ formatearFecha(c.fecha) }}</td>
            <td><span :class="claseEstado(c.estado)">{{ etiquetaEstado(c.estado) }}</span></td>
            <td>{{ c.total }}</td>
            <td>{{ c.creadoPor || '—' }}</td>
            <td class="acciones">
              <BotonAccion
                v-if="c.estado === 'REGISTRADA'"
                texto="Confirmar"
                :cargando="accionId === c.id"
                class="btn--sm"
                @accion="confirmar(c)"
              />
              <BotonAccion
                v-if="c.estado === 'CONFIRMADA' || c.estado === 'REGISTRADA'"
                texto="Anular"
                varianta="warn"
                :cargando="accionId === c.id"
                class="btn--sm"
                @accion="anular(c)"
              />
              <RouterLink :to="`/compras/${c.id}`">Ver</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
