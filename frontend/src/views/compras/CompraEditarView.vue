<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { proveedorApi } from '@/api/proveedorApi'
import { productoApi } from '@/api/productoApi'
import { compraApi } from '@/api/compraApi'
import { parsearMonto, subtotal, formatoMonto, formatoApi } from '@/utils/money'
import { validarCantidad, longitudOpcional } from '@/utils/validators'
import FormErroresServidor from '@/components/FormErroresServidor.vue'
import BotonAccion from '@/components/BotonAccion.vue'

const ruta = useRoute()
const router = useRouter()

const compra = ref(null)
const cargando = ref(true)
const error = ref(null)

const proveedores = ref([])
const productos = ref([])
const cargandoProductos = ref(false)

const proveedorId = ref('')
const observaciones = ref('')
const errorObservaciones = ref(null)

let contadorLinea = 0
const nuevaLinea = () => ({ __id: ++contadorLinea, productoId: '', cantidad: '', precioUnitario: '' })
const lineas = ref([])

const enviando = ref(false)
const errorServidor = ref(null)

onMounted(async () => {
  try {
    const c = await compraApi.obtener(ruta.params.id)
    compra.value = c
    proveedorId.value = c.proveedorId
    observaciones.value = c.observaciones ?? ''
    lineas.value = (c.detalles || []).map((d) => ({
      __id: ++contadorLinea,
      productoId: String(d.productoId),
      cantidad: String(d.cantidad),
      precioUnitario: String(d.precioUnitario),
    }))
    const provs = await proveedorApi.listar()
    proveedores.value = provs.filter((p) => p.estado === 'ACTIVO')
    await cargarProductos()
  } catch (e) {
    error.value = e
  } finally {
    cargando.value = false
  }
})

async function cargarProductos() {
  if (!proveedorId.value) {
    productos.value = []
    return
  }
  cargandoProductos.value = true
  try {
    const lista = await productoApi.listarPorProveedor(proveedorId.value)
    productos.value = lista.filter((p) => p.estado === 'ACTIVO' && p.stock > 0)
  } catch (e) {
    errorServidor.value = e
  } finally {
    cargandoProductos.value = false
  }
}

watch(proveedorId, async () => {
  lineas.value = []
  await cargarProductos()
})

function agregarLinea() {
  lineas.value.push(nuevaLinea())
}
function quitarLinea(i) {
  lineas.value.splice(i, 1)
}

function productosRestantes(omitirIdx = -1) {
  const usados = new Set(lineas.value.filter((_, i) => i !== omitirIdx).map((l) => l.productoId).filter(Boolean))
  return productos.value.filter((p) => !usados.has(String(p.id)))
}

const totalEstimadoCentavos = computed(() => {
  let total = 0
  for (const l of lineas.value) {
    if (!l.productoId || !l.cantidad || !l.precioUnitario) continue
    const cant = Number(l.cantidad)
    const monto = parsearMonto(l.precioUnitario)
    if (!monto.ok || !Number.isInteger(cant) || cant < 1) continue
    total += subtotal(cant, monto.centavos)
  }
  return total
})

function validarFormulario() {
  errorServidor.value = null
  const obs = longitudOpcional(observaciones.value, 300, 'las observaciones')
  if (!obs.ok) {
    errorObservaciones.value = obs.mensaje
    errorServidor.value = { message: 'Revise los campos', details: [obs.mensaje] }
    return false
  }
  errorObservaciones.value = null
  const vistos = new Set()
  for (const l of lineas.value) {
    if (!l.productoId) {
      errorServidor.value = { message: 'Cada línea necesita un producto', details: ['productoId: el productoId es obligatorio'] }
      return false
    }
    if (vistos.has(String(l.productoId))) {
      errorServidor.value = { message: 'Producto repetido', details: ['productoId: no se puede repetir el mismo producto'] }
      return false
    }
    vistos.add(String(l.productoId))
    const vCant = validarCantidad(l.cantidad)
    if (!vCant.ok) {
      errorServidor.value = { message: 'Cantidad inválida', details: ['cantidad: ' + vCant.mensaje] }
      return false
    }
    const vPrecio = parsearMonto(l.precioUnitario)
    if (!vPrecio.ok || vPrecio.centavos <= 0) {
      errorServidor.value = { message: 'Precio inválido', details: ['precioUnitario: debe ser mayor que cero con hasta 2 decimales'] }
      return false
    }
  }
  if (!lineas.value.length) {
    errorServidor.value = { message: 'La compra debe tener al menos un detalle', details: ['detalles: al menos un detalle'] }
    return false
  }
  return true
}

async function guardar() {
  if (compra.value?.estado !== 'REGISTRADA') {
    errorServidor.value = {
      message: 'Solo una compra REGISTRADA puede modificarse',
      details: ['estado: la compra está ' + (compra.value?.estado ?? '?')],
    }
    return
  }
  if (!validarFormulario()) return
  enviando.value = true
  try {
    const cuerpo = {
      proveedorId: Number(proveedorId.value),
      observaciones: observaciones.value?.trim() || null,
      detalles: lineas.value.map((l) => ({
        productoId: Number(l.productoId),
        cantidad: Number(l.cantidad),
        precioUnitario: formatoApi(parsearMonto(l.precioUnitario).centavos),
      })),
    }
    await compraApi.actualizar(compra.value.id, cuerpo)
    router.push({ name: 'compras-detalle', params: { id: compra.value.id } })
  } catch (e) {
    errorServidor.value = e
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <section class="pagina">
    <div class="pagina__encabezado">
      <h1>Editar compra</h1>
      <RouterLink :to="`/compras/${compra?.id ?? ruta.params.id}`" class="btn">Volver</RouterLink>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando…</p>
    <p v-else-if="error" class="alerta alerta--error" role="alert">{{ error.message }}</p>
    <p v-else-if="compra?.estado !== 'REGISTRADA'" class="alerta alerta--warn" role="alert">
      Solo una compra REGISTRADA puede modificarse (estado actual: {{ compra?.estado ?? '—' }}).
    </p>

    <form v-else class="formulario formulario--ancho" novalidate @submit.prevent="guardar">
      <div class="campo">
        <label for="proveedorId">Proveedor (ACTIVO) *</label>
        <select id="proveedorId" v-model="proveedorId" required>
          <option v-for="p in proveedores" :key="p.id" :value="p.id">{{ p.nombreComercial || p.razonSocial }} ({{ p.id }})</option>
        </select>
      </div>

      <div class="campo">
        <label for="observaciones">Observaciones</label>
        <input id="observaciones" v-model.trim="observaciones" maxlength="300" :aria-invalid="!!errorObservaciones" />
        <small v-if="errorObservaciones" class="campo__error" role="alert">{{ errorObservaciones }}</small>
      </div>

      <h2>Detalles</h2>
      <p v-if="cargandoProductos" class="estado-vacio">Cargando productos…</p>
      <div v-else>
        <table class="tabla tabla--linea">
          <thead><tr><th>Producto</th><th>Stock</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal (est.)</th><th></th></tr></thead>
          <tbody>
            <tr v-for="(l, idx) in lineas" :key="l.__id">
              <td>
                <select v-model="l.productoId" required>
                  <option value="" disabled>Seleccionar…</option>
                  <option v-for="prod in productosRestantes(idx)" :key="prod.id" :value="prod.id">
                    {{ prod.codigo }} — {{ prod.nombre }} (stock {{ prod.stock }})
                  </option>
                </select>
              </td>
              <td>{{ productos.find((p) => String(p.id) === String(l.productoId))?.stock ?? '—' }}</td>
              <td><input v-model.trim="l.cantidad" inputmode="numeric" /></td>
              <td><input v-model.trim="l.precioUnitario" /></td>
              <td>
                {{
                  l.productoId && l.cantidad && l.precioUnitario && parsearMonto(l.precioUnitario).ok && Number.isInteger(Number(l.cantidad))
                    ? formatoMonto(subtotal(Number(l.cantidad), parsearMonto(l.precioUnitario).centavos))
                    : '—'
                }}
              </td>
              <td><button type="button" class="btn btn--sm" @click="quitarLinea(idx)">Quitar</button></td>
            </tr>
            <tr v-if="!lineas.length"><td colspan="6" class="tabla__vacio">Sin detalles.</td></tr>
          </tbody>
        </table>
        <button type="button" class="btn" @click="agregarLinea">+ Agregar producto</button>
      </div>

      <div class="total-estimado">
        <span>Total <strong>est.</strong>:</span>
        <strong class="total-estimado__valor">{{ formatoMonto(totalEstimadoCentavos) }}</strong>
        <small>Estimado en el frontend. El total real lo devuelve el backend.</small>
      </div>

      <FormErroresServidor :error="errorServidor" />

      <div class="formulario__acciones">
        <BotonAccion texto="Guardar cambios" :cargando="enviando" />
      </div>
    </form>
  </section>
</template>
