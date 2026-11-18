import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CompraNuevaView from '@/views/compras/CompraNuevaView.vue'
import { proveedorApi } from '@/api/proveedorApi'
import { productoApi } from '@/api/productoApi'
import { compraApi } from '@/api/compraApi'

vi.mock('@/api/proveedorApi', () => ({
  proveedorApi: { listar: vi.fn() },
}))
vi.mock('@/api/productoApi', () => ({
  productoApi: { listarPorProveedor: vi.fn() },
}))
vi.mock('@/api/compraApi', () => ({
  compraApi: { crear: vi.fn() },
}))

const proveedoresActivos = [
  { id: 1, nombreComercial: 'Innova', razonSocial: 'INNOVA S.A.C.', estado: 'ACTIVO' },
]
const productosProveedor = [
  { id: 1, codigo: 'PRD-001', nombre: 'Arroz 25kg', precio: 95.0, stock: 120, estado: 'ACTIVO', proveedorId: 1 },
]

function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'compras', component: { template: '<div/>' } },
      { path: '/compras', name: 'compras', component: { template: '<div/>' } },
      { path: '/compras/:id', name: 'compras-detalle', component: { template: '<div/>' } },
    ],
  })
  return mount(CompraNuevaView, {
    global: { plugins: [router], stubs: { RouterLink: true } },
  })
}

describe('CompraNuevaView — contrato de la petición (C4)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    proveedorApi.listar.mockResolvedValue(proveedoresActivos)
    productoApi.listarPorProveedor.mockResolvedValue(productosProveedor)
    compraApi.crear.mockResolvedValue({ id: 9, numeroCompra: 'C-2026-000009' })
  })

  async function montarListo() {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.vm.$nextTick()
    return wrapper
  }

  it('NUNCA envía total, estado ni creadoPor en el body (contrato del backend)', async () => {
    const wrapper = await montarListo()

    await wrapper.find('#numeroCompra').setValue('c-2026-000010')
    await wrapper.find('#proveedorId').setValue(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Primera línea: producto 1, cantidad 2 × 95.00
    await wrapper.findAll('.tabla--linea tbody tr input')[0].setValue('2')
    await wrapper.findAll('.tabla--linea select')[0].setValue('1')
    await wrapper
      .findAll('.tabla--linea tbody tr input')
      .at(1)
      .setValue('95.00')

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(compraApi.crear).toHaveBeenCalledTimes(1)
    const cuerpo = compraApi.crear.mock.calls[0][0]
    expect(cuerpo).toEqual({
      numeroCompra: 'C-2026-000010',
      proveedorId: 1,
      observaciones: null,
      detalles: [{ productoId: 1, cantidad: 2, precioUnitario: '95.00' }],
    })
    expect(cuerpo).not.toHaveProperty('total')
    expect(cuerpo).not.toHaveProperty('estado')
    expect(cuerpo).not.toHaveProperty('creadoPor')
    expect(cuerpo.detalles[0]).not.toHaveProperty('subtotal')
  })

  it('calcula el total ESTIMADO en el frontend con centavos (etiqueta avisada)', async () => {
    const wrapper = await montarListo()
    await wrapper.find('#numeroCompra').setValue('c-2026-000010')
    await wrapper.find('#proveedorId').setValue(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    await wrapper.findAll('.tabla--linea tbody tr input')[0].setValue('3')
    await wrapper.findAll('.tabla--linea select')[0].setValue(1)
    await wrapper
      .findAll('.tabla--linea tbody tr input')
      .at(1)
      .setValue('12.50')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.total-estimado__valor').text()).toBe('37.50')
    expect(wrapper.find('.total-estimado').text()).toContain('est.')
  })

  it('bloquea productos repetidos antes de enviar', async () => {
    const wrapper = await montarListo()
    await wrapper.find('#numeroCompra').setValue('c-2026-000010')
    await wrapper.find('#proveedorId').setValue(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Línea 1
    await wrapper.findAll('.tabla--linea select')[0].setValue(1)
    await wrapper.findAll('.tabla--linea tbody tr input')[0].setValue('1')
    await wrapper
      .findAll('.tabla--linea tbody tr input')
      .at(1)
      .setValue('10.00')

    // Línea 2 (mismo producto)
    await wrapper.vm.agregarLinea()
    await wrapper.vm.$nextTick()
    await wrapper.findAll('.tabla--linea select')[1].setValue(1)
    await wrapper.findAll('.tabla--linea tbody tr input')[2].setValue('1')
    await wrapper
      .findAll('.tabla--linea tbody tr input')
      .at(3)
      .setValue('10.00')

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(compraApi.crear).not.toHaveBeenCalled()
    expect(wrapper.find('.alerta--warn').exists()).toBe(true)
  })

  it('exige el número de compra escrito por el usuario (no es automático)', async () => {
    const wrapper = await montarListo()
    await wrapper.find('#proveedorId').setValue(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    await wrapper.findAll('.tabla--linea tbody tr input')[0].setValue('1')
    await wrapper.findAll('.tabla--linea select')[0].setValue(1)
    await wrapper.findAll('.tabla--linea tbody tr input').at(1).setValue('10.00')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(compraApi.crear).not.toHaveBeenCalled()
    expect(wrapper.find('#numero-error').text()).toContain('obligatorio')
  })

  it('total estimado en centavos con etiqueta "est."', async () => {
    const wrapper = await montarListo()
    await wrapper.find('#numeroCompra').setValue('c-2026-000010')
    await wrapper.find('#proveedorId').setValue(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    await wrapper.findAll('.tabla--linea tbody tr input')[0].setValue('3')
    await wrapper.findAll('.tabla--linea select')[0].setValue(1)
    await wrapper
      .findAll('.tabla--linea tbody tr input')
      .at(1)
      .setValue('12.50')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.total-estimado__valor').text()).toBe('37.50')
    expect(wrapper.find('.total-estimado').text()).toContain('est.')
  })

  it('el cuerpo enviado incluye solo lo permitido (sin total/estado/creadoPor/subtotal)', async () => {
    const wrapper = await montarListo()
    await wrapper.find('#numeroCompra').setValue('c-2026-000010')
    await wrapper.find('#proveedorId').setValue(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    await wrapper.findAll('.tabla--linea tbody tr input')[0].setValue('2')
    await wrapper.findAll('.tabla--linea select')[0].setValue(1)
    await wrapper
      .findAll('.tabla--linea tbody tr input')
      .at(1)
      .setValue('95.00')

    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(compraApi.crear).toHaveBeenCalledTimes(1)
    const cuerpo = compraApi.crear.mock.calls[0][0]
    expect(Object.keys(cuerpo).sort()).toEqual(['detalles', 'numeroCompra', 'observaciones', 'proveedorId'])
    expect(cuerpo.detalles[0]).toEqual({ productoId: 1, cantidad: 2, precioUnitario: '95.00' })
  })
})
