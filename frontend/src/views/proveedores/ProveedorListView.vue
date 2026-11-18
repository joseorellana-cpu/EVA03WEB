<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { proveedorApi } from '@/api/proveedorApi'
import { etiquetaEstado, claseEstado } from '@/utils/formatters'
import BotonAccion from '@/components/BotonAccion.vue'

const proveedores = ref([])
const cargando = ref(true)
const error = ref(null)
const buscando = ref('')
const filtroEstado = ref('')
const confirmandoId = ref(null)

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    proveedores.value = await proveedorApi.listar()
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

const visibles = computed(() => {
  const texto = buscando.value.trim().toUpperCase()
  return proveedores.value.filter((p) => {
    if (filtroEstado.value && p.estado !== filtroEstado.value) return false
    if (!texto) return true
    return [p.razonSocial, p.nombreComercial, p.ruc, p.estado]
      .filter(Boolean)
      .join(' ')
      .toUpperCase()
      .includes(texto)
  })
})

async function cambiarEstado(p) {
  confirmandoId.value = p.id
  const nuevo = p.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
  try {
    await proveedorApi.cambiarEstado(p.id, nuevo)
    p.estado = nuevo
  } catch (e) {
    error.value = e
  } finally {
    confirmandoId.value = null
  }
}
</script>

<template>
  <section class="pagina">
    <div class="pagina__encabezado">
      <h1>Proveedores</h1>
      <RouterLink to="/proveedores/nuevo" class="btn btn--primary">+ Nuevo proveedor</RouterLink>
    </div>

    <div class="filtros">
      <label class="filtros__campo">
        <span>Buscar</span>
        <input v-model="buscando" type="search" placeholder="Razón social, nombre o RUC…" />
      </label>
      <label class="filtros__campo">
        <span>Estado</span>
        <select v-model="filtroEstado">
          <option value="">Todos</option>
          <option>ACTIVO</option>
          <option>INACTIVO</option>
        </select>
      </label>
      <button type="button" class="btn" @click="cargar">Recargar</button>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando proveedores…</p>
    <p v-else-if="error" class="alerta alerta--error" role="alert">
      {{ error.message }}
      <span v-if="error.details?.length"> — {{ error.details.join(' · ') }}</span>
    </p>
    <p v-else-if="!visibles.length" class="estado-vacio">Sin resultados.</p>

    <div v-else class="tabla-wrap">
      <table class="tabla">
        <thead>
          <tr>
            <th>RUC</th>
            <th>Razón social</th>
            <th>Nombre comercial</th>
            <th>Estado</th>
            <th>Creado por</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in visibles" :key="p.id">
            <td>{{ p.ruc }}</td>
            <td>{{ p.razonSocial }}</td>
            <td>{{ p.nombreComercial || '—' }}</td>
            <td><span :class="claseEstado(p.estado)">{{ etiquetaEstado(p.estado) }}</span></td>
            <td>{{ p.creadoPor || '—' }}</td>
            <td class="acciones">
              <RouterLink :to="`/proveedores/${p.id}/editar`">Editar</RouterLink>
              <BotonAccion
                :texto="p.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'"
                :varianta="p.estado === 'ACTIVO' ? 'warn' : 'primary'"
                :cargando="confirmandoId === p.id"
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
