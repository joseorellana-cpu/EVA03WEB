<script setup>
import { computed, ref, watch } from 'vue'
import BotonAccion from './BotonAccion.vue'

/**
 * Tabla genérica de datos con ordenación y paginación EN EL CLIENTE.
 * - `columnas`: [{ key, label, sortable?: boolean }]
 * - `filas`: array de objetos
 * - `idKey`: propiedad de id estable (defecto 'id')
 * - Paginación local: 10 filas por página por defecto.
 * - Todo valor se renderiza con interpolación {{ }} (texto, C1).
 */
const props = defineProps({
  columnas: { type: Array, required: true, default: () => [] },
  filas: { type: Array, default: () => [] },
  titulo: { type: String, default: '' },
  vacio: { type: String, default: 'Sin resultados' },
})

const emit = defineEmits(['fila:seleccion', 'fila:editar', 'fila:eliminar', 'fila:accion'])

const pagina = ref(1)
const filasPorPagina = ref(10)
const columnaOrden = ref(null)
const direccion = ref('asc')

const filasOrdenadas = computed(() => {
  if (!columnaOrden.value) return props.filas
  const k = columnaOrden.value
  const dir = direccion.value === 'asc' ? 1 : -1
  return [...props.filas].sort((a, b) => {
    const av = a[k]; const bv = b[k]
    if (av === null || av === undefined) return 1
    if (bv === null || bv === undefined) return -1
    if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir
    return String(av).localeCompare(String(bv), 'es', { numeric: true }) * dir
  })
})

const paginadas = computed(() => {
  const inicio = (pagina.value - 1) * filasPorPagina.value
  return filasOrdenadas.value.slice(inicio, inicio + filasPorPagina.value)
})

const totalPaginas = computed(() => Math.max(1, Math.ceil(filasOrdenadas.value.length / filasPorPagina.value)))

watch(
  () => props.filas,
  () => {
    pagina.value = 1
  },
)

function ordenar(key) {
  if (columnaOrden.value === key) {
    direccion.value = direccion.value === 'asc' ? 'desc' : 'asc'
  } else {
    columnaOrden.value = key
    direccion.value = 'asc'
  }
}

function anterior() {
  if (pagina.value > 1) pagina.value--
}
function siguiente() {
  if (pagina.value < totalPaginas.value) pagina.value++
}

function celda(fila, key) {
  const v = fila?.[key]
  return v === undefined || v === null || v === '' ? '—' : v
}
</script>

<template>
  <div class="tabla-wrap">
    <div v-if="titulo" class="tabla-meta">
      <strong>{{ titulo }}</strong>
      <span class="tabla-meta__conteo">{{ filas.length }} registro(s)</span>
    </div>

    <table class="tabla">
      <thead>
        <tr>
          <th v-for="col in columnas" :key="col.key" scope="col">
            <button
              v-if="col.sortable"
              type="button"
              class="tabla__ordenar"
              :aria-label="`Ordenar por ${col.label}`"
              @click="ordenar(col.key)"
            >
              {{ col.label }}<span v-if="columnaOrden === col.key">{{ direccion === 'asc' ? ' ▲' : ' ▼' }}</span>
            </button>
            <span v-else>{{ col.label }}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="fila in paginadas" :key="fila?.id ?? JSON.stringify(fila)">
          <td v-for="col in columnas" :key="col.key" :data-label="col.label">{{ celda(fila, col.key) }}</td>
        </tr>
        <tr v-if="!paginadas.length">
          <td :colspan="columnas.length || 1" class="tabla__vacio">{{ vacio }}</td>
        </tr>
      </tbody>
    </table>

    <div class="tabla-pager">
      <button type="button" :disabled="pagina <= 1" @click="anterior">← Anterior</button>
      <span>Pag. {{ pagina }} / {{ totalPaginas }}</span>
      <button type="button" :disabled="pagina >= totalPaginas" @click="siguiente">Siguiente →</button>
    </div>
  </div>
</template>
