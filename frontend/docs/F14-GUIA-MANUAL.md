# F-14 · Guía de pruebas manuales contra el backend real

**Alcance:** flujo completo por la interfaz (sin Postman) y casos fallidos.
El backend (MySQL → Eureka → micros → Gateway) debe estar arriba antes de
empezar. El frontend en desarrollo usa el proxy de Vite:
`VITE_API_PROXY_TARGET` por defecto `http://localhost:8080`.

## A. Flujo completo (8 minutos)

1. **Proveedores** → crear con RUC **20123456789** y datos de prueba.
   - Carga el listado; verifica que aparece con estado ACTIVO y creado por
     `jose.orellana` (usuario simulado del topbar).
2. **Productos** → crear con `precio` 95.00, `stock` 120, `stockMinimo` 20 y
   seleccionando el proveedor **ACTIVO** recién creado.
   - En el listado debe verse `precio: 95.00` y, si `stock <= stockMinimo`,
     el marcador **Bajo stock mínimo**.
3. **Compras → Nueva compra** →
   - Proveedor: el ACTIVO creado.
   - Producto: el creado arriba.
   - Cantidad: 3 · Precio unitario: **95.00** (el precio de compra viaja en el request).
   - El total **estimado** muestra **285.00** (3 × 95.00) — etiqueta "total est.".
   - Guardar → navega a `/compras/:id` con estado **REGISTRADA**.
4. **Verificar que REGISTRAR no descontó stock** →
   - Ve a Producto → Movimientos: todavía **no hay descuento** (solo movimientos previos).
5. **Confirmar** → en el detalle pulsa **Confirmar** (descuenta stock).
   - Productos → Movimientos: aparece un movimiento con
     `operacion: DESCONTAR` y stock anterior 120 → nuevo 117.
6. **Verificar stock** → listado de productos: stock **117**.
7. **Anular** → en el detalle pulsa **Anular**.
   - Detalle: estado **ANULADA**. Stock vuelve a **120**.
8. **Auditoría** → en el detalle, tabla de auditoría:
   tres filas **REGISTRADA → CONFIRMADA → ANULADA** con `usuario: jose.orellana`
   (o el usuario simulado activo).

## B. Casos fallidos

| # | Caso | Esperado |
|---|---|---|
| 1 | RUC inválido (menos de 11 dígitos) | Error en el formulario; el backend responde 400 y se muestran los `details` como lista |
| 2 | Crear producto con código duplicado | 409; mensaje del backend visible en el formulario |
| 3 | Compra a proveedor INACTIVO | 409 "El proveedor … no está ACTIVO" |
| 4 | Cantidad > stock disponible | 409 "Stock insuficiente…" |
| 5 | Editar compra ANULADA | La vista no permite editar; la URL muestra el aviso y el backend protege con 409 |
| 6 | Backend caído (detener Gateway o micros) | 503 genérico en español, sin stack trace |

**Detalles por caso:**

- **B1 — RUC inválido:** en "Nuevo proveedor" escribe RUC `123` → error cliente
  "El RUC debe tener exactamente 11 dígitos numéricos". Si se fuerza en el
  controlador con un `POST` directo, el backend devuelve 400 y la interfaz
  muestra `details` en la lista final.
- **B2 — Código duplicado:** crea un producto con `PRD-001` (ya existe) →
  resp. 409 y mensaje "El producto con código PRD-001 ya existe".
- **B3 — Proveedor inactivo:** en Proveedores pulsa **Desactivar**; en
  **Nueva compra** el proveedor ya no se ofrece (solo ACTIVOS); si se usa el
  ID de un INACTIVO (por consola o Postman) el backend responde 409.
- **B4 — Stock insuficiente:** compra con cantidad 99999 → 409 con el
  stock disponible y el solicitado.
- **B5 — Compra ANULADA:** intenta PUT directo (Postman) y verás 409.
  La UI lo refleja ocultando los botones de edición/confirmación/anulación.
- **B6 — Backend caído:** detén el proceso del Gateway o del ms-producto y
  recarga: la interfaz muestra "El servicio no está disponible…" para 503,
  sin volcar stack traces ni objetos crudos (solo `message` + `details`).
