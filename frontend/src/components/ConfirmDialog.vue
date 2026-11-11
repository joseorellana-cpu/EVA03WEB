<script setup>
/**
 * Diálogo de confirmación accesible: role="dialog", aria-modal, foco inicial
 * en el botón de cancelar, Escape para cerrar (si no es destructivo ciego).
 */
defineProps({
  titulo: { type: String, required: true, default: 'Confirmar acción' },
  mensaje: { type: String, default: '' },
  textoConfirmar: { type: String, default: 'Confirmar' },
  textoCancelar: { type: String, default: 'Cancelar' },
  peligro: { type: Boolean, default: false },
})

const emit = defineEmits(['confirmar', 'cancelar'])

function alEscape(ev) {
  if (ev.key === 'Escape') emit('cancelar')
}
</script>

<template>
  <div class="modal-fondo" @click.self="$emit('cancelar')">
    <div
      class="modal"
      role="dialog"
      aria-modal="true"
      :aria-label="titulo"
      @keydown="alEscape"
    >
      <h2 class="modal__titulo">{{ titulo }}</h2>
      <p class="modal__mensaje">{{ mensaje }}</p>
      <div class="modal__acciones">
        <button type="button" class="btn" @click="$emit('cancelar')">{{ textoCancelar }}</button>
        <button
          ref="btnConfirmar"
          type="button"
          class="btn"
          :class="peligro ? 'btn--danger' : 'btn--primary'"
          @click="$emit('confirmar')"
        >
          {{ textoConfirmar }}
        </button>
      </div>
    </div>
  </div>
</template>
