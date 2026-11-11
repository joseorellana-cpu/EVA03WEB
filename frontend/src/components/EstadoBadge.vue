<script setup>
import { computed } from 'vue'
import { etiquetaEstado, claseEstado } from '@/utils/formatters'

const props = defineProps({
  estado: { type: String, default: '' },
})

const etiqueta = computed(() => etiquetaEstado(props.estado))
const clase = computed(() => claseEstado(props.estado))
</script>

<template>
  <span :class="clase">{{ etiqueta }}</span>
</template>
