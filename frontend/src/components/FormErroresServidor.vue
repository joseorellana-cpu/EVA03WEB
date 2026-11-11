<script setup>
/**
 * Muestra los detalles del ErrorApi como LISTA DE TEXTO (details: ["campo: motivo"]).
 * Es el lugar donde el usuario ve qué campo corrigió el backend (400).
 */
defineProps({
  error: { type: Object, default: null },
  titulo: { type: String, default: 'El servidor respondió con errores de validación' },
})
</script>

<template>
  <div v-if="error" class="alerta alerta--warn" role="alert">
    <strong>{{ titulo }}</strong>
    <p v-if="error.message" class="alerta__mensaje">{{ error.message }}</p>
    <ul v-if="error.details?.length" class="alerta__detalles">
      <li v-for="(d, i) in error.details" :key="i">{{ d }}</li>
    </ul>
  </div>
</template>
