<script setup>
import { computed } from 'vue'

/**
 * Campo de formulario con etiqueta asociada, error y aria-* correctos.
 */
const props = defineProps({
  id: { type: String, required: true, default: '' },
  etiqueta: { type: String, required: true, default: '' },
  error: { type: String, default: null },
  ayuda: { type: String, default: '' },
  requerido: { type: Boolean, default: false },
})

const model = defineModel({ type: [String, Number], default: '' })

const descritoPor = computed(() => [props.error ? `${props.id}-error` : null].filter(Boolean).join(' ') || undefined)
</script>

<template>
  <div class="campo">
    <label :for="id">
      {{ etiqueta }}<span v-if="requerido" aria-hidden="true"> *</span>
    </label>
    <input
      :id="id"
      v-model="model"
      :required="requerido"
      :aria-invalid="!!error"
      :aria-describedby="descritoPor"
      class="campo__input"
    />
    <small v-if="ayuda && !error" class="campo__ayuda">{{ ayuda }}</small>
    <small v-if="error" :id="`${id}-error`" class="campo__error" role="alert">{{ error }}</small>
  </div>
</template>
