<script setup>
import { computed, ref } from 'vue'

/**
 * Botón de acción que usa el guardián de doble envío (C6):
 * evita lanzar una segunda petición mientras la primera sigue abierta.
 *
 * AVISO: esto NO es protección suficiente contra duplicados; la
 * idempotencia real debe garantizarla el servidor. Es UX + disciplina.
 */
const props = defineProps({
  texto: { type: String, default: 'Aceptar' },
  cargando: { type: Boolean, default: false },
  varianta: { type: String, default: 'primary' },
  bloqueado: { type: Boolean, default: false },
})

const emit = defineEmits(['accion'])
const enCursoLocal = ref(false)
const enCurso = computed(() => props.cargando || enCursoLocal.value || props.bloqueado)

async function pulsar() {
  if (enCurso.value) return
  enCursoLocal.value = true
  try {
    emit('accion')
  } finally {
    enCursoLocal.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="btn"
    :class="`btn--${varianta}`"
    :disabled="enCurso"
    :aria-busy="enCurso"
    @click="pulsar"
  >
    <slot>{{ texto }}</slot><span v-if="enCurso">…</span>
  </button>
</template>
