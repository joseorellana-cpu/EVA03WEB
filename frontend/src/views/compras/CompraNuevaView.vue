<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { proveedorApi } from '@/api/proveedorApi'
import { productoApi } from '@/api/productoApi'
import { compraApi } from '@/api/compraApi'
import { parsearMonto, sumarMontoCentavos, subtotal, formatoMonto, formatoApi } from '@/utils/money'
import { validarCantidad, longitudOpcional, validarNumeroCompra } from '@/utils/validators'
import { numeroCompraPorDefecto } from '@/utils/codigos'
import FormErroresServidor from '@/components/FormErroresServidor.vue'
import BotonAccion from '@/components/BotonAccion.vue'

const router = useRouter()

const proveedores = ref([])
const cargando = ref(true)
const errorCarga = ref(null)

const numeroCompra = ref('')
const errorNumero = ref(null)
const proveedorId = ref('')
const observaciones = ref('')
const errorObservaciones = ref(null)

// Productos del proveedor seleccionado (ACTIVO y con stock)
const productos = ref([])
const cargandoProductos = ref(false)

// Líneas del pedido: { productoId, cantidad (string), precioUnitario (string) }
const lineas = ref([])

// ids de línea para key estable
let contadorLinea = 0
const nuevaLinea = () => ({ __id: ++contadorLinea, productoId: '', cantidad: '', precioUnitario: '' })

const enviando = ref(false)
const errorServidor = ref(null)

onMounted(async () => {
  try {
    const provs = await proveedorApi.listar()
    proveedores.value = provs.filter((p) => p.estado === 'ACTIVO')
  } catch (e) {
    errorCarga.value = e
  } finally {
    cargando.value = false
  }
  // Número por defecto (editable). Si no se puede consultar, el campo queda vacío.
  try {
    const compras = await compraApi.listar()
    if (!numeroCompra.value) numeroCompra.value = numeroCompraPorDefecto(compras)
  } catch {
    /* sin sugerencia: el usuario lo escribe */
  }
})

async function alCambiarProveedor() {
  if (!proveedorId.value) {
    productos.value = []
    lineas.value = []
    return
  }
  cargandoProductos.value = true
  errorServidor.value = null
  try {
    const lista = await productoApi.listarPorProveedor(proveedorId.value)
    productos.value = lista.filter((p) => p.estado === 'ACTIVO' && p.stock > 0)
    lineas.value = [nuevaLinea()]
  } catch (e) {
    errorServidor.value = e
    productos.value = []
  } finally {
    cargandoProductos.value = false
  }
}

watch(proveedorId, alCambiarProveedor)

function agregarLinea() {
  if (!proveedorId.value) return
  lineas.value.push(nuevaLinea())
}

function quitarLinea(index) {
  lineas.value.splice(index, 1)
}

function productosRestantes(omitirIdx = -1) {
  const usados = new Set(lineas.value.filter((_, i) => i !== omitirIdx).map((l) => l.productoId).filter(Boolean))
  return productos.value.filter((p) => !usados.has(String(p.id)))
}

// Total ESTIMADO en centavos (nunca se envía al servidor)
const totalEstimadoCentavos = computed(() => {
  let total = 0
  for (const l of lineas.value) {
    if (!l.productoId || !l.cantidad || !l.precioUnitario) continue
    const cant = Number(l.cantidad)
    const monto = parsearMonto(l.precioUnitario)
    if (!monto.ok || !Number.isInteger(cant) || cant < 1) continue
    total = sumarMontoCentavos([total, subtotal(cant, monto.centavos)])
  }
  return total
})

const totalEstimadoTexto = computed(() => formatoMonto(totalEstimadoCentavos.value))

function validarFormulario() {
  errorServidor.value = null
  errorObservaciones.value = null
  const obs = longitudOpcional(observaciones.value, 300, 'las observaciones')
  if (!obs.ok) errorObservaciones.value = obs.mensaje
  if (!proveedorId.value) {
    errorServidor.value = {
      message: 'Seleccione un proveedor',
      details: ['proveedorId: el proveedorId es obligatorio'],
    }
  }
  if (errorObservaciones.value) {
    errorServidor.value = { message: 'Revise los campos del formulario', details: [errorObservaciones.value] }
  }
  const num = validarNumeroCompra(numeroCompra.value)
  errorNumero.value = num.ok ? null : num.mensaje
  if (!num.ok) {
    errorServidor.value = { message: 'Revise los campos del formulario', details: [num.mensaje] }
    return false
  }
  // repetidos — bloquea el doble envío del mismo producto
  const vistos = new Set()
  for (const l of lineas.value) {
    if (!l.productoId) continue
    if (vistos.has(String(l.productoId))) {
      errorServidor.value = {
        message: 'No se puede repetir el mismo producto en una compra',
        details: ['productoId: el producto ya está en otro detalle'],
      }
      return false
    }
    vistos.add(String(l.productoId))
  }
  if (!lineas.value.length) {
    errorServidor.value = { message: 'La compra debe tener al menos un detalle', details: ['detalles: al menos un detalle'] }
    return false
  }
  for (const l of lineas.value) {
    if (!l.productoId) {
      errorServidor.value = { message: 'Cada línea necesita un producto', details: ['productoId: el productoId es obligatorio'] }
      return false
    }
    const vCant = validarCantidad(l.cantidad)
    if (!vCant.ok) {
      errorServidor.value = { message: 'Cantidad inválida', details: ['cantidad: ' + vCant.mensaje] }
      return false
    }
    const vPrecio = parsearMonto(l.precioUnitario)
    if (!vPrecio.ok || vPrecio.centavos <= 0) {
      errorServidor.value = {
        message: 'Precio inválido',
        details: ['precioUnitario: debe ser mayor que cero con hasta 2 decimales'],
      }
      return false
    }
  }
  return !errorServidor.value
}

async function guardar() {
  if (!validarFormulario()) return
  enviando.value = true
  try {
    const cuerpo = {
      numeroCompra: numeroCompra.value.trim().toUpperCase(),
      proveedorId: Number(proveedorId.value),
      observaciones: observaciones.value?.trim() || null,
      detalles: lineas.value.map((l) => ({
        productoId: Number(l.productoId),
        cantidad: Number(l.cantidad),
        precioUnitario: formatoApi(parsearMonto(l.precioUnitario).centavos),
      })),
    }
    // NUNCA se envía total, estado ni creadoPor: el backend es la autoridad.
    const creada = await compraApi.crear(cuerpo)
    router.push({ name: 'compras-detalle', params: { id: creada.id } })
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
      <h1>Nueva compra</h1>
      <RouterLink to="/compras" class="btn">Volver</RouterLink>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando proveedores…</p>
    <p v-else-if="errorCarga" class="alerta alerta--error" role="alert">{{ errorCarga.message }}</p>

    <form v-else class="formulario formulario--ancho" novalidate @submit.prevent="guardar">
      <div class="campo">
        <label for="numeroCompra">Número de compra *</label>
        <input id="numeroCompra" v-model.trim="numeroCompra" maxlength="20" required placeholder="Ej. C-2026-000010"
          :aria-invalid="!!errorNumero" :aria-describedby="errorNumero ? 'numero-error' : 'numero-ayuda'"
          @input="errorNumero = null" />
        <small v-if="errorNumero" id="numero-error" class="campo__error" role="alert">{{ errorNumero }}</small>
        <small v-else id="numero-ayuda" class="campo__ayuda">Viene sugerido el siguiente número; puedes cambiarlo. Letras, números y guiones, sin repetir.</small>
      </div>

      <div class="campo">
        <label for="proveedorId">Proveedor (ACTIVO) *</label>
        <select id="proveedorId" v-model="proveedorId" required>
          <option value="" disabled>Seleccionar…</option>
          <option v-for="p in proveedores" :key="p.id" :value="p.id">{{ p.nombreComercial || p.razonSocial }} ({{ p.id }})</option>
        </select>
      </div>

      <div class="campo">
        <label for="observaciones">Observaciones</label>
        <input id="observaciones" v-model.trim="observaciones" maxlength="300"
          :aria-invalid="!!errorObservaciones" />
        <small v-if="errorObservaciones" class="campo__error" role="alert">{{ errorObservaciones }}</small>
      </div>

      <template v-if="proveedorId">
        <h2>Detalles</h2>
        <p v-if="cargandoProductos" class="estado-vacio">Cargando productos del proveedor…</p>

        <div v-else>
          <table class="tabla tabla--linea">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Stock</th>
                <th>Cantidad</th>
                <th>Precio unitario</th>
                <th>Subtotal (est.)</th>
                <th></th>
              </tr>
            </thead>
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
              <tr v-if="!lineas.length"><td colspan="6" class="tabla__vacio">Sin detalles. Agregue una línea.</td></tr>
            </tbody>
          </table>

          <button type="button" class="btn" @click="agregarLinea">+ Agregar producto</button>
        </div>
      </template>

      <div class="total-estimado">
        <span>Total <strong>est.</strong>:</span>
        <strong class="total-estimado__valor">{{ totalEstimadoTexto }}</strong>
        <small>Estimado en el frontend (centavos). El total real lo devuelve el backend.</small>
      </div>

      <FormErroresServidor :error="errorServidor" />

      <div class="formulario__acciones">
        <BotonAccion texto="Crear compra" :cargando="enviando" />
      </div>
    </form>
  </section>
</template>
