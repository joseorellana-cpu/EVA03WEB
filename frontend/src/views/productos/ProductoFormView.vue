<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productoApi } from '@/api/productoApi'
import { proveedorApi } from '@/api/proveedorApi'
import { validarPrecio, validarEnteroNoNegativo, longitudObligatoria, longitudOpcional } from '@/utils/validators'
import { parsearMonto, formatoApi } from '@/utils/money'
import FormErroresServidor from '@/components/FormErroresServidor.vue'
import BotonAccion from '@/components/BotonAccion.vue'

const ruta = useRoute()
const router = useRouter()
const esEdicion = computed(() => !!ruta.params.id)

const proveedores = ref([])
const cargando = ref(true)
const errorServidor = ref(null)
const enviando = ref(false)
const errores = reactive({})

const datos = reactive({
  codigo: '',
  nombre: '',
  descripcion: '',
  precio: '',
  stock: '',
  stockMinimo: '',
  categoria: '',
  proveedorId: '',
})

function limpiar(campo) {
  delete errores[campo]
  errorServidor.value = null
}

async function cargar() {
  cargando.value = true
  errorServidor.value = null
  try {
    const provs = await proveedorApi.listar()
    proveedores.value = provs.filter((p) => p.estado === 'ACTIVO')
    if (esEdicion.value) {
      const p = await productoApi.obtener(ruta.params.id)
      Object.assign(datos, {
        codigo: p.codigo,
        nombre: p.nombre,
        descripcion: p.descripcion ?? '',
        precio: p.precio,
        stock: p.stock,
        stockMinimo: p.stockMinimo,
        categoria: p.categoria ?? '',
        proveedorId: p.proveedorId,
      })
    }
  } catch (e) {
    errorServidor.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

function validarFormulario() {
  Object.keys(errores).forEach((k) => delete errores[k])
  const cod = longitudObligatoria(datos.codigo, 30, 'el código')
  if (!cod.ok) errores.codigo = cod.mensaje
  const nom = longitudObligatoria(datos.nombre, 150, 'el nombre')
  if (!nom.ok) errores.nombre = nom.mensaje
  const des = longitudOpcional(datos.descripcion, 500, 'la descripción')
  if (!des.ok) errores.descripcion = des.mensaje
  const pr = validarPrecio(datos.precio)
  if (!pr.ok) errores.precio = pr.mensaje
  const st = validarEnteroNoNegativo(datos.stock, { campo: 'stock' })
  if (!st.ok) errores.stock = st.mensaje
  const sm = validarEnteroNoNegativo(datos.stockMinimo, { campo: 'stock mínimo' })
  if (!sm.ok) errores.stockMinimo = sm.mensaje
  const cat = longitudOpcional(datos.categoria, 80, 'la categoría')
  if (!cat.ok) errores.categoria = cat.mensaje
  if (!datos.proveedorId) errores.proveedorId = 'El proveedor es obligatorio'
  return Object.keys(errores).length === 0
}

async function guardar() {
  if (!validarFormulario()) return
  enviando.value = true
  errorServidor.value = null
  const precioParsed = parsearMonto(datos.precio)
  const cuerpo = { ...datos, precio: formatoApi(precioParsed.centavos) }
  try {
    if (esEdicion.value) {
      await productoApi.actualizar(ruta.params.id, cuerpo)
    } else {
      await productoApi.crear(cuerpo)
    }
    router.push({ name: 'productos' })
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
      <h1>{{ esEdicion ? 'Editar producto' : 'Nuevo producto' }}</h1>
      <RouterLink to="/productos" class="btn">Volver</RouterLink>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando…</p>

    <form v-else class="formulario" novalidate @submit.prevent="guardar">
      <div class="campo">
        <label for="codigo">Código *</label>
        <input id="codigo" v-model.trim="datos.codigo" maxlength="30" required
          :aria-invalid="!!errores.codigo" @input="limpiar('codigo')" />
        <small v-if="errores.codigo" class="campo__error" role="alert">{{ errores.codigo }}</small>
      </div>

      <div class="campo">
        <label for="nombre">Nombre *</label>
        <input id="nombre" v-model.trim="datos.nombre" maxlength="150" required
          :aria-invalid="!!errores.nombre" @input="limpiar('nombre')" />
        <small v-if="errores.nombre" class="campo__error" role="alert">{{ errores.nombre }}</small>
      </div>

      <div class="campo">
        <label for="descripcion">Descripción</label>
        <input id="descripcion" v-model.trim="datos.descripcion" maxlength="500"
          :aria-invalid="!!errores.descripcion" @input="limpiar('descripcion')" />
        <small v-if="errores.descripcion" class="campo__error" role="alert">{{ errores.descripcion }}</small>
      </div>

      <div class="campo">
        <label for="precio">Precio *</label>
        <input id="precio" v-model.trim="datos.precio" maxlength="20"
          :aria-invalid="!!errores.precio" @input="limpiar('precio')" />
        <small v-if="errores.precio" class="campo__error" role="alert">{{ errores.precio }}</small>
        <small v-else class="campo__ayuda">Precio de referencia del catálogo (mayor que cero, 2 decimales).</small>
      </div>

      <div class="campo">
        <label for="stock">Stock *</label>
        <input id="stock" v-model.trim="datos.stock" inputmode="numeric" required
          :aria-invalid="!!errores.stock" @input="limpiar('stock')" />
        <small v-if="errores.stock" class="campo__error" role="alert">{{ errores.stock }}</small>
      </div>

      <div class="campo">
        <label for="stockMinimo">Stock mínimo *</label>
        <input id="stockMinimo" v-model.trim="datos.stockMinimo" inputmode="numeric" required
          :aria-invalid="!!errores.stockMinimo" @input="limpiar('stockMinimo')" />
        <small v-if="errores.stockMinimo" class="campo__error" role="alert">{{ errores.stockMinimo }}</small>
      </div>

      <div class="campo">
        <label for="categoria">Categoría</label>
        <input id="categoria" v-model.trim="datos.categoria" maxlength="80"
          :aria-invalid="!!errores.categoria" @input="limpiar('categoria')" />
        <small v-if="errores.categoria" class="campo__error" role="alert">{{ errores.categoria }}</small>
      </div>

      <div class="campo">
        <label for="proveedorId">Proveedor (ACTIVO) *</label>
        <select id="proveedorId" v-model="datos.proveedorId" required
          :aria-invalid="!!errores.proveedorId" @change="limpiar('proveedorId')">
          <option value="" disabled>Seleccionar…</option>
          <option v-for="p in proveedores" :key="p.id" :value="p.id">{{ p.nombreComercial || p.razonSocial }} ({{ p.id }})</option>
        </select>
        <small v-if="errores.proveedorId" class="campo__error" role="alert">{{ errores.proveedorId }}</small>
      </div>

      <FormErroresServidor :error="errorServidor" />

      <div class="formulario__acciones">
        <BotonAccion texto="Guardar" :cargando="enviando" />
      </div>
    </form>
  </section>
</template>
