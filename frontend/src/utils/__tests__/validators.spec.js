import { describe, it, expect } from 'vitest'
import {
  validarRuc,
  validarEmail,
  validarPrecio,
  validarEnteroNoNegativo,
  validarCantidad,
  longitudObligatoria,
  longitudOpcional,
  validarUsuarioSimulado,
  validarNumeroCompra,
} from '@/utils/validators'

describe('validators — RUC', () => {
  it('acepta exactamente 11 dígitos numéricos', () => {
    expect(validarRuc('20123456789').ok).toBe(true)
  })
  it.each([['123'], ['2012345678'], ['201234567890'], ['201234567A'], ['201234567A1']])(
    'rechaza %j',
    (s) => {
      expect(validarRuc(s).ok).toBe(false)
    },
  )
  it('espacios exteriores se recortan (UX) y el contenido sigue validándose', () => {
    expect(validarRuc('  20123456789  ').ok).toBe(true)
  })
})

describe('validators — email', () => {
  it('email válido', () => {
    expect(validarEmail('ventas@innova.pe').ok).toBe(true)
  })
  it('rechaza formato inválido', () => {
    expect(validarEmail('no-es-email').ok).toBe(false)
    expect(validarEmail('a@b').ok).toBe(false)
  })
  it('opcional: vacío es válido', () => {
    expect(validarEmail('').ok).toBe(true)
  })
})

describe('validators — precio', () => {
  it('acepta > 0 con 2 decimales', () => {
    expect(validarPrecio('12.50').ok).toBe(true)
    expect(validarPrecio('0.01').ok).toBe(true)
    expect(validarPrecio('1,000.00').ok).toBe(false) // coma miles no admitida
    expect(validarPrecio('0.00').ok).toBe(false)
    expect(validarPrecio('-1.00').ok).toBe(false)
    expect(validarPrecio('12.999').ok).toBe(false)
    expect(validarPrecio('').ok).toBe(false)
  })
})

describe('validators — stock y cantidad', () => {
  it('entero >= 0 en stock', () => {
    expect(validarEnteroNoNegativo(10).ok).toBe(true)
    expect(validarEnteroNoNegativo(0).ok).toBe(true)
    expect(validarEnteroNoNegativo(-1).ok).toBe(false)
    expect(validarEnteroNoNegativo(3.5).ok).toBe(false)
    expect(validarEnteroNoNegativo('5').ok).toBe(true)
  })
  it('cantidad entera >= 1', () => {
    expect(validarCantidad(1).ok).toBe(true)
    expect(validarCantidad(0).ok).toBe(false)
    expect(validarCantidad(2.5).ok).toBe(false)
    expect(validarCantidad(-3).ok).toBe(false)
    expect(validarCantidad('3').ok).toBe(true)
  })
})

describe('validators — longitudes', () => {
  it('obligatorio falla en vacío y exceso', () => {
    expect(longitudObligatoria('', 10, 'nombre').ok).toBe(false)
    expect(longitudObligatoria('x'.repeat(11), 10, 'nombre').ok).toBe(false)
    expect(longitudObligatoria('ana', 10, 'nombre').ok).toBe(true)
  })
  it('opcional pasa en vacío', () => {
    expect(longitudOpcional('', 10, 'obs').ok).toBe(true)
    expect(longitudOpcional('x'.repeat(11), 10, 'obs').ok).toBe(false)
  })
})

describe('validators — usuario simulado', () => {
  it('acepta login tipo lab (3-40 chars, letras/dígitos . _ -)', () => {
    expect(validarUsuarioSimulado('jose.orellana')).toBe(true)
    expect(validarUsuarioSimulado('sistema')).toBe(true)
    expect(validarUsuarioSimulado('Aab')).toBe(true)
  })
  it('rechaza vacío, espacios y caracteres raros', () => {
    expect(validarUsuarioSimulado('')).toBe(false)
    expect(validarUsuarioSimulado('ana reyes')).toBe(false)
    expect(validarUsuarioSimulado('a')).toBe(false)
    expect(validarUsuarioSimulado('hacker<script>')).toBe(false)
  })

  it('validarNumeroCompra: obligatorio, letras/números/guiones, máx 20', () => {
    expect(validarNumeroCompra('C-2026-000010').ok).toBe(true)
    expect(validarNumeroCompra('F001').ok).toBe(true)
    expect(validarNumeroCompra('').ok).toBe(false)
    expect(validarNumeroCompra('C 2026').ok).toBe(false)
    expect(validarNumeroCompra('A'.repeat(21)).ok).toBe(false)
  })
})
