import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useAccionUnica } from '@/composables/useAccionUnica'

describe('useAccionUnica — guardián de doble envío (C6)', () => {
  it('dos clics = una sola petición (botón se deshabilita mientras está en curso)', async () => {
    const fakePeticion = vi.fn().mockImplementation(() => new Promise((res) => setTimeout(res, 20)))

    const Comp = defineComponent({
      setup() {
        const { enCurso, ejecutar } = useAccionUnica()
        function accion() {
          ejecutar(fakePeticion)
        }
        return { enCurso, accion }
      },
      render() {
        return h('button', { disabled: this.enCurso, onClick: this.accion }, 'Guardar')
      },
    })

    const wrapper = mount(Comp)
    const boton = wrapper.find('button')

    await boton.trigger('click') // primer click → pone enCurso = true
    await boton.trigger('click') // segundo click → ignorado (ya en curso)

    expect(fakePeticion).toHaveBeenCalledTimes(1)
    expect(boton.attributes().disabled).toBeDefined()

    // espera a que la petición termine
    await new Promise((r) => setTimeout(r, 40))
    expect(boton.attributes().disabled).toBeUndefined()
  })

  it('un error en la petición libera el guardián (puede reintentarse)', async () => {
    const fakeFallo = vi.fn().mockRejectedValueOnce(new Error('falló'))
    const Comp = defineComponent({
      setup() {
        const { enCurso, ejecutar } = useAccionUnica()
        function accion() {
          return ejecutar(fakeFallo).catch(() => {})
        }
        return { enCurso, accion }
      },
      render() {
        return h('button', { disabled: this.enCurso, onClick: this.accion }, 'Guardar')
      },
    })

    const wrapper = mount(Comp)
    const boton = wrapper.find('button')
    await boton.trigger('click')
    await new Promise((r) => setTimeout(r, 10))
    expect(boton.attributes().disabled).toBeUndefined()
  })
})
