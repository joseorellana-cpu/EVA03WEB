<script setup>
/**
 * Muestra un error normalizado (ErrorApi) como TEXTO plano.
 * Nunca renderiza detalles en bruto ni objetos de red (C7).
 */
defineProps({
  error: { type: Object, default: null },
  titulo: { type: String, default: 'Ha ocurrido un problema' },
})
</script>

<template>
  <div v-if="error" class="alerta alerta--error" role="alert">
    <strong>{{ titulo }}</strong>
    <p class="alerta__mensaje">{{ error.message }}</p>
    <ul v-if="error.details?.length" class="alerta__detalles">
      <li v-for="(d, i) in error.details" :key="i">{{ d }}</li>
    </ul>
    <p v-if="error.status" class="alerta__codigo">Código: {{ error.status }}</p>
    <p v-if="error.esServicioNoDisponible" class="alerta__nota">
      El servicio no está disponible. Verifique que el Gateway y los microservicios estén en ejecución.
    </p>
  </div>
</template>
