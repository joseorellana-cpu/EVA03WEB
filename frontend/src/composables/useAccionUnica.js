import { ref } from 'vue'

/**
 * Guardián de "petición en curso": deshabilita el botón mientras la
 * petición está abierta y evita lanzar una segunda llamada.
 *
 * AVISO DE SEGURIDAD (C6): deshabilitar el botón NO es una protección
 * completa — el usuario puede recargar la página, abrir otra pestaña
 * o forzar peticiones fuera de la UI. La idempotencia REAL debe
 * garantizarla el servidor (dedup por clave, o reglas de negocio).
 * Este guardián es UX + disciplina del cliente, no una barrera final.
 */
export function useAccionUnica() {
  const enCurso = ref(false)

  async function ejecutar(funcion) {
    if (enCurso.value) return undefined
    enCurso.value = true
    try {
      return await funcion()
    } finally {
      enCurso.value = false
    }
  }

  return { enCurso, ejecutar }
}
