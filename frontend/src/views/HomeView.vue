<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { productoApi } from '@/api/productoApi'
import { proveedorApi } from '@/api/proveedorApi'
import { soles } from '@/utils/formatters'

const productos = ref([])
const proveedores = ref([])
const cargando = ref(true)
const error = ref(null)
const buscando = ref('')
const categoria = ref('')

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [ps, pr] = await Promise.all([productoApi.listar(), proveedorApi.listar()])
    productos.value = (ps ?? []).filter((p) => p.estado !== 'INACTIVO')
    proveedores.value = pr ?? []
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

const nombreProveedor = (id) => {
  const p = proveedores.value.find((x) => String(x.id) === String(id))
  return p ? p.nombreComercial || p.razonSocial : null
}

const categorias = computed(() =>
  [...new Set(productos.value.map((p) => p.categoria).filter(Boolean))].sort(),
)

const visibles = computed(() => {
  const texto = buscando.value.trim().toUpperCase()
  return productos.value.filter((p) => {
    if (categoria.value && p.categoria !== categoria.value) return false
    if (!texto) return true
    return [p.codigo, p.nombre, p.categoria].filter(Boolean).join(' ').toUpperCase().includes(texto)
  })
})

const inicial = (nombre) => String(nombre ?? '?').trim().charAt(0).toUpperCase() || '?'

// Color estable por categoría para la "foto" de la tarjeta
const TONOS = ['#ffe8d6', '#d8f3dc', '#dbeafe', '#fde2e4', '#ede9fe', '#fef3c7', '#cffafe']
const tono = (texto) => {
  let h = 0
  for (const c of String(texto ?? '')) h = (h * 31 + c.charCodeAt(0)) >>> 0
  return TONOS[h % TONOS.length]
}
</script>

<template>
  <section class="pagina">
    <div class="hero">
      <div class="hero__texto">
        <p class="hero__eyebrow">Bienvenido</p>
        <h1>Todo lo que necesitas, en un solo lugar</h1>
        <p>Explora nuestro catálogo, revisa el stock disponible y registra compras a nuestros proveedores.</p>
        <div class="hero__acciones">
          <a href="#catalogo" class="btn btn--primary">Ver catálogo</a>
          <RouterLink to="/proveedores" class="btn">Nuestros proveedores</RouterLink>
        </div>
      </div>
      <div class="hero__stats" v-if="!cargando && !error">
        <div><strong>{{ productos.length }}</strong><span>productos</span></div>
        <div><strong>{{ categorias.length }}</strong><span>categorías</span></div>
        <div><strong>{{ proveedores.length }}</strong><span>proveedores</span></div>
      </div>
    </div>

    <div id="catalogo" class="catalogo__barra">
      <h2>Catálogo</h2>
      <input v-model="buscando" type="search" class="buscador" placeholder="Buscar productos…" aria-label="Buscar productos" />
    </div>

    <div v-if="categorias.length" class="chips" role="group" aria-label="Filtrar por categoría">
      <button type="button" class="chip" :class="{ 'chip--activo': !categoria }" @click="categoria = ''">Todas</button>
      <button
        v-for="c in categorias"
        :key="c"
        type="button"
        class="chip"
        :class="{ 'chip--activo': categoria === c }"
        @click="categoria = c"
      >
        {{ c }}
      </button>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando productos…</p>
    <div v-else-if="error" class="alerta alerta--error" role="alert">
      {{ error.message }}
      <button type="button" class="btn btn--sm" @click="cargar">Reintentar</button>
    </div>
    <p v-else-if="!visibles.length" class="estado-vacio">No encontramos productos con ese criterio.</p>

    <div v-else class="productos-grid">
      <article v-for="p in visibles" :key="p.id" class="producto">
        <div class="producto__foto" :style="{ background: tono(p.categoria || p.nombre) }">
          <span>{{ inicial(p.nombre) }}</span>
          <span v-if="p.stock <= 0" class="producto__etiqueta producto__etiqueta--agotado">Agotado</span>
          <span v-else-if="p.bajoStockMinimo" class="producto__etiqueta">¡Últimas unidades!</span>
        </div>
        <div class="producto__cuerpo">
          <small class="producto__cat">{{ p.categoria || 'General' }}</small>
          <h3 class="producto__nombre">{{ p.nombre }}</h3>
          <small v-if="nombreProveedor(p.proveedorId)" class="producto__prov">por {{ nombreProveedor(p.proveedorId) }}</small>
          <div class="producto__pie">
            <span class="producto__precio">{{ soles(p.precio) }}</span>
            <span class="producto__stock">{{ p.stock }} en stock</span>
          </div>
          <div class="producto__acciones">
            <RouterLink to="/compras/nueva" class="btn btn--primary btn--sm">Pedir</RouterLink>
            <RouterLink :to="`/productos/${p.id}/movimientos`" class="btn btn--sm">Historial</RouterLink>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
