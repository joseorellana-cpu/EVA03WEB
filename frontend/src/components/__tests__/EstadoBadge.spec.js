import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import EstadoBadge from '@/components/EstadoBadge.vue'

describe('EstadoBadge — render seguro (texto plano, anti-XSS)', () => {
  it('muestra "<img src=x onerror=alert(1)>" como TEXTO, sin ejecutar HTML', () => {
    const payload = '<img src=x onerror=alert(1)>'
    const wrapper = mount(EstadoBadge, { props: { estado: payload } })
    expect(wrapper.text()).toBe(payload)
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.element.tagName).toBe('SPAN')
  })

  it('muestra estados conocidos con etiqueta legible y clase de color', () => {
    const r = mount(EstadoBadge, { props: { estado: 'CONFIRMADA' } })
    expect(r.text()).toBe('CONFIRMADA')
    expect(r.classes()).toContain('badge--ok')

    const r2 = mount(EstadoBadge, { props: { estado: 'ANULADA' } })
    expect(r2.classes()).toContain('badge--danger')
  })
})
