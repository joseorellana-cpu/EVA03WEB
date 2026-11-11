import { describe, it, expect } from 'vitest'
import {
  esMontoValido,
  parsearMonto,
  sumarCentavos,
  sumarMontoCentavos,
  multiplicarPorCantidad,
  subtotal,
  formatoMonto,
  formatoApi,
} from '@/utils/money'

describe('money — 0,10 + 0,20 (coma y punto)', () => {
  it('resuelve exactamente 0.30 con coma o punto decimal', () => {
    const a = parsearMonto('0,10')
    const b = parsearMonto('0.20')
    expect(a.centavos).toBe(10)
    expect(b.centavos).toBe(20)
    expect(sumarCentavos(a.centavos, b.centavos)).toBe(30)
    expect(formatoApi(sumarMontoCentavos([10, 20]))).toBe('0.30')
  })
})

describe('money — validación con regex', () => {
  it.each([
    ['12.50', true],
    ['0.01', true],
    ['12,50', true],
    ['1,000.00', false], // cero-padding a la izquierda de miles no permitido en regex simple
    ['12.5', true],
    ['12.', false],
    ['12.505', false],
    ['', false],
    ['abc', false],
    ['-12.50', false],
    ['1,2,3', false],
  ])('esMontoValido(%s) = %s', (entrada, esperado) => {
    expect(esMontoValido(entrada)).toBe(esperado)
  })

  it('9,999,999,999.99 es válido (entero seguro en centavos)', () => {
    expect(esMontoValido('9999999999.99')).toBe(true)
    expect(parsearMonto('9999999999.99').centavos).toBe(999999999999)
    expect(Number.isSafeInteger(999999999999)).toBe(true)
  })
})

describe('money — rechazo fuera de rango y decimales', () => {
  it('9007199254740993 (2^53+1) no es entero seguro', () => {
    expect(Number.isSafeInteger(9007199254740993)).toBe(false)
    expect(esMontoValido('9007199254740993.00')).toBe(false)
  })

  it('rechaza más de 2 decimales aunque redondee razonable', () => {
    expect(esMontoValido('12.505')).toBe(false)
    expect(parsearMonto('12.505').ok).toBe(false)
  })

  it('formato con 2+ decimales no se redondea silenciosamente', () => {
    expect(parsearMonto('155.999').ok).toBe(false)
  })
})

describe('money — operaciones', () => {
  it('multiplicarPorCantidad: enteros, no flotantes', () => {
    expect(multiplicarPorCantidad(1250, 10)).toBe(12500)
    expect(subtotal(10, 1250)).toBe(12500) // 125.00
    expect(subtotal(2, 18990)).toBe(37980) // 379.80
    expect(sumarMontoCentavos([12500, 37980])).toBe(50480) // 504.80
  })

  it('lanzar RangeError fuera de rango seguro, nunca NaN', () => {
    expect(() => multiplicarPorCantidad(Number.MAX_SAFE_INTEGER, 2)).toThrow(RangeError)
  })

  it('formato de salida es-PE', () => {
    expect(formatoMonto(12500)).toBe('125.00')
    expect(formatoMonto(123456)).toBe('1,234.56')
    expect(formatoMonto(1250000)).toBe('12,500.00')
    expect(formatoApi(12500)).toBe('125.00')
    expect(formatoApi(37980)).toBe('379.80')
    expect(formatoApi(-12500)).toBe('-125.00')
  })
})
