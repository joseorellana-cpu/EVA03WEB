<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { proveedorApi } from '@/api/proveedorApi'
import { validarRuc, validarEmail, longitudObligatoria, longitudOpcional } from '@/utils/validators'
import FormErroresServidor from '@/components/FormErroresServidor.vue'
import BotonAccion from '@/components/BotonAccion.vue'

const ruta = useRoute()
const router = useRouter()
const esEdicion = computed(() => !!ruta.params.id)

const datos = reactive({
  ruc: '',
  razonSocial: '',
  nombreComercial: '',
  email: '',
  telefono: '',
  direccion: '',
})

const errores = reactive({})
const errorServidor = ref(null)
const enviando = ref(false)
const cargando = ref(true)

function limpiarCampo(campo) {
  delete errores[campo]
  errorServidor.value = null
}

async function cargar() {
  if (!esEdicion.value) {
    cargando.value = false
    return
  }
  cargando.value = true
  try {
    const p = await proveedorApi.obtener(ruta.params.id)
    Object.assign(datos, {
      ruc: p.ruc,
      razonSocial: p.razonSocial,
      nombreComercial: p.nombreComercial ?? '',
      email: p.email ?? '',
      telefono: p.telefono ?? '',
      direccion: p.direccion ?? '',
    })
  } catch (e) {
    errorServidor.value = e
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

function validarFormulario() {
  Object.keys(errores).forEach((k) => delete errores[k])
  const ruc = validarRuc(datos.ruc)
  if (!ruc.ok) errores.ruc = ruc.mensaje
  const rs = longitudObligatoria(datos.razonSocial, 150, 'la razón social')
  if (!rs.ok) errores.razonSocial = rs.mensaje
  const nc = longitudOpcional(datos.nombreComercial, 150, 'el nombre comercial')
  if (!nc.ok) errores.nombreComercial = nc.mensaje
  const em = validarEmail(datos.email)
  if (!em.ok) errores.email = em.mensaje
  const tf = longitudOpcional(datos.telefono, 20, 'el teléfono')
  if (!tf.ok) errores.telefono = tf.mensaje
  const dr = longitudOpcional(datos.direccion, 200, 'la dirección')
  if (!dr.ok) errores.direccion = dr.mensaje
  return Object.keys(errores).length === 0
}

async function guardar() {
  if (!validarFormulario()) return
  enviando.value = true
  errorServidor.value = null
  const cuerpo = { ...datos }
  try {
    if (esEdicion.value) {
      await proveedorApi.actualizar(ruta.params.id, cuerpo)
    } else {
      await proveedorApi.crear(cuerpo)
    }
    router.push({ name: 'proveedores' })
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
      <h1>{{ esEdicion ? 'Editar proveedor' : 'Nuevo proveedor' }}</h1>
      <RouterLink to="/proveedores" class="btn">Volver</RouterLink>
    </div>

    <p v-if="cargando" class="estado-vacio">Cargando…</p>

    <form v-else class="formulario" novalidate @submit.prevent="guardar">
      <div class="campo">
        <label for="ruc">RUC *</label>
        <input id="ruc" v-model.trim="datos.ruc" inputmode="numeric" maxlength="11" required
          :aria-invalid="!!errores.ruc" :aria-describedby="errores.ruc ? 'ruc-error' : undefined"
          @input="limpiarCampo('ruc')" />
        <small v-if="errores.ruc" id="ruc-error" class="campo__error" role="alert">{{ errores.ruc }}</small>
        <small v-else class="campo__ayuda">Exactamente 11 dígitos numéricos.</small>
      </div>

      <div class="campo">
        <label for="razonSocial">Razón social *</label>
        <input id="razonSocial" v-model.trim="datos.razonSocial" maxlength="150" required
          :aria-invalid="!!errores.razonSocial" :aria-describedby="errores.razonSocial ? 'rs-error' : undefined"
          @input="limpiarCampo('razonSocial')" />
        <small v-if="errores.razonSocial" id="rs-error" class="campo__error" role="alert">{{ errores.razonSocial }}</small>
      </div>

      <div class="campo">
        <label for="nombreComercial">Nombre comercial</label>
        <input id="nombreComercial" v-model.trim="datos.nombreComercial" maxlength="150"
          :aria-invalid="!!errores.nombreComercial" @input="limpiarCampo('nombreComercial')" />
        <small v-if="errores.nombreComercial" class="campo__error" role="alert">{{ errores.nombreComercial }}</small>
      </div>

      <div class="campo">
        <label for="email">Email</label>
        <input id="email" v-model.trim="datos.email" type="email" maxlength="120"
          :aria-invalid="!!errores.email" @input="limpiarCampo('email')" />
        <small v-if="errores.email" class="campo__error" role="alert">{{ errores.email }}</small>
      </div>

      <div class="campo">
        <label for="telefono">Teléfono</label>
        <input id="telefono" v-model.trim="datos.telefono" maxlength="20"
          :aria-invalid="!!errores.telefono" @input="limpiarCampo('telefono')" />
        <small v-if="errores.telefono" class="campo__error" role="alert">{{ errores.telefono }}</small>
      </div>

      <div class="campo">
        <label for="direccion">Dirección</label>
        <input id="direccion" v-model.trim="datos.direccion" maxlength="200"
          :aria-invalid="!!errores.direccion" @input="limpiarCampo('direccion')" />
        <small v-if="errores.direccion" class="campo__error" role="alert">{{ errores.direccion }}</small>
      </div>

      <FormErroresServidor :error="errorServidor" />

      <div class="formulario__acciones">
        <BotonAccion texto="Guardar" :cargando="enviando" />
      </div>
    </form>
  </section>
</template>
