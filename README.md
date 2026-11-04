# Laboratorio de Arquitectura de Microservicios – Sistema de Compras

**Autor:** Orellana Rodriguez Jose Miguel
**Fecha:** 04 de noviembre de 2026
**Ciclo:** Cuarto ciclo

---

## 1. Título y autor

Sistema de compras distribuido con arquitectura de microservicios, implementado con **Java 21**, **Spring Boot 4.1.2** y **Spring Cloud 2025.1.4**. El laboratorio completo está organizado en 5 proyectos Maven generados desde **Spring Initializr**:

| Proyecto | Rol | Puerto | Base de datos |
|---|---|---|---|
| `eureka-server` | Servidor de registro y descubrimiento | 8761 | — |
| `ms-proveedor` | Microservicio de negocio: proveedores | 8081 | `bd_proveedor` |
| `ms-producto` | Microservicio de negocio: productos e inventario | 8082 | `bd_producto` |
| `ms-compra` | Microservicio de negocio: proceso de compra | 8083 | `bd_compra` |
| `api-gateway` | Puerta de entrada única (enrutado con balanceo) | 8080 | — |

## 2. Objetivo

Construir un sistema de compras distribuido donde **cada microservicio tenga su propia base de datos**, se comuniquen por **HTTP/JSON vía OpenFeign**, se descubran entre sí por **Eureka**, y el cliente acceda **solo por el Gateway**. El proceso de compra implementa un ciclo de vida con estados (`REGISTRADA → CONFIRMADA → ANULADA`), **compensación manual de stock** cuando una llamada remota falla a mitad del flujo, **auditoría de usuario por header** y un **formato de error global** idéntico en los 3 microservicios.

Todo el tráfico de pruebas pasa por el **Gateway (8080)**: ningún cliente llama a un microservicio directamente.

## 3. Requisitos

- **JDK 21** (el proyecto declara Java 21 en los `pom.xml`; también compila con JDKs posteriores instalados en la máquina).
- **Maven**: los proyectos incluyen wrapper (`mvnw.cmd`), no hace falta instalarlo.
- **MySQL 8** en ejecución local, con usuario/contraseña que el desarrollador configure (ver perfiles).
- **Postman** (versión con soporte de colecciones v2.1) para probar la colección global incluida.
- Opcional: `git` para revisar el historial.

> Credenciales: NO hay secretos en el repositorio. La contraseña de MySQL se define por variable de entorno `DB_PASSWORD` (o por el archivo `application-dev.yaml` del perfil dev local).

## 4. Estructura del repositorio

```
EVA03WEB/
├─ eureka-server/          # registro de servicios (puerto 8761)
├─ api-gateway/            # puerta única (puerto 8080)
├─ ms-proveedor/           # CRUD de proveedores (8081, bd_proveedor)
├─ ms-producto/            # CRUD de productos + inventario (8082, bd_producto)
├─ ms-compra/              # proceso de compra (8083, bd_compra)
├─ sql/01_creacion_bases.sql  # esquema + datos semilla de las 3 bases
├─ postman/coleccion-global.json  # colección Postman global
└─ docs/CONCLUSIONES_Y_SUSTENTACION.md  # conclusiones, guía de sustentación, checklist
```

### Diagrama de componentes

```mermaid
flowchart LR
    C(["Cliente / Postman"]) -->|HTTP :8080| G["API Gateway<br/>(Netty, sin StripPrefix)"]
    G -->|lb://MS-PROVEEDOR| P["ms-proveedor :8081<br/>bd_proveedor"]
    G -->|lb://MS-PRODUCTO| R["ms-producto :8082<br/>bd_producto"]
    G -->|lb://MS-COMPRA| K["ms-compra :8083<br/>bd_compra"]
    K -->|Feign GET /api/proveedores/{id}| P
    K -->|Feign GET /api/productos/{id}<br/>PATCH .../stock| R
    R -->|Feign GET /api/proveedores/{id}| P
    P --> D1[("bd_proveedor<br/>MariaDB")]
    R --> D2[("bd_producto<br/>MariaDB")]
    K --> D3[("bd_compra<br/>MariaDB")]
    E["Eureka :8761"] -.->|registro/descubrimiento| P
    E -.->|registro/descubrimiento| R
    E -.->|registro/descubrimiento| K
    E -.->|consultar rutas| G
```

Los tres microservicios se comunican por HTTP/JSON con Feign (resuelto por Eureka); **no hay FK cruzadas entre bases**: los ids (`Long`) son la única relación.

Dentro de cada microservicio, el código sigue la misma estructura de paquetes:

```
com.orellana.microservicios.ms_xxx
├─ model/       # entidades JPA, enums, auditoría y generadores
├─ config/      # auditoría JPA, interceptor Feign, error decoder
├─ dto/         # records de entrada/salida (DTO) y mapper
├─ client/      # clientes OpenFeign hacia otros microservicios
├─ repository/  # interfaces Spring Data JPA
├─ services/    # interfaz + impl de casos de uso (@Transactional)
├─ controller/  # API REST y GlobalExceptionHandler
└─ exception/   # jerarquía de excepciones y formato global de error
```

## 5. Puertos y bases de datos

| Microservicio | Puerto | Base MySQL | Tablas |
|---|---|---|---|
| ms-proveedor | 8081 | `bd_proveedor` | `proveedores` |
| ms-producto | 8082 | `bd_producto` | `productos`, `stock_movimientos` |
| ms-compra | 8083 | `bd_compra` | `compras`, `compra_detalles`, `compra_auditoria` |

**No hay claves foráneas entre bases de datos**: cada microservicio referencia entidades de otros dominios solo por `id` (`Long`). Esta es una decisión de diseño para mantener la independencia de despliegue (ver sección 10). Las FK internas a la misma base sí existen (por ejemplo `compra_detalles.compra_id → compras.id`).

## 6. Perfiles de configuración

Cada microservicio tiene 4 archivos YAML:

| Archivo | Uso |
|---|---|
| `application.yaml` | base: nombre de la app, `spring.profiles.active` (por defecto `dev`) |
| `application-dev.yaml` | desarrollo: MySQL local, `ddl-auto: update`, timeouts Feign cortos |
| `application-test.yaml` | pruebas: **H2 en memoria**, `ddl-auto: create-drop`, `eureka.client.enabled: false` |
| `application-prod.yaml` | producción: todos los valores por variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `EUREKA_URL`), `ddl-auto: validate` |

> **Por qué no se usó Config Server:** la guía base no lo exige y con perfiles YAML + variables de entorno se logra lo mismo sin una pieza central extra que fallaría por sí misma en producción. Queda documentado como extensión futura.

### Qué cambia entre dev y prod

| Aspecto | dev (perfil por defecto) | prod |
|---|---|---|
| Base de datos | `jdbc:mysql://localhost:3306/bd_*` (local) | URL completa por variable `${DB_URL}` (sin valor por defecto) |
| Credenciales | `${DB_USER:root}` / `${DB_PASSWORD:}` (local XAMPP: root sin clave) | Sin valor por defecto: `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}` (el arranque **falla** si faltan) |
| `ddl-auto` | `update` (crea/modifica esquema al arrancar) | `validate` (solo verifica; nunca toca el esquema) |
| `show-sql` | `true` (SQL visible con `format_sql`) | `false` |
| Logging del SQL | activado a través de `show-sql`/`format_sql` | desactivado (no se vuelca SQL a los logs) |
| URL de Eureka | `${EUREKA_URL:http://localhost:8761/eureka}` (localhost) | `${EUREKA_URL}` (sin default; obligatoria) |
| Puertos | fijos 8081/8082/8083 | parametrizados (`${MS_*_PORT}`, `${GATEWAY_PORT}`) |
| H2 | solo en `test` (`create-drop`) | no existe (nunca se ejecuta en prod) |

## 7. Pasos de ejecución (EN ESTE ORDEN)

```text
0) MySQL: crear las 3 bases y datos semilla
   > mysql -u root -p < sql/01_creacion_bases.sql

1) Eureka (8761)
   cd eureka-server && .\mvnw.cmd spring-boot:run
   → http://localhost:8761

2) ms-proveedor (8081)
   cd ms-proveedor && .\mvnw.cmd spring-boot:run

3) ms-producto (8082)
   cd ms-producto && .\mvnw.cmd spring-boot:run

4) ms-compra (8083)
   cd ms-compra && .\mvnw.cmd spring-boot:run

5) API Gateway (8080)
   cd api-gateway && .\mvnw.cmd spring-boot:run
```

El orden importa: **MySQL → Eureka → los 3 microservicios → Gateway**. Los microservicios se registran en Eureka al arrancar; el Gateway resuelve por nombre de servicio con LoadBalancer.

**Ejecutar pruebas unitarias de cada microservicio** (perfil `test`, H2, sin MySQL ni Eureka):

```text
cd ms-proveedor; .\mvnw.cmd test     → 7/7 verdes
cd ms-producto;  .\mvnw.cmd test     → 10/10 verdes
cd ms-compra;    .\mvnw.cmd test     → 11/11 verdes
```

## 8. Endpoints (SIEMPRE por el Gateway en el 8080)

### Gateways de ruta
Ninguna ruta del Gateway aplica `StripPrefix`: el path del cliente ES el path interno del microservicio.

### ms-proveedor (via Gateway: 8080)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/proveedores` | listar todos |
| GET | `/api/proveedores/{id}` | obtener por id (404 si no existe) |
| GET | `/api/proveedores/ruc/{ruc}` | buscar por RUC |
| POST | `/api/proveedores` | crear (201) |
| PUT | `/api/proveedores/{id}` | actualizar |
| PATCH | `/api/proveedores/{id}/estado` | cambiar estado `{"estado":"INACTIVO"}` |
| DELETE | `/api/proveedores/{id}` | eliminar (baja lógica previa si tiene productos) |

### ms-producto (via Gateway: 8080)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | listar todos |
| GET | `/api/productos/{id}` | obtener por id |
| GET | `/api/productos/proveedor/{proveedorId}` | listar por proveedor |
| POST | `/api/productos` | crear (valida proveedor remoto ACTIVO) |
| PUT | `/api/productos/{id}` | actualizar |
| PATCH | `/api/productos/{id}/estado` | cambiar estado |
| DELETE | `/api/productos/{id}` | eliminar |
| PATCH | `/api/productos/{id}/stock?motivo=...` | ajustar stock `{"cantidad":n,"operacion":"DESCONTAR|INCREMENTAR"}` — deja movimiento en `stock_movimientos` |
| GET | `/api/productos/{id}/movimientos` | historial de stock (quién, qué y cuánto) |

### ms-compra (via Gateway: 8080)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/compras` | listar todas |
| GET | `/api/compras/{id}` | obtener por id |
| GET | `/api/compras/proveedor/{proveedorId}` | listar por proveedor |
| POST | `/api/compras` | **registrar** (201, estado `REGISTRADA`, NO descuenta stock) |
| PUT | `/api/compras/{id}` | modificar (solo si `REGISTRADA`) |
| POST | `/api/compras/{id}/confirmar` | **confirmar** (descuenta stock; 409 con compensación si falla) |
| POST | `/api/compras/{id}/anular` | **anular** (devuelve stock si estaba `CONFIRMADA`) |
| DELETE | `/api/compras/{id}` | eliminar (solo si `ANULADA`) |
| GET | `/api/compras/{id}/auditoria` | historial de estados y de quién hizo cada acción |

**Header de usuario (auditoría):** todas las peticiones pueden incluir `X-Usuario: jose.orellana`. Si no viaja, la auditoría registra `sistema`. El interceptor Feign propaga ese header hacia ms-proveedor y ms-producto.

## 9. Cómo probar en Postman

1. Abrir Postman → **Import** → `postman/coleccion-global.json` (las variables ya están embebidas: `baseUrl`, `proveedorId`, `productoId`, `cantidad`, `compraId`, `stockAntes`).
2. Ejecutar en orden las carpetas:
   - **01 Proveedor** → listar/obtener/crear/cambiar estado.
   - **02 Producto** → listar/obtener/crear, ajuste manual de stock y movimientos.
   - **03 Compra - FLUJO COMPLETO** → el flujo del checkpoint: registrar (no descuenta) → confirmar (descuenta) → verificar stock descontado → anular (devuelve) → verificar stock devuelto → auditoría con el usuario del header.
    - **03B Aceptacion (prueba de aceptacion oficial)** - **ACEPTACION - Compra con dos productos y total**: proveedor 1, producto 1 x 10 a 'precioUnitario' 12.50 y producto 2 x 2 a 189.90; 'pm.test' verifica subtotales '125.00' y '379.80' y total '504.80'. No descuenta stock (queda REGISTRADA).
   - **04 Errores esperados** → validaciones (400), 404, proveedor inactivo (409), stock insuficiente (409), productos repetidos (409), modificar una ANULADA (409), DELETE de la ANULADA (204). El rojo en esta carpeta **pasa**, porque se espera el error.
   - **05 Servicio caído** → detener `ms-producto` y ejecutar: ms-compra responde **503** con el formato global y **sin stack trace**.
3. Cada petición lleva pruebas `pm.test` que validan el contrato JSON y guardan variables para la siguiente petición.

Verificacion SQL de la prueba de aceptacion (cliente mysql o Workbench):

```sql
-- Detalle de la compra recien creada (2 lineas)
SELECT d.producto_id, d.cantidad, d.precio_unitario, d.subtotal
FROM bd_compra.compra_detalles d
WHERE d.compra_id = (SELECT MAX(id) FROM bd_compra.compras)
ORDER BY d.producto_id;
-- subtotal 1 = 125.00 ; subtotal 2 = 379.80

-- Total de la compra
SELECT total
FROM bd_compra.compras
WHERE id = (SELECT MAX(id) FROM bd_compra.compras);
-- total = 504.80
```


Verificación directa del stock (sin Postman), por el Gateway:

```text
GET http://localhost:8080/api/productos/1        → stock antes
POST http://localhost:8080/api/compras (registrar) → stock IGUAL
POST http://localhost:8080/api/compras/1/confirmar → stock MENOS la cantidad
POST http://localhost:8080/api/compras/1/anular    → stock IGUAL al inicio
GET http://localhost:8080/api/compras/1/auditoria  → REGISTRADA, CONFIRMADA, ANULADA + usuario
```

## 10. Decisiones de diseño

1. **Bases de datos separadas por dominio (3 MySQL).** Cada microservicio es dueño de sus datos y podría desplegarse, escalarse o caerse sin afectar a los demás. MySQL permitiría FK entre bases porque están en el mismo servidor, pero **no se usan** por una decisión deliberada: nacería un acoplamiento físico entre "microservicios" que en producción estarían en bases distintas. La integridad se comprueba en tiempo de ejecución (ms-compra valida proveedor y productos vía Feign).

2. **Referencias entre dominios por `id`, no por FK.** `compras.proveedor_id`, `compra_detalles.producto_id` y `productos.proveedor_id` son `Long` simples. Las únicas FK son **internas a la misma base** (reales y necesarias: `compra_detalles → compras`, `compra_auditoria → compras`).

3. **OpenFeign + LoadBalancer + Eureka.** Los clientes se declaran por **nombre de servicio** (`@FeignClient(name = "MS-PRODUCTO")`) y Eureka resuelve la instancia. Principios **I** (interfaces de 1–2 métodos) y **D** (ms-compra depende de abstracciones, no de implementaciones).

4. **Ciclo de vida de la compra — Decisión J:** crear la compra como `REGISTRADA` que **NO descuenta stock**; `confirmar` aplica `OPERACION_AL_CONFIRMAR` (DESCONTAR); `anular` devuelve stock solo si estaba `CONFIRMADA`. Esto da un punto de revisión antes de afectar inventario y hace el flujo auditable.

5. **Compensación manual, no transaccionalidad distribuida.** El rollback local **no deshace llamadas remotas**: si `confirmar` descuenta el producto 1 y el producto 2 no logra descontarlo por falta de stock, la transacción local puede deshacer `compras`/`compra_auditoria`, pero el descuento del producto 1 ya ocurrió en otra BD (y otro proceso). Por eso `confirmar` vuelve a llamar a ms-producto con la operación **inversa** (`OPERACION_AL_CONFIRMAR.inversa()` = INCREMENTAR) por cada producto descontado — patrón de **compensación** — y responde 409. Se eligió compensación explícita antes que un gestor de transacciones distribuidas (2PC/XA) porque el laboratorio no incluye ese middleware y la compensación es legible, diagnóstica y apropiada para 3 servicios.

6. **Auditoría en la misma transacción.** Cada cambio de estado escribe su fila en `compra_auditoria` dentro de la **misma** transacción `@Transactional`: si algo revierte, el historial no queda desincronizado (ningún estado visible sin su rastro). El usuario se toma del header `X-Usuario` (`AuditorAware` + interceptor Feign que lo propaga a los demás servicios). En ms-producto, además, cada ajuste deja un `stock_movimientos` con stock anterior/nuevo y usuario.

7. **Baja lógica en proveedor y producto.** El `DELETE` físico está protegido: el estado es `ACTIVO | INACTIVO` (PUT/PATCH), y las reglas de ms-compra solo aceptan entidades `ACTIVO`.

8. **Extras sobre la guía base:** número de compra `C-2026-000001` (`NumeroCompraGenerator`, secuencial por año con respaldo de unique + reintento) y formatos de error/dto idénticos entre los 3 servicios. El **precioUnitario de compra lo envía el cliente** (`> 0`, máx 2 decimales) y **puede diferir del precio de referencia del catálogo** (`Producto.precio`); el precio de catálogo NO se copia a la compra.

9. **Cambio en vivo preparado (sustentación):** la operación que `confirmar` aplica en ms-producto vive en **una sola constante**: `com.orellana.microservicios.ms_compra.services.impl.CompraServiceImpl.OPERACION_AL_CONFIRMAR` (`DESCONTAR`). Invertirla (`INCREMENTAR`) cambia el comportamiento de confirmar/anular **sin tocar el resto del código**, porque la anulación ya usa la inversa, no una operación hardcodeada.

## 11. Problemas conocidos y limitaciones

- **Credenciales de MySQL:** la máquina local no acepta `root/root`; los YAML dev usan `${DB_USER}`/`${DB_PASSWORD}`. En una instalación nueva ejecutar `mysql -u root -p < sql/01_creacion_bases.sql` y definir las variables de entorno antes de arrancar.
- **Primer arranque lento:** Maven descarga dependencias la primera vez (~minutos). Con el repo local caliente, pocos segundos.
- **Spring Boot DevTools** está presente en dev: al cambiar código se reinicia el servicio automáticamente; si Eureka aún no refleja la instancia puede verse un 503 momentáneo.
- **Eureka y el Gateway con "no instances":** si se arranca el Gateway antes que los microservicios, las primeras peticiones dan 503 hasta que Eureka propaga los registros (algunos segundos).
- **Compensación de confirmar es best-effort:** si incluso la llamada de compensación falla (por ejemplo, ms-producto se cae en ese instante), el descuento podría quedar huérfano y quedan los `LOG.severe` del momento de compensar. Un sistema de verdad añadiría reintentos en colas o un registro de compensaciones pendientes (fuera del alcance del laboratorio).
- **Tests usan H2**, no MySQL: cubren la lógica (reglas, compensación, auditoría) pero no el dialecto MySQL en sí; el esquema real se valida con `ddl-auto: update` en dev.
- **Concurrencia de datos:** ms-compra valida dentro de su flujo transaccional y ms-producto bloquea por producto (`findByIdForUpdate`, `PESSIMISTIC_WRITE`), pero dos `confirmar` simultáneos de la misma compra no tienen bloqueo local en `compras`: el estado final no puede volverse negativo porque el stock lo impide ms-producto, y la unique de `numero_compra` impide duplicados; aun así, un segundo `confirmar` de la misma compra quedaría detenido por la regla de ciclo de vida (solo REGISTRADA se confirma), que se evalúa primero.

## 12. Núcleo exigido por la guía N.° 03 y extras (entregables aparte)

La guía oficial (Evaluación N.° 03, laboratorio "Proveedores y compras") define el núcleo mínimo. Lo demás es material adicional que puede entregarse aparte.

### 12.1 Núcleo exigido — todo lo siguiente está CUMPLIDO

| Núcleo (obligatorio) | Evidencia |
|---|---|
| 3 microservicios (proveedor, producto, compra) | README §5; puertos 8081/8082/8083; `bd_proveedor`, `bd_producto`, `bd_compra` |
| Referencias por id (sin FK entre bases) | `compra.proveedorId`, `compraDetalle.productoId`, `producto.proveedorId` como `Long`; FK solo internas |
| DTOs de entrada/salida y Bean Validation | `dto/*.java` (records) + `@Valid` en controladores + mensajes en español |
| YAML dev / prod / **test** (H2, eureka off en test) | `application*.yaml` en cada servicio (5/5); perfil por defecto `dev` |
| Fail-fast de secretos en prod (sin defaults) | `application-prod.yaml`: `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`, `${EUREKA_URL}` sin valores por defecto |
| Prueba de aceptación oficial | carpeta `03B Aceptacion` en la colección Postman (`ACEPTACION - Compra con dos productos y total`); subtotales `125.00`/`379.80`, total `504.80`; SQL de verificación en el propio request y en README §9 |
| Colección Postman con rutas exitosas y fallidos | `postman/coleccion-global.json` (carpetas 01–03B verdes, 04 roja-pero-esperada, 05 con servicio caído) |
| README (este documento) + diagrama | README §4–§13; diagrama Mermaid en §4 |
| SQL con estructura de las 3 bases | `sql/01_creacion_bases.sql` (creadas y migradas con datos semilla 3/6/5 filas) |
| Conclusiones y guía de sustentación | `docs/CONCLUSIONES_Y_SUSTENTACION.md` (incluye respuesta modelo oficial de 1 min y guion de sustentación de 5–8 min) |
| Información inválida de la API → 400 | `GlobalExceptionHandler` + `@Valid` en cascada (`cantidad: 0` daría 400; probado) |

### 12.2 Extras (entregables aparte; no bloquean el núcleo)

| Extra | Dónde |
|---|---|
| Eureka (registro y descubrimiento) | `eureka-server`, puerto 8761 |
| API Gateway (puerta única 8080, sin StripPrefix) | `api-gateway` |
| OpenFeign + cliente resiliente + error decoder | `client/` y `config/FeignErrorDecoder.java` en ms-producto y ms-compra |
| Auditoría detallada (`compra_auditoria`, actor por `X-Usuario`, `stock_movimientos`) | `CompraAuditoria`, `StockMovimiento`, `GET /api/compras/{id}/auditoria`, `GET /api/productos/{id}/movimientos` |
| Control de stock (registrar no descuenta, confirmar descuenta, anular devuelve, compensación) | `CompraServiceImpl` + `OperacionStock` + lock pesimista nativo en ms-producto |
| Panel web del laboratorio | `docs/panel.html` |
| Colección Postman global (flujo completo y 5 carpetas) | `postman/coleccion-global.json` |

## 13. Evidencias de la rúbrica

- Compila, arranca los 5 proyectos y pasa todos los tests (27 verdes en total: 7 + 10 + 11).
- Colección Postman con flujo completo pasando por el Gateway y errores esperados.
- `sql/01_creacion_bases.sql`: esquema con 3 bases, datos semilla y queries de verificación.
- Conclusiones individuales y guía de sustentación en `docs/CONCLUSIONES_Y_SUSTENTACION.md`.

---

*Laboratorio realizado por Orellana Rodriguez Jose Miguel, siguiendo la guía base del curso y el CONTEXTO COMÚN del enunciado.*
# EVA03WEB
