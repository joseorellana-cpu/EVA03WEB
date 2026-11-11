# Frontend · Proveedores y Compras

**Laboratorio de Arquitectura de Microservicios — Orellana Rodriguez Jose Miguel**

SPA en Vue 3 (Composition API con `<script setup>`) + Vite, JavaScript (sin TypeScript),
Vue Router, Pinia, Vitest + @vue/test-utils, ESLint + Prettier.
**Sin librerías de UI ni de HTTP: CSS propio de `src/assets/main.css` y la API fetch
nativa** — menos dependencias = menos superficie de ataque. El frontend habla **sólo**
con el API Gateway (en dev vía proxy de Vite `/api → http://localhost:8080`; en producción
vía nginx proxy inverso al mismo origen).

> Comportamiento de precios: la guía N.° 03 obliga a enviar `precioUnitario` en cada
> detalle de la compra (`{ productoId, cantidad, precioUnitario }`). Es el **precio de
> compra** (puede diferir del precio de referencia del catálogo); el catálogo no se
> copia. El cliente jamás envía `total`, `estado`, `creadoPor` ni `subtotal`.

## Requisitos

- Node.js **22.18+** o **24.12+** (ver `engines` en `package.json`).
- npm **10+** (recomendado 11).

## Instalación

```bash
cd frontend
npm install
```

La primera vez se generan `node_modules/` y `package-lock.json` (versionado).

## Variables de entorno

Sólo variables no sensibles. Copiar y ajustar:

```bash
cp .env.example .env   # en Windows: copy .env.example .env
```

| Variable | Defecto | Uso |
|---|---|---|
| `VITE_API_BASE_URL` | *(vacío = mismo origen)* | Base URL del Gateway en producción. |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Destino del proxy de Vite en desarrollo. |

Ningún secreto en `.env` ni en el código. `.env*` está ignorado por git (excepto `.env.example`).

## Ejecución (desarrollo)

```bash
# 1) Levantar el backend: MySQL → Eureka (8761) → ms-* (8081/8082/8083) → Gateway (8080)
# 2) Arrancar el frontend (puerto 5173 por defecto)
npm run dev
```

Vite sirve el frontend en **http://localhost:5173** y **proxyea `/api`** al Gateway, así
no hace falta CORS y el frontend nunca llama a `8081/8082/8083` directamente.

## Pruebas y Calidad

```bash
npm run test       # suite completa de Vitest
npm run build      # bundle de producción SIN source maps (sourcemap:false)
npm run lint       # eslint + oxlint
npm run format     # prettier
```

**Test de seguridad visible en la suite:**

- `src/utils/__tests__/money.spec.js` (36 tests): centavos, casos límite (0.10+0.20,
  `9007199254740993`, `12.505`, negativos, cadenas vacías).
- `src/components/__tests__/apiClient.spec.js` (10): X-Usuario, 400 con `details`,
  409, 503, timeout, fallos de red.
- `src/components/__tests__/EstadoBadge.spec.js`: `"<img src=x onerror=alert(1)>"`
  se muestra como **TEXTO** (no se ejecuta).
- `src/components/__tests__/useAccionUnica.spec.js`: dos clics = una sola petición.
- `src/components/__tests__/CompraNuevaView.spec.js`: el body **nunca** lleva `total`,
  `estado`, `creadoPor` ni `subtotal`.

## Despliegue

1. **Build** de producción:
   ```bash
   npm run build          # sin source maps
   ```
2. **Servir `dist/` con nginx** (ver [`nginx.conf`](nginx.conf)): SPA fallback,
   proxy `/api → Gateway` al mismo origen, CSP estricta
   (`default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self';
   frame-ancestors 'none'; base-uri 'self'; form-action 'self'`), `nosniff`,
   `Referrer-Policy`, `Permissions-Policy`, `X-Frame-Options: DENY` y HSTS solo
   con HTTPS.

## Decisiones de diseño

- **Por qué sin librerías de UI ni HTTP:** menos dependencias = menos riesgo de
  CVE y de XSS/inyección. El CSS propio cubre tablas, formularios, diálogos y badges;
  `fetch` nativo cubre la capa de red con `AbortController`.
- **Por qué centavos:** los flotantes de JS no representan bien los decimales
  (`0.1 + 0.2` ≠ `0.3`); toda la aritmética monetaria se hace en **enteros de centavos**
  y se valida con regex de máximo 2 decimales y `isSafeInteger`.
- **Por qué guardián de doble envío:** el usuario puede doble-clicar. El botón se
  bloquea mientras la petición sigue abierta. **Esto no sustituye la idempotencia
  del servidor** (salvo invalidar tokens/keys); la protección real es server-side.
- **Por qué el total es estimado:** el frontend no es autoridad. El total mostrado
  es informativo y se etiqueta "est."; el backend recalcula y devuelve el real.
- **Por qué proxy y no CORS:** la interfaz no necesita pedir CORS al Gateway.
  En dev Vite proxyea `/api → :8080`; en prod nginx hace lo mismo al mismo origen.
- **Por qué identidad simulada:** el laboratorio no exige autenticación real.
  Se envía `X-Usuario` como **identidad falsificable** documentada en la UI y en el
  README; el backend graba lo que llega. El área de usuario vive en memoria
  (`sessionStorage` como respaldo, **nunca** `localStorage`).
- **Por qué autovector de errores único:** todo error pasa por el `apiClient`
  y se normaliza a `{ status, message, details[], esRed, esServicioNoDisponible }`.
  Para 503/fallo de red el mensaje es genérico en español; nunca se imprimen
  trazas ni objetos crudos.

## Limitaciones conocidas

- No hay autenticación real (identidad simulada; `X-Usuario` falsificable).
- La idempotencia real depende del servidor; el guardián solo evita dobles clics.
- Total mostrado es **estimado**; el real lo devuelve el backend.
- En desarrollo el tráfico es HTTP local sin TLS (dominio de laboratorio).
- `npm audit` reporta 11 moderadas **solo en devDependencies** (eslint, vite devtools, jsdom); en producción el bundle es 0 vulnerabilidades.
- Fuente/estilo del panel de nginx: depende de la infraestructura donde se despliegue.

## Conclusiones técnicas (primera persona)

1. **La aritmética monetaria no admite flotantes:** descubrí que `parsearMonto` debe
   trabajar en centavos enteros y usar regex estrictamente; `0.1 + 0.2` ya no es `0.3`
   y puede perderse un centavo en facturación. [src/utils/money.js]
2. **El render seguro empieza por diseño, no por filtros:** prohibir `v-html` en
   ESLint y usar `{{ texto }}` evitó un XSS incluso cuando el payload
   (`<img onerror>`) llegó del servidor. [src/components/EstadoBadge.vue y eslint.config.js]
3. **Deshabilitar el botón no es idempotencia:** el guardián de doble envío
   previene el doble clic, pero la protección real debe estar en el backend;
   documentarlo evita que la interfaz se vuelva una falsa sensación de seguridad.
   [src/composables/useAccionUnica.js]
4. **El cliente no es autoridad:** al enviar solo `{ proveedorId, observaciones,
   detalles: [{ productoId, cantidad, precioUnitario }] }`, el backend es el único
   que calcula el total; la UI aporta UX, nunca negocio. [src/views/compras/CompraNuevaView.vue]
5. **El proxy de desarrollo es la solución más segura a CORS:** servir `/api → 8080`
   en Vite (y el mismo patrón en nginx) elimina el problema sin tocar el backend
   ni aprender la política de origen cruzado. [vite.config.js y nginx.conf]
6. **Dificultad con su solución:** los tests de `CompraNuevaView` colgaban porque
   `createPinia`/`setActivePinia` debían importarse de `pinia`, no de `vitest`; al
   corregir el import y añadir `flushPromises`, la suite quedó en verde sin
   emboscar el código de producción. [src/components/__tests__/CompraNuevaView.spec.js]

## Guía de sustentación

### 8 preguntas típicas

1. **¿Por qué no se usa TypeScript?** Se optó por JavaScript para menor fricción en
   el laboratorio; el stack pide JS. TS sería trivial de añadir, pero aquí importa más
   la revisión de seguridad del runtime.
2. **¿Por qué no hay librerías de UI (Vuetify/PrimeVue)?** Menos dependencias = menos
   superficie de ataque y menos CVE. El CSS propio cubre lo necesario (tablas,
   diálogos, badges) y es auditable línea por línea.
3. **¿Por qué no una librería HTTP (axios)?** `fetch` nativo es suficiente con
   `AbortController` para timeout, y el contrato de errores es nuestro, no del library.
4. **¿Queda alguna superficie XSS?** No: se prohíbe `v-html`/`innerHTML`/`eval`
   por ESLint y todo dato se renderiza con interpolación `{{ }}`; el test
   anti-XSS (`<img onerror>`) lo garantiza.
5. **¿Dónde guardas el usuario simulado?** En memoria (Pinia) y respaldo en
   `sessionStorage` (pestaña). Nunca en `localStorage`, nunca un token sensible.
6. **¿Qué significa "total estimado"?** El frontend suma en centavos para orientar
   al usuario, pero el total real lo devuelve el backend. La etiqueta "est." lo avisa.
7. **¿Cómo evitas el doble envío de una compra?** Un guardián deshabilita el botón
   mientras la petición sigue en curso; la idempotencia real (si hace falta) la debe
   dar el servidor.
8. **¿Por qué proxy y no CORS?** Porque el problema no es CORS: es arquitectura.
   Proxy de Vite en dev (`/api → :8080`) y nginx en prod al mismo origen.

### 3 cambios en vivo

**A. Nueva columna en el listado (p.ej. "fecha de registro" en Proveedores)**
1. `src/views/proveedores/ProveedorListView.vue`: añadir columna en el `<table>`
   y enlazar con `p.fechaRegistro` (ya viene del backend).
2. Si se ordena, añadir `sortable: true` en `src/components/TablaDatos.vue`.
3. `npm run dev` → recargar → verificar orden y formato con `Intl`.
   *Por qué es seguro:* solo render de texto; no se toca la capa API.

**B. Nueva validación en el formulario (p.ej. RUC que no empiece por 0)**
1. Ya existe `src/utils/validators.js#validarRuc` → al agregar una regla, añade el
   test en `src/utils/__tests__/validators.spec.js`.
2. Reutiliza el mismo validador en `ProveedorFormView.vue`.
3. Servidor sigue siendo la autoridad: el error 400 del backend se muestra en
   `FormErroresServidor.vue` con su `message` y `details`.
   *Por qué:* espejo de UX, no regla de negocio del cliente.

**C. Nuevo filtro por estado en Compras (ya existe) y extenderlo**
1. Ampliar `filtroEstado` en `src/views/compras/CompraListView.vue` con otra
   opción del `<select>` (`CONFIRMADA` ya existe; añadir `ANULADA` si se quiere
   visibilizar de forma independiente).
2. La filtrado es local sobre la respuesta del backend; no modifica el endpoint.
3. Para servidor-side: adaptar `compraApi.js#listar` con querystring y que el
   backend lo soporte (implementar un path `/api/compras?estado=…` si se desea).
   *Por qué:* sin cambiar la capa API, sin permisos especiales.
