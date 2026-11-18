import { describe, it, expect } from 'vitest'
import { siguienteCodigo, numeroCompraPorDefecto, codigoProductoPorDefecto } from '@/utils/codigos'

describe('códigos por defecto', () => {
  it('toma el mayor existente y le suma 1', () => {
    expect(siguienteCodigo(['PRD-001', 'PRD-006', 'PRD-003'], 'PRD-', 3)).toBe('PRD-007')
  })

  it('empieza en 1 si no hay ninguno con ese prefijo', () => {
    expect(siguienteCodigo([], 'PRD-', 3)).toBe('PRD-001')
    expect(siguienteCodigo(['OTRO-9', 'F001'], 'PRD-', 3)).toBe('PRD-001')
  })

  it('número de compra del año dado, ignorando otros años y números escritos a mano', () => {
    const compras = [
      { numeroCompra: 'C-2026-000005' },
      { numeroCompra: 'C-2025-000099' },
      { numeroCompra: 'F001-23' },
    ]
    expect(numeroCompraPorDefecto(compras, 2026)).toBe('C-2026-000006')
    expect(numeroCompraPorDefecto([], 2027)).toBe('C-2027-000001')
  })

  it('código de producto por defecto', () => {
    expect(codigoProductoPorDefecto([{ codigo: 'PRD-001' }, { codigo: 'prd-010' }])).toBe('PRD-011')
  })
})
