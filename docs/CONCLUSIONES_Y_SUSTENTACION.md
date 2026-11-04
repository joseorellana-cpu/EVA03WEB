# Conclusiones y Guía de Sustentación
**Laboratorio de Arquitectura de Microservicios — Orellana Rodriguez Jose Miguel**
**Fecha:** 04 de noviembre de 2026

---

## 4.16 Conclusiones individuales (en primera persona)

**Aprendizaje 1 — La transacción local no es una transacción distribuida.** Empecé creyendo que envolver `confirmar()` en `@Transactional` bastaba para que "todo se deshaga" si algo fallaba, pero al escribir la compensación entendí que el rollback de Spring solo revierte lo que este microservicio hizo en **bd_compra**: los descuentos por Feign en **bd_producto** están en otra conexión y se confirman solos. Por eso el flujo descuenta producto por producto guardando cuáles ya fueron afectados, y si el siguiente falla vuelve a llamar con la operación inversa (patrón de compensación) antes de responder 409. `[com.orellana.microservicios.ms_compra.services.impl.CompraServiceImpl (métodos confirmar() y compensar()), test CompraServiceImplTest.confirmarConCompensacion]`

**Aprendizaje 2 — Feign no es solo una URL: es un contrato que hay que traducir.** Al integrar los 3 servicios aprendí que un `@FeignClient(name = "MS-PRODUCTO")` resuelto con Eureka + LoadBalancer devuelve errores y formatos ajenos al dominio local; un 409 de stock llegó como `FeignException.Conflict` con su propia estructura y a veces como `RetryableException` si la conexión se cortó. Construir un `ErrorDecoder` que lea el `message` del cuerpo del formato global y lo convierta en excepciones de dominio (`StockInsuficienteException`, `ServicioNoDisponibleException→503`) mantuvo al `GlobalExceptionHandler` pequeño y el cliente libre de detalles internos del servicio remoto. `[com.orellana.microservicios.ms_compra.config.FeignErrorDecoder y FeignHeaderInterceptor, client/ProductoClient y ProveedorClient]`

**Aprendizaje 3 — La auditoría automática se diseña de punta a punta, no se improvisa.** **Implementar quién hizo cada cambio (`creadoPor`/`modificadoPor`) con `@EnableJpaAuditing` + `AuditorAware`** me mostró que el usuario viaja como **dato de la petición**, no del servicio: si ms-compra llama a ms-producto sin propagar el header, la fila remota se registraría como "sistema" aunque el actor real fuese otro. Resolví eso con un interceptor Feign que copia `X-Usuario` hacia el servicio remoto, y con la decisión de escribir la auditoría de compra en la **misma transacción** del cambio de estado, así el historial nunca queda desincronizado del estado real. `[com.orellana.microservicios.ms_compra.config.UsuarioAuditoriaAware y FeignConfig, registrarAuditoria() en CompraServiceImpl, test AuditoriaCompraTest.cicloCompletoConAuditoria]`

**Aprendizaje 4 — Los DTO son el muro contra el acoplamiento entre dominios.** Comprendí por qué la guía exige `XxxResponse` como records: los DTO remotos siempre llegan exactamente iguales a `ms-compra` cuando el DTO es el contrato (record `ProductoClientDto`, copia fiel pero local); si mañana ms-producto agrega un campo, `ms-compra` sigue compilando y el campo extra se ignora en el mapping. También aprendí la regla opuesta: el `precioUnitario` **de compra sí viaja en el request** (`{ productoId, cantidad, precioUnitario }`, validado en `@Valid` con `@Digits(integer=9, fraction=2)` y `@DecimalMin("0.01")`), porque el precio de la compra puede diferir del precio de referencia del catálogo (`Producto.precio`); el catálogo ya NO se copia como precio de la compra, y el total se calcula en el servidor con `cantidad × precioUnitario` (`bd_compra.compra_detalles.subtotal`, `compras.total`). `[com.orellana.microservicios.ms_compra.dto.CompraRequest / DetalleRequest / CompraMapper, construirDetalles() en CompraServiceImpl]`

**Aprendizaje 5 — Los estados explican el negocio mejor que el CRUD.** Diseñar el ciclo `REGISTRADA → CONFIRMADA → ANULADA` (y bloquear cada operación al estado válido, guardando también la acción en auditoría) es una decisión de negocio con regla visible: al guardar `estadoAnterior`/`estadoNuevo`/usuario/fecha en la misma transacción, puedo responder "quién anuló esta compra y desde qué estado" sin reingeniería. Lo mismo apliqué en ms-producto con `EstadoProducto` y en `stock_movimientos` con `stockAnterior`/`stockNuevo`, y la regla del DELETE solo sobre `ANULADA` me hizo entender por qué el borrado físico es siempre excepcional. `[com.orellana.microservicios.ms_compra.model.EstadoCompra y AccionCompra, model.CompraAuditoria, eliminar() y anular() en CompraServiceImpl]`

**Dificultad encontrada y cómo la resolví: la compensación al fallar el segundo producto.** Mi primer borrador de `confirmar()` descontaba todos los productos en bucles independientes y, cuando el segundo fallaba, el rollback local dejaba el stock del primero **ya descontado** en ms-producto: la compra quedaba en REGISTRADA pero el inventario quedaba descuadrado. Lo resolví así: (1) revalidar **todos** los productos antes de tocar nada (existencia, ACTIVO, mismo proveedor); (2) descuentar producto por producto registrando cuáles ya fueron afectados; (3) si falla alguno, **compensar** cada uno con `OperacionStock.inversa()` (INCREMENTAR) en el mismo `catch`; (4) responder 409 con formato global. El test `CompraServiceImplTest.confirmarConCompensacion` reproduce el caso exacto (1º descuentado OK, 2º lanza `StockInsuficienteException`) y verifica que la compra recibió un `INCREMENTAR` y que `save` nunca ocurre. Con esto el sistema vuelve por sí mismo al estado previo. Y `OPERACION_AL_CONFIRMAR` vive en una constante para permitir el cambio en vivo de sustentación. `[CompraServiceImpl.confirmar/compensar + OperacionStock.inversa(), test CompraServiceImplTest.confirmarConCompensacion]`

---

## 4.17 Guía de sustentación

### Preguntas típicas con respuesta corta

**1. ¿Por qué bases de datos separadas si MySQL permite FK entre bases?**
Porque las FK entre bases acoplarían físicamente tres servicios que en producción tendrían bases distintas: haría imposible mover un dominio a otro servidor y me obligaría a coordinar migraciones. La integridad se revisa en tiempo de ejecución (ms-compra valida con Feign) y las únicas FK reales son internas a cada base, como `compra_detalles → compras`.

**2. ¿Entonces cómo garantizas que el proveedorId o productoId existan?**
Con validación en el momento del uso: `registrar()` llama a `ProveedorClient.obtenerProveedor()` y `ProductoClient.obtenerProducto()`; si el 404 remoto llega, ms-compra responde 404 con el formato global. No necesito un índice cross-base: necesito validar en la capa de negocio y guardar la traza de lo que existía en el pedido (`compra_detalles`).

**3. ¿Qué pasa si ms-producto está caído y quiero comprar?**
`llamarRemoto()` captura `FeignException` (conexión/reintentos) y responderá 503 con el formato global: "El servicio MS-PRODUCTO no esta disponible…". El cliente sabe que es un problema de disponibilidad, no de reglas de negocio.

**4. ¿Y si cayese ms-proveedor?**
Igual: 503 con formato global, y además NO se crea la compra — el `registrar()` valida proveedor activo antes de guardar nada.

**5. ¿Cómo evitas stock negativo?**
Con dos barreras: (1) al registrar, ms-compra compara con el stock leído (validación temprana, diagnostica el total "Stock insuficiente (stock disponible X, solicitado Y)"); (2) al descontar, ms-producto revalida con lock pesimista (`findByIdForUpdate`, `PESSIMISTIC_WRITE`) y devuelve 409 si no alcanza — la barrera definitiva vive en quien posee el dato.

**6. ¿Compensación o transacción distribuida? ¿Por qué compensación?**
Compensación. Un 2PC/XA coordinaría ambas BD pero añade un gestor que el laboratorio no incluye, bloquea a los participantes y acopla los servicios; con compensación explícita yo entiendo el fallo en el log ("COMPENSACION aplicando INCREMENTAR sobre N productos"), respondo un 409 razonable y no necesito middleware extra. Para 3 servicios HTTP con inventario es el patrón correcto.

**7. ¿Por qué Eureka si ya hay un Gateway?**
Funciones distintas: Eureka es un registro que resuelve la instancia por nombre (`MS-PRODUCTO`) y permite escalar/cambiar puertos sin tocar código; el Gateway es el punto de entrada que el cliente conoce (8080) y decide qué ruta aplicar. Sin Eureka, Feign funcionaría solo con URLs fijas (sin balanceo ni alta disponibilidad); sin Gateway, cada microservicio necesitaría puertos abiertos al cliente.

**8. ¿Cómo aplicaste SOLID (D, I, O)?**
**D:** `CompraServiceImpl` depende de `ProveedorClient`/`ProductoClient` (abstracciones Feign) y de repositorios, nunca de implementaciones — la sustitución del "servicio real por mock" en todos los tests se hace posible gracias a esa inversión. **I:** interfaces pequeñas con solo los métodos que se usan (`ProveedorClient` tiene 1 método; `ProductoClient` tiene 2). **O:** la jerarquía de excepciones está cerrada para modificación pero abierta a extensión: agregar una regla de negocio significa definir una excepción nueva y su mapping (`GlobalExceptionHandler`), sin cambiar las clases de cada método. La inversión de `OPERACION_AL_CONFIRMAR` demuestra la lectura O en otra dimensión: el patrón del flujo es estable y solo el dato cambia.

**9. ¿Por qué confirmar descuenta stock, y no al registrar la compra?**
Con la Decisión J: la compra se crea como `REGISTRADA`, que representa una intención validada (proveedor activo, productos válidos, stock); `confirmar` es el acto de ejecutarla, y ahí aplico la operación aislada (`OPERACION_AL_CONFIRMAR = DESCONTAR`) al inventario. Si falla la ejecución a mitad del flujo, puedo compensar: es más natural tener un punto de decisión antes de tocar el inventario.

**10. ¿Cómo sabes quién anuló una compra?**
Por 3 vías consistentes: (1) el header `X-Usuario` llena `modificadoPor` de la compra vía auditoría JPA; (2) `anular()` escribe una fila en `compra_auditoria` donde `usuario` es el header (o "sistema") con `estadoAnterior=CONFIRMADA` y `estadoNuevo=ANULADA`; (3) si la anulada hubiese tocado stock, el `stock_movimientos` de ms-producto muestra el `INCREMENTAR` con el mismo usuario (y el interceptor Feign hace que el remoto registre el mismo actor). `GET /api/compras/{id}/auditoria` devuelve todo ordenado del más reciente al más antiguo.

### Pregunta oficial N.° 03 — respuesta modelo (1 minuto)

*Pregunta oficial:* "Explique por qué separó el sistema en estos servicios, cómo persiste los datos y qué ocurre cuando la API recibe información inválida."

**Respuesta:** "Separé el sistema por dominios de negocio: **ms-proveedor** administra proveedores, **ms-producto** catálogo e inventario y **ms-compra** el proceso de compra; cada uno tiene su propia base: `bd_proveedor`, `bd_producto` y `bd_compra`, y el Eureka (`eureka-server`) es el punto de encuentro y el API Gateway (`api-gateway`) el único puerto que ve el cliente (`8080`, sin StripPrefix, enrutado con `lb://`). Entre bases no hay llaves foráneas: las relaciones se guardan como ids `Long` y la integridad se comprueba en tiempo de ejecución vía Feign (`validarProveedor` y `construirDetalles` llaman a ms-proveedor/ms-producto y responden 404/409 si algo no cuadra). Los datos persisten con Spring Data JPA: entidades como `Compra` mapean a la tabla `compras` con relaciones internas `@OneToMany`/`@ManyToOne`, y el ciclo de vida está en `EstadoCompra` (`REGISTRADA` → `CONFIRMADA` → `ANULADA`). Cuando la API recibe información inválida, el `GlobalExceptionHandler` responde con el formato global de error `{timestamp, status, error, message, path, details}`: una entrada mal escrita (por ejemplo `cantidad: 0` o sin `detalles`) produce **400** con el campo que falló y sin stack trace; un id inexistente da **404**; una regla de negocio incumplida da **409** (proveedor no ACTIVO, stock insuficiente, productos repetidos, estados inválidos); si un servicio dependiente falla, **503**; y cualquier excepción inesperada termina en **500** genérico, nunca con stack trace al cliente."

### Guion de sustentación (5–8 minutos) — una Entity, un Repository, un Service, un Controller y el manejo de errores

**Minuto 0:00–0:45 | Entity — `Compra`** (`ms-compra/src/main/java/com/orellana/microservicios/ms_compra/model/Compra.java`)
Señala `@Entity @Table(name = "compras")`, los campos `numeroCompra` (formato `C-2026-000001` de `NumeroCompraGenerator`), `proveedorId` como `Long` (sin FK cruzada), `total` (`BigDecimal`), `estado` (`@Enumerated(EnumType.STRING)` → `REGISTRADA`/`CONFIRMADA`/`ANULADA`) y las relaciones: `detalles` con `@OneToMany(cascade = ALL, orphanRemoval = true)` mapeada por `@JoinColumn(name = "compra_id")`, y la `FK` interna a `compra_detalles` y `compra_auditoria`. Muestra que `AuditoriaBase` aporta `fechaCreacion`, `fechaModificacion`, `creadoPor`, `modificadoPor` — auditoría de la misma tabla, sin una tabla paralela.

**Minuto 0:45–1:30 | Repository — `CompraRepository`** (`.../ms-compra/repository/CompraRepository.java`)
Extiende `JpaRepository<Compra, Long>`; muestra `findByProveedorId` (feed por proveedor), `existsByNumeroCompra` (respaldo de la unicidad de `numero_compra`), y `countByNumeroCompraStartingWith` (secuencial por año para el `C-2026-...` con reintento). Aclara que el lock pesimista NO está aquí: vive en ms-producto (`ProductoRepository.findByIdForUpdate`, SQL nativo `SELECT ... FOR UPDATE` que bloquea la fila y hace imposible el stock negativo por concurrencia). Puedes mencionar también que `StockMovimientoRepository` es el kardex (balance por transacción, `DESCONTAR`/`INCREMENTAR`, usuario y stock anterior/nuevo).

**Minuto 1:30–3:30 | Service — `CompraServiceImpl`** (`.../services/impl/CompraServiceImpl.java`)
Recorre `registrar()` (valida proveedor ACTIVO, duplicados, existencia y stock; `construirDetalles()` toma el `precioUnitario` del **detalle del pedido** —no del catálogo— y calcula `subtotal = cantidad × precioUnitario` y `total = Σ subtotales`; guarda, audita y NO descuenta). Luego `confirmar()` (solo desde `REGISTRADA`; descuenta producto por producto con la constante `OPERACION_AL_CONFIRMAR = DESCONTAR`; si falla un producto, `compensar()` re-llama ms-producto con la operación inversa y responde 409). Luego `anular()` (sólo si `CONFIRMADA` devuelve stock vía `OPERACION_AL_CONFIRMAR.inversa()`). Destaca la decisión J y que el rollback local NO deshace llamadas remotas — por eso existe compensación. Habla de `@Transactional` envolviendo todo el flujo y la escritura de `compra_auditoria` en la MISMA transacción.

**Minuto 3:30–4:30 | Controller — `CompraController`** (`.../controller/CompraController.java`)
Arquitectura delgada: `@RestController`, `@RequestMapping("/api/compras")`, 9 endpoints (lista, `GET /{id}`, `GET /proveedor/{proveedorId}`, `POST`, `PUT /{id}`, `POST /{id}/confirmar`, `POST /{id}/anular`, `DELETE /{id}`, `GET /{id}/auditoria`), `@Valid` en los `@RequestBody` y header `X-Usuario` (fallback `sistema`) para la auditoría. Enseña el contrato exacto de `CompraRequest`/`CompraResponse`/`DetalleRequest` (record: `{ proveedorId, observaciones, detalles: [{ productoId, cantidad, precioUnitario }] }`).

**Minuto 4:30–6:00 | Manejo de errores — `GlobalExceptionHandler`** (`.../exception/GlobalExceptionHandler.java`)
Muestra el formato global único `{timestamp, status, error, message, path, details}` idéntico en los 3 microservicios: `MethodArgumentNotValidException → 400` (campo + defaultMessage), `HttpMessageNotReadableException → 400` (JSON inválido), `MethodArgumentTypeMismatchException → 400`, `RecursoNoEncontradoException → 404`, `{ConflictoException, ProveedorInactivoException, StockInsuficienteException, CompraNoModificableException} → 409`, `ServicioNoDisponibleException → 503`, `Exception → 500` genérico **sin stack trace** (el stack queda en el log del servidor). Dentro de `CompraServiceImpl.llamarRemoto()` el `FeignErrorDecoder` traduce los errores remotos a excepciones de dominio (404 remoto → `RecursoNoEncontradoException`; 409 remoto → `StockInsuficienteException`/`ProveedorInactivoException`/`ConflictoException` según mensaje; resto → `ServicioNoDisponibleException`), de modo que ms-compra responde con un contrato propio y el cliente nunca ve internals del servicio remoto.

**Minuto 6:00–7:30 | Cierre — la prueba de aceptación**
Cierra ejecutando en Postman la carpeta `03B Aceptacion` → **ACEPTACION - Compra con dos productos y total** (proveedor 1; producto 1 × 10 a 12.50; producto 2 × 2 a 189.90): `pm.test` exige `subtotales 125.00 / 379.80` y `total 504.80`, y el `description` del request trae el SQL en `bd_compra` que lo comprueba (`compra_detalles` ordenada por `producto_id` y `SELECT total FROM bd_compra.compras`). Remata reiterando el patrón: **separación de dominios, persistencia propia, errores uniformes y contrato explícito**.

### Cambios en vivo en la sustentación

**Cambio A — Nueva validación: compras con tope máximo de 1000 unidades por producto.**
**Pasos:**
1. Abrir `ms-compra/src/main/java/com/orellana/microservicios/ms_compra/services/impl/CompraServiceImpl.java`.
2. En `construirDetalles()`, justo después de verificar el stock, añadir:
```java
if (pedido.cantidad() > 1000) {
    throw new ConflictoException(
        "La cantidad por producto no debe exceder 1000 (solicitado " + pedido.cantidad() + ")");
}
```
3. Añadir test en `CompraServiceImplTest` que llama `registrar()` con cantidad 1001 y verifica 409 + `save` nunca.
4. `mvnw.cmd test` (verde) → comprobar vía POST con cantidad 1001: error 409.
**Por qué es seguro:** la validación remota ya se hace así: `ms-compra` captura la regla distinta del stock, lo cual mantiene el principio O (regla nueva = excepción nueva, sin tocar controladores ni DTOs).

**Cambio B — Nuevo campo "prioridad" en la compra (entidad + DTO + SQL + test).**
**Pasos:**
1. `model/Compra.java`: añadir `@Column(name = "prioridad", length = 20) private String prioridad;` con getter/setter.
2. `dto/CompraRequest.java`: añadir `@Size(max = 20) String prioridad` al record y pasarlo al record `CompraResponse` en el mismo lugar de la lista.
3. `dto/CompraMapper.java`: mapear `compra.getPrioridad()` en `toDto()` (y setearlo en el servicio a partir del request, con valor por defecto `"NORMAL"`).
4. SQL (para bases ya existentes): `ALTER TABLE compras ADD COLUMN prioridad VARCHAR(20) DEFAULT 'NORMAL';`
5. Test en `AuditoriaCompraTest`: POST con `"prioridad": "ALTA"`, y GET que verifica `$.prioridad == "ALTA"`.
6. `mvnw.cmd test` (con `ddl-auto: update` en test, H2, la columna se crea sola) → arrancar dev y verificar en MySQL.
**Por qué no rompe:** los DTO son records con contrato explícito; agregar campo al final del record no rompe llamadas que construyen el record por nombre.

**Cambio C — Nueva regla de negocio: anular solo permitido dentro de las 24 horas de la confirmación.**
**Pasos:**
1. `CompraServiceImpl.anular()`: cuando el estado sea `CONFIRMADA`, antes de devolver el stock validar: `if (compra.getFechaModificacion() != null && LocalDateTime.now().isAfter(compra.getFechaModificacion().plusHours(24))) { throw new ConflictoException("Solo se puede anular una compra dentro de las 24 horas"); }`
2. Test en `CompraServiceImplTest`: fabricar compra CONFIRMADA con fecha de modificación forzada a hace 25 horas (`compra.setFechaModificacion(LocalDateTime.now().minusHours(25))`), llamar `anular()` y verificar `ConflictoException` + `save` nunca llamado.
3. `mvnw.cmd test` → verde, comprobar vía Postman: `POST /{id}/anular` responde 409 si la compra es vieja.
**Por qué menciono en la sustentación la inversión de OPERACION_AL_CONFIRMAR:** la constante `static final OperacionStock OPERACION_AL_CONFIRMAR = OperacionStock.DESCONTAR` en `CompraServiceImpl` está en UNA línea; al cambiarla a `INCREMENTAR`, `confirmar()` pasaría a **devolver** stock (modo "compra para devolver inventario") y `anular()` — que usa `OPERACION_AL_CONFIRMAR.inversa()` — pasaría a quitarlo automáticamente, sin tocar `confirmar`, `anular` ni `compensar`. Es el ejemplo perfecto de cambio de un solo dato que altera el comportamiento de los tres flujos porque el resto del código está orientado a la abstracción, no al valor.

---

## 4.18 Checklist final

### (a) Rúbrica: criterio | puntos | evidencia | CUMPLE

| Criterio | Puntos | Evidencia | CUMPLE |
|---|---|---|---|
| 5 proyectos Maven de Initializr, mismos paquetes, Java 21 | 2 | `pom.xml` de cada proyecto (parent Spring Boot 4.1.2, Spring Cloud 2025.1.4); estructura `com.orellana.microservicios.*` | CUMPLE |
| Base de datos y puerto propios por microservicio | 2 | `application-dev.yaml` (8081/8082/8083; bd_proveedor/bd_producto/bd_compra); `sql/01_creacion_bases.sql` | CUMPLE |
| Eureka como registro | 1 | `eureka-server` con `@EnableEurekaServer`, puerto 8761; captura del dashboard con las 3 apps UP | CUMPLE |
| Gateway como puerta única (8080, sin StripPrefix) | 1 | `api-gateway/src/main/resources/application-dev.yaml` (rutas `lb://` sin filtro de prefijo) | CUMPLE |
| DTOs de entrada/salida y validaciones en ms-proveedor | 1 | `dto/*.java` (record) + `@Valid` + mensajes en español | CUMPLE |
| DTOs/validaciones en ms-producto y contratos de stock | 2 | `dto/ProductoRequest`, `dto/AjusteStockRequest {cantidad>0, operacion}`, `dto/StockMovimiento` | CUMPLE |
| DTOs/validaciones de compra y precio enviado por el cliente | 2 | `dto/CompraRequest / DetalleRequest {productoId @NotNull, cantidad @Min(1), precioUnitario @DecimalMin("0.01") @Digits(integer=9, fraction=2)}`; ms-compra calcula `subtotal = cantidad × precioUnitario`, `total = Σ subtotales`; `Producto.precio` queda como precio de referencia (no se copia a la compra) | CUMPLE |
| Ciclo de vida: comprar REGISTRADA / confirmar descuenta / anular devuelve | 2 | `EstadoCompra`, `CompraServiceImpl` (registrar/actualizar/confirmar/anular/eliminar), Postman flujo completo | CUMPLE |
| Compensación ante fallo remoto a mitad del descuento | 2 | `confirmar()/compensar()` con `OperacionStock.inversa()`; test `CompraServiceImplTest.confirmarConCompensacion`; 409 con formato global | CUMPLE |
| Auditoría (cuándo, quién, qué) por header y por movimiento | 1 | `AuditoriaBase` (`fecha_creacion/modificacion`, `creado_por/modificado_por`) + `compra_auditoria` (acción, estados anterior y nuevo, usuario) + `stock_movimientos`; GET `/{id}/auditoria` | CUMPLE |
| Colección Postman global con casos de éxito/fallo y header X-Usuario | 1 | `postman/coleccion-global.json` (5 carpetas, pm.test, flujo completo y errores esperados) | CUMPLE |
| Tests automatizados visibles y ejecutables | 1 | 7/7 + 10/10 + 11/11 verdes con `mvnw.cmd test` | CUMPLE |
| **TOTAL** | **20/20 pts** | | **CUMPLE** |

### (b) Cobertura de los 12 temas del punto M

| # | Tema del punto M | Evidencia |
|---|---|---|
| 1 | Arquitectura: dominios y microservicios separados | README §1 y §4; 3 dominios (proveedor, producto, compra) en proyectos distintos |
| 2 | Spring Boot / Initializr / dependencias | `pom.xml` × 5: parent 4.1.2, cloud 2025.1.4, web, data-jpa, validation, mysql, openfeign, eureka, gateway |
| 3 | Registro y descubrimiento con Eureka | `eureka-server` (@EnableEurekaServer); `@FeignClient(name="MS-…")` de cada cliente |
| 4 | API Gateway con rutas por servicio | `api-gateway/src/main/resources/application-dev.yaml` (`lb://MS-…`, sin StripPrefix) |
| 5 | Base de datos y puerto propio por servicio | README §5; `application-*.yaml`; `sql/01_creacion_bases.sql` |
| 6 | DTOs por capacidad: entrada + salida | `dto/*.java` records en los 3 ms; respuestas sin campos internos |
| 7 | Validaciones de datos en entrada | `@NotNull/@Min/@Size/@NotBlank` + `@Valid` en controladores; errores 400 con campo+detalle |
| 8 | Relaciones entre tablas en la misma BD | `Compra.detalle` (@OneToMany), `CompraDetalle.compra` (@ManyToOne), `deleteByCompraId`, uso de `orphanRemoval` |
| 9 | Referencias entre microservicios EN TABLES de otras bases: GET/feed arenlos IDs, DESCUENTO vía PATCH, XML/JSON | `compra.proveedorId`, `compraDetalle.productoId` como `Long` sin FK cruzada; `ProductoClient.ajustarStock` con `AjusteStockRequest {cantidad, operacion}` y `ProductoResponse` como salida |
| 10 | Ciclo de vida con estados | `EstadoCompra`/`AccionCompra`; reglas de PUT/confirmar/anular/DELETE; `EstadoProducto` |
| 11 | Manejo de errores, negocio y fallback | `exception/GlobalExceptionHandler`, jerarquía `ExcepcionNegocio`, error decoder Feign, 400/404/409/503/500 sin stack trace |
| 12 | Auditoría (fecha, usuario, acciones) | `AuditoriaBase` + `UsuarioAuditoriaAware` + `FeignHeaderInterceptor` + `compra_auditoria` + `stock_movimientos` |

(El tema 9 aclara: la relación de compras a productos/proveedores se lee por `id` y la escritura de stock va por PATCH con JSON; ms-compra NO guarda entidades remotas en su base.)

### (c) Cumplimiento de la guía base

| Requisito de la guía base | ¿Cumplido? | Dónde |
|---|---|---|
| Proyectos creados con Spring Initializr | Sí | 5 módulos Maven con estructura y `wrapper` de Initializr (`mvnw.cmd`, `.gitignore`, `HELP.md` y test inicial `*ApplicationTests`) |
| Dependencias solicitadas (Boot 4.1.2, Cloud 2025.1.4, Java 21, MySQL, H2 (test), openfeign, eureka, gateway, validation) | Sí | `pom.xml` de cada proyecto (H2 queda con `scope=test`) |
| Paquetes y estructura por dominio (model, repository, services, controller, dto, client, config, exception) | Sí | árbol en README §4 |
| BD y puerto propios por microservicio | Sí | README §5 y YAML de cada ms |
| Colección Postman con la interfaz REST y header X-Usuario | Sí | `postman/coleccion-global.json` |
| DTO de entrada/salida y validaciones | Sí | records en `dto/` + Bean Validation |
| YAML con perfiles dev/prod (+test) en todos los servicios | Sí | `application.yaml`, `-dev.yaml`, `-test.yaml`, `-prod.yaml` (eureka-server y api-gateway sin YAML de BD) |
| Referencias entre bases por IDs (no crecen todos resto de FKs cruzadas) | Sí | README §10.1–10.2 |
| Prueba del Q flujo (registrar no descuenta, confirmar descuenta, anular devuelve) | Sí | carpeta 03 de la colección + test `AuditoriaCompraTest` |
| Extras (auditoría de compra, número C-AAAA, compensación, extras de contrato de precio, estado de producto/proveedor) | Sí | README §10.6–10.9 |
| Prueba de aceptación oficial (ACEPTACION — proveedor 1; producto 1 × 10 @ 12.50; producto 2 × 2 @ 189.90) | Sí | carpeta `03B Aceptacion` de `postman/coleccion-global.json` (subtotales 125.00 / 379.80; total 504.80; SQL de verificación en el propio request y en README §9) |

### (d) "Lista antes de entregar"

**En local (esta máquina):** [x] Compila todo: `mvnw.cmd test` en 3/3 ms queda SIEMPRE verde; [x] los 5 `.yaml` dev funcionan con `$env:DB_PASSWORD` definido; [x] reposulario con README, carpetas SQL y Postman limpias; [x] no hay credenciales reales (claves NO viajan en los YAML, solo `${DB_PASSWORD}`).

**Checklist para el día de la sustentación:**
1. `mvnw.cmd -q test` en los 3 microservicios: **BUILD SUCCESS, 27 tests verdes** (7 + 10 + 11).
2. Arrancar en orden: MySQL → Eureka → ms-proveedor → ms-producto → ms-compra → Gateway; totales de 5 apps con detalles y sin errores en el log de arranque.
3. Captura de Eureka: las 3 apps UP (8081, 8082, 8083) + captura del dashboard.
4. Captura de las 3 bases de datos en MySQL Workbench: tablas creadas con `sql/01_creacion_bases.sql` y datos demo; `compras` con `numero_compra` en formato C-2026-XXXXXX.
5. Postman: ejecutar la colección en orden → **carpetas 01–03 verdes, carpeta 04 verde (los errores... "rojos pero esperados"), carpeta 05 verde con ms-producto detenido (503)**.
6. Comprobar el flujo clave por el Gateway en vivo: stock antes = X → registrar = X → confirmar = X−cantidad → anular = X → auditoría con `X-Usuario: jose.orellana`.
7. Verificar en `bd_compra` que cada cambio de estado dejó su fila en `compra_auditoria` con el nombre de usuario del header.
8. Revisar que NINGÚN error deja expuesto un stack trace: probar con un 500 y con ms-producto caído (respuestas con `message` legible).

---

*Documento generado como parte del cierre del laboratorio. Autor: Orellana Rodriguez Jose Miguel — 04/11/2026.*
