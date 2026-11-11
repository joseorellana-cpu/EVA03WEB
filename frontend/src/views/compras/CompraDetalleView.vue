<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { compraApi } from '@/api/compraApi'
import { formatearFecha, etiquetaEstado, claseEstado } from '@/utils/formatters'
import { formatoMonto, parsearMonto } from '@/utils/money'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import BotonAccion from '@/components/BotonAccion.vue'

const ruta = useRoute()
const router = useRouter()
const compra = ref(null)
const auditoria = ref([])
const cargando = ref(true)
const error = ref(null)

const confirmando = ref(false)
const accion = ref(null)

async function cargar() {
  cargando.value = true
  error.value = null
  try {
    const [c, a] = await Promise.all([compraApi.obtener(ruta.params.id), compraApi.auditoria(ruta.params.id)])
    compra.value = c
    auditoria.value = a
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

const puedeEditar = computed(() => compra.value?.estado === 'REGISTRADA')
const puedeConfirmar = computed(() => compra.value?.estado === 'REGISTRADA')
const puedeAnular = computed(() => compra.value?.estado === 'CONFIRMADA' || compra.value?.estado === 'REGISTRADA')
const puedeEliminar = computed(() => compra.value?.estado === 'ANULADA')

function abrirConfirmar() {
  accion.value = 'confirmar'
  confirmando.value = true
}
function abrirAnular() {
  accion.value = 'anular'
  confirmando.value = true
}
function abrirEliminar() {
  accion.value = 'eliminar'
  confirmando.value = true
}
function cerrarConfirmar() {
  confirmando.value = false
  accion.value = null
}

async function alConfirmar() {
  confirmando.value = false
  try {
    if (accion.value === 'confirmar') {
      const r = await compraApi.confirmar(compra.value.id)
      compra.value = r ?? compra.value
    } else if (accion.value === 'anular') {
      const r = await compraApi.anular(compra.value.id)
      compra.value = r ?? compra.value
    } else if (accion.value === 'eliminar') {
      await compraApi.eliminar(compra.value.id)
      router.push({ name: 'compras' })
      return
    }
    await cargar()
  } catch (e) {
    error.value = e
  } finally {
    accion.value = null
  }
}
</script>

<template>
  <section class="pagina">
    <div class="pagina__encabezado">
      <h1>Detalle de compra</h1>
      <RouterLink to="/compras" class="btn">Volver</RouterLink>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando…</p>
    <p v-else-if="error" class="alerta alerta--error" role="alert">{{ error.message }}</p>

    <template v-else-if="compra">
      <div class="resumen">
        <p><strong>{{ compra.numeroCompra }}</strong> ·
          <span :class="claseEstado(compra.estado)">{{ etiquetaEstado(compra.estado) }}</span></p>
        <p>Proveedor: {{ compra.proveedorId }} · Fecha: {{ formatearFecha(compra.fecha) }}</p>
        <p>Total: <strong>{{ compra.total }}</strong> · Creado por: {{ compra.creadoPor ?? '—' }}</p>
        <p v-if="compra.observaciones">Observaciones: {{ compra.observaciones }}</p>
      </div>

      <div class="acciones-detalle">
        <RouterLink v-if="puedeEditar" class="btn btn--primary" :to="`/compras/${compra.id}/editar`">Editar</RouterLink>
        <BotonAccion v-if="puedeConfirmar" texto="Confirmar (descuenta stock)" @accion="abrirConfirmar" />
        <BotonAccion v-if="puedeAnular" texto="Anular (devuelve stock si estaba CONFIRMADA)" varianta="warn" @accion="abrirAnular" />
        <BotonAccion v-if="puedeEliminar" texto="Eliminar (solo si ANULADA)" varianta="danger" @accion="abrirEliminar" />
      </div>

      <h2>Detalles</h2>
      <div class="tabla-wrap">
        <table class="tabla">
          <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
          <tbody>
            <tr v-for="(d, i) in compra.detalles" :key="d.id ?? i">
              <td>{{ d.productoId }}</td>
              <td>{{ d.cantidad }}</td>
              <td>{{ d.precioUnitario }}</td>
              <td>{{ d.subtotal }}</td>
            </tr>
            <tr v-if="!compra.detalles?.length"><td colspan="4" class="tabla__vacio">Sin detalles.</td></tr>
          </tbody>
        </table>
      </div>

      <h2>Auditoría (quién hizo qué)</h2>
      <div class="tabla-wrap">
        <table class="tabla">
          <thead><tr><th>Fecha</th><th>Acción</th><th>Desde</th><th>Hasta</th><th>Usuario</th><th>Detalle</th></tr></thead>
          <tbody>
            <tr v-for="(a, i) in auditoria" :key="a.id ?? i">
              <td>{{ formatearFecha(a.fecha) }}</td>
              <td>{{ a.accion }}</td>
              <td>{{ a.estadoAnterior ?? '—' }}</td>
              <td>{{ a.estadoNuevo ?? '—' }}</td>
              <td>{{ a.usuario }}</td>
              <td>{{ a.detalle || '—' }}</td>
            </tr>
            <tr v-if="!auditoria.length"><td colspan="6" class="tabla__vacio">Sin auditoría.</td></tr>
          </tbody>
        </table>
      </div>
    </template>

    <ConfirmDialog
      v-if="confirmando"
      :titulo="accion === 'eliminar' ? 'Eliminar compra' : accion === 'confirmar' ? 'Confirmar compra' : 'Anular compra'"
      :mensaje="accion === 'eliminar' ? 'Esta acción elimina la compra ANULADA y no se puede deshacer.' : accion === 'confirmar' ? 'Se descontará el stock de los productos. Si falla, se compensará.' : 'La compra quedará ANULADA; si estaba CONFIRMADA se devolverá el stock.'"
      :texto-confirmar="accion === 'eliminar' ? 'Eliminar' : accion === 'confirmar' ? 'Confirmar' : 'Anular'"
      :peligro="accion === 'eliminar'"
      @confirmar="alConfirmar"
      @cancelar="cerrarConfirmar"
    />
  </section>
</template>
