<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useNotificacionesStore } from '@/stores/notificacionesStore'

const nots = useNotificacionesStore()
const anio = new Date().getFullYear()
</script>

<template>
  <div class="app-shell">
    <div class="franja">Catálogo actualizado al momento · Precios en soles (S/)</div>

    <header class="topbar">
      <RouterLink to="/" class="brand">
        <span class="brand__mark" aria-hidden="true">🛒</span>
        <span class="brand__text">
          <strong>La Tiendita</strong>
          <small>Productos y proveedores</small>
        </span>
      </RouterLink>

      <nav class="nav" aria-label="Navegación principal">
        <RouterLink to="/" exact-active-class="activo">Tienda</RouterLink>
        <RouterLink to="/productos" active-class="activo">Productos</RouterLink>
        <RouterLink to="/proveedores" active-class="activo">Proveedores</RouterLink>
        <RouterLink to="/compras" active-class="activo">Compras</RouterLink>
      </nav>

      <RouterLink to="/compras/nueva" class="btn btn--primary topbar__cta">+ Nueva compra</RouterLink>
    </header>

    <aside v-if="nots.mensajes.length" class="toasts" aria-live="polite">
      <div v-for="m in nots.mensajes" :key="m.id" class="toast" :class="`toast--${m.tipo}`" role="status">
        <span class="toast__texto">{{ m.texto }}</span>
        <button type="button" class="toast__cerrar" aria-label="Descartar aviso" @click="nots.quitar(m.id)">×</button>
      </div>
    </aside>

    <main class="contenido">
      <RouterView />
    </main>

    <footer class="pie">
      <span>© {{ anio }} La Tiendita</span>
      <span>Hecho con Vue · Microservicios</span>
    </footer>
  </div>
</template>
