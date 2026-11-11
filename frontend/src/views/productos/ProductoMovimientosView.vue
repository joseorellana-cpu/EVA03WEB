<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { productoApi } from '@/api/productoApi'
import { formatearFecha } from '@/utils/formatters'
import { validarCantidad } from '@/utils/validators'
import FormErroresServidor from '@/components/FormErroresServidor.vue'
import BotonAccion from '@/components/BotonAccion.vue'

const ruta = useRoute()
const producto = ref(null)
const movimientos = ref([])
const cargando = ref(true)
const error = ref(null)
const enviando = ref(false)
const errorAjuste = ref(null)

const ajuste = ref({ cantidad: '', operacion: 'DESCONTAR', motivo: '' })
const errorCantidad = ref(null)

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [p, ms] = await Promise.all([productoApi.obtener(ruta.params.id), productoApi.listarMovimientos(ruta.params.id)])
    producto.value = p
    movimientos.value = ms
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

async function ajustar() {
  const v = validarCantidad(ajuste.value.cantidad)
  errorCantidad.value = v.ok ? null : v.mensaje
  if (!v.ok) return
  enviando.value = true
  errorAjuste.value = null
  try {
    const r = await productoApi.ajustarStock(ruta.params.id, Number(ajuste.value.cantidad), ajuste.value.operacion, ajuste.value.motivo || null)
    if (producto.value) producto.value.stock = r?.stock ?? producto.value.stock
    await cargar()
    ajuste.value = { cantidad: '', operacion: 'DESCONTAR', motivo: '' }
  } catch (e) {
    errorAjuste.value = e
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <section class="pagina">
    <div class="pagina__encabezado">
      <h1>Movimientos de stock</h1>
      <RouterLink to="/productos" class="btn">Volver a productos</RouterLink>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando…</p>
    <p v-else-if="error" class="alerta alerta--error" role="alert">{{ error.message }}</p>

    <template v-else>
      <p class="resumen">
        {{ producto.codigo }} — {{ producto.nombre }} ·
        stock actual: <strong>{{ producto.stock }}</strong> ·
        stock mínimo: {{ producto.stockMinimo }}
      </p>

      <form class="formulario formulario--inline" @submit.prevent="ajustar">
        <div class="campo">
          <label for="cantidad">Cantidad *</label>
          <input id="cantidad" v-model.trim="ajuste.cantidad" inputmode="numeric" required
            :aria-invalid="!!errorCantidad" />
          <small v-if="errorCantidad" class="campo__error" role="alert">{{ errorCantidad }}</small>
        </div>
        <div class="campo">
          <label for="operacion">Operación</label>
          <select id="operacion" v-model="ajuste.operacion">
            <option value="DESCONTAR">DESCONTAR</option>
            <option value="INCREMENTAR">INCREMENTAR</option>
          </select>
        </div>
        <div class="campo">
          <label for="motivo">Motivo</label>
          <input id="motivo" v-model.trim="ajuste.motivo" maxlength="300" />
        </div>
        <BotonAccion texto="Aplicar ajuste" :cargando="enviando" />
      </form>

      <FormErroresServidor :error="errorAjuste" />

      <div class="tabla-wrap">
        <table class="tabla">
          <thead>
            <tr><th>Fecha</th><th>Operación</th><th>Cantidad</th><th>Ant.</th><th>Nuevo</th><th>Usuario</th><th>Motivo</th></tr>
          </thead>
          <tbody>
            <tr v-for="(m, i) in movimientos" :key="m.id ?? i">
              <td>{{ formatearFecha(m.fecha) }}</td>
              <td>{{ m.operacion }}</td>
              <td>{{ m.cantidad }}</td>
              <td>{{ m.stockAnterior }}</td>
              <td>{{ m.stockNuevo }}</td>
              <td>{{ m.usuario }}</td>
              <td>{{ m.motivo || '—' }}</td>
            </tr>
            <tr v-if="!movimientos.length"><td colspan="7" class="tabla__vacio">Sin movimientos.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>
