import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Notificaciones de la aplicación: sólo TEXTO plano (nunca HTML).
 * El componente que las renderiza usa interpolación {{ }} y nunca v-html.
 */
export const useNotificacionesStore = defineStore('notificaciones', () => {
  const mensajes = ref([])
  let secuencia = 0

  function push(tipo, texto) {
    if (!texto) return
    mensajes.value.push({ id: ++secuencia, tipo, texto: String(texto) })
  }
  const ok = (texto) => push('ok', texto)
  const error = (texto) => push('error', texto)
  const info = (texto) => push('info', texto)
  function quitar(id) {
    mensajes.value = mensajes.value.filter((m) => m.id !== id)
  }
  function limpiar() {
    mensajes.value = []
  }

  return { mensajes, ok, error, info, quitar, limpiar }
})
