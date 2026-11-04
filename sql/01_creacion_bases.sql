-- =====================================================================
-- 01_creacion_bases.sql  |  MySQL 8+  |  utf8mb4
-- Bases separadas por microservicio (decisión de diseño: SIN FK entre
-- bases; MySQL las permitiría en el mismo servidor, pero no se usan
-- para mantener independencia de despliegue y bajo acoplamiento).
-- Compatible con ddl-auto=validate de Hibernate (nombres snake_case).
-- =====================================================================

-- =====================================================================
-- 1) bd_proveedor
-- =====================================================================
DROP DATABASE IF EXISTS bd_proveedor;
CREATE DATABASE bd_proveedor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE bd_proveedor;

CREATE TABLE proveedores (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  ruc                VARCHAR(11)  NOT NULL,
  razon_social       VARCHAR(150) NOT NULL,
  nombre_comercial   VARCHAR(150) NULL,
  email              VARCHAR(120) NULL,
  telefono           VARCHAR(20)  NULL,
  direccion          VARCHAR(200) NULL,
  estado             VARCHAR(20)  NOT NULL,
  -- columnas de auditoría (AuditoriaBase)
  fecha_creacion     DATETIME(6)  NOT NULL,
  fecha_modificacion DATETIME(6)  NOT NULL,
  creado_por         VARCHAR(60)  NOT NULL,
  modificado_por     VARCHAR(60)  NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_proveedores_ruc UNIQUE (ruc),
  CONSTRAINT chk_proveedores_estado      CHECK (estado IN ('ACTIVO','INACTIVO')),
  CONSTRAINT chk_proveedores_ruc_digits  CHECK (ruc REGEXP '^[0-9]{11}$')
) ENGINE = InnoDB;

-- =====================================================================
-- 2) bd_producto
-- =====================================================================
DROP DATABASE IF EXISTS bd_producto;
CREATE DATABASE bd_producto
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE bd_producto;

CREATE TABLE productos (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  codigo             VARCHAR(30)   NOT NULL,
  nombre             VARCHAR(150)  NOT NULL,
  descripcion        VARCHAR(500)  NULL,
  precio             DECIMAL(12,2) NOT NULL,
  stock              INT           NOT NULL,
  stock_minimo       INT           NOT NULL,
  categoria          VARCHAR(80)   NULL,
  estado             VARCHAR(20)   NOT NULL,
  proveedor_id       BIGINT        NOT NULL,   -- SOLO el ID, sin FK entre bases
  -- columnas de auditoría (AuditoriaBase)
  fecha_creacion     DATETIME(6)   NOT NULL,
  fecha_modificacion DATETIME(6)   NOT NULL,
  creado_por         VARCHAR(60)   NOT NULL,
  modificado_por     VARCHAR(60)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_productos_codigo          UNIQUE (codigo),
  KEY idx_productos_proveedor_id   (proveedor_id),
  CONSTRAINT chk_productos_estado         CHECK (estado IN ('ACTIVO','INACTIVO')),
  CONSTRAINT chk_productos_precio_pos     CHECK (precio > 0),
  CONSTRAINT chk_productos_stock_no_neg   CHECK (stock >= 0),
  CONSTRAINT chk_productos_stockmin_no_neg CHECK (stock_minimo >= 0)
) ENGINE = InnoDB;

CREATE TABLE stock_movimientos (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  producto_id    BIGINT        NOT NULL,     -- FK INTERNA a esta misma base (OK)
  operacion      VARCHAR(20)   NOT NULL,
  cantidad       INT           NOT NULL,
  stock_anterior INT           NOT NULL,
  stock_nuevo    INT           NOT NULL,
  usuario        VARCHAR(60)   NOT NULL,
  fecha          DATETIME(6)   NOT NULL,
  motivo         VARCHAR(200)  NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_stockmov_producto      FOREIGN KEY (producto_id) REFERENCES productos (id),
  KEY idx_stockmov_producto     (producto_id),
  CONSTRAINT chk_stockmov_operacion    CHECK (operacion IN ('DESCONTAR','INCREMENTAR')),
  CONSTRAINT chk_stockmov_cantidad_pos CHECK (cantidad > 0),
  CONSTRAINT chk_stockmov_stock_nuevo  CHECK (stock_nuevo >= 0)
) ENGINE = InnoDB;

-- =====================================================================
-- 3) bd_compra
-- =====================================================================
DROP DATABASE IF EXISTS bd_compra;
CREATE DATABASE bd_compra
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE bd_compra;

CREATE TABLE compras (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  numero_compra      VARCHAR(20)   NOT NULL,
  proveedor_id       BIGINT        NOT NULL,   -- referencia por ID: sin FK entre bases
  fecha              DATETIME(6)   NOT NULL,   -- fecha de NEGOCIO (distingue de fecha_creacion)
  estado             VARCHAR(20)   NOT NULL,
  total              DECIMAL(12,2) NOT NULL,
  observaciones      VARCHAR(300)  NULL,
  -- columnas de auditoría (AuditoriaBase)
  fecha_creacion     DATETIME(6)   NOT NULL,
  fecha_modificacion DATETIME(6)   NOT NULL,
  creado_por         VARCHAR(60)   NOT NULL,
  modificado_por     VARCHAR(60)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_compras_numero          UNIQUE (numero_compra),
  KEY idx_compras_proveedor_id   (proveedor_id),
  CONSTRAINT chk_compras_estado         CHECK (estado IN ('REGISTRADA','CONFIRMADA','ANULADA'))
) ENGINE = InnoDB;

CREATE TABLE compra_detalles (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  compra_id       BIGINT        NOT NULL,    -- FK INTERNA a esta misma base (OK)
  producto_id     BIGINT        NOT NULL,    -- referencia por ID: sin FK entre bases
  cantidad        INT           NOT NULL,
  precio_unitario DECIMAL(12,2) NOT NULL,    -- precio de compra enviado en el detalle del pedido (> 0, máx 2 decimales)
  subtotal        DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_detalles_compra       FOREIGN KEY (compra_id) REFERENCES compras (id),
  KEY idx_detalles_compra      (compra_id),
  KEY idx_detalles_producto    (producto_id),
  CONSTRAINT chk_detalles_cantidad_pos CHECK (cantidad > 0)
) ENGINE = InnoDB;

CREATE TABLE compra_auditoria (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  compra_id       BIGINT       NOT NULL,   -- FK INTERNA a esta misma base (OK)
  accion          VARCHAR(20)  NOT NULL,
  estado_anterior VARCHAR(20)  NULL,
  estado_nuevo    VARCHAR(20)  NOT NULL,
  usuario         VARCHAR(60)  NOT NULL,
  fecha           DATETIME(6)  NOT NULL,
  detalle         VARCHAR(300) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_compraud_compra    FOREIGN KEY (compra_id) REFERENCES compras (id),
  KEY idx_compraud_compra   (compra_id),
  CONSTRAINT chk_compraud_accion   CHECK (accion IN ('REGISTRADA','ACTUALIZADA','CONFIRMADA','ANULADA'))
) ENGINE = InnoDB;

-- =====================================================================
-- 4) Datos de prueba (creado_por = 'sistema', IDs consistentes entre bases)
--    proveedores 1,2 ACTIVO / 3 INACTIVO
--    productos: 2 y 3 bajo stock_minimo, 6 INACTIVO
--    compra 1 REGISTRADA: 2 unidades PRD-001 (95.00) + 10 unidades PRD-002 (7.50)
--      subtotales 190.00 + 75.00 = total 265.00  (verificado abajo)
-- =====================================================================
USE bd_proveedor;
INSERT INTO proveedores (id, ruc, razon_social, nombre_comercial, email, telefono, direccion, estado, fecha_creacion, fecha_modificacion, creado_por, modificado_por) VALUES
(1, '20100047110', 'INNOVA DISTRIBUCIONES S.A.C.', 'Innova Distribuciones', 'ventas@innova.pe', '01-2345678', 'Av. Argentina 1450, Callao', 'ACTIVO',   '2026-10-20 09:00:00.000000', '2026-10-20 09:00:00.000000', 'sistema', 'sistema'),
(2, '20481234560', 'SUMINISTROS ANDINOS E.I.R.L.', 'Suministros Andinos',   'contacto@andinos.pe', '01-8765432', 'Jr. Union 800, Lima',    'ACTIVO',   '2026-10-20 09:05:00.000000', '2026-10-20 09:05:00.000000', 'sistema', 'sistema'),
(3, '20551234567', 'COMERCIAL PACIFICO SUR S.A.', 'Pacifico Sur',          'ventas@pacificosur.pe', '01-1112223', 'Av. Espana 500, Lima',  'INACTIVO', '2026-10-20 09:10:00.000000', '2026-10-20 09:10:00.000000', 'sistema', 'sistema');

USE bd_producto;
INSERT INTO productos (id, codigo, nombre, descripcion, precio, stock, stock_minimo, categoria, estado, proveedor_id, fecha_creacion, fecha_modificacion, creado_por, modificado_por) VALUES
(1, 'PRD-001', 'Arroz Superior 25 kg',   'Saco de arroz extra superior 25 kg', 95.00, 120, 20, 'Abarrotes', 'ACTIVO',   1, '2026-10-20 10:00:00.000000', '2026-10-20 10:00:00.000000', 'sistema', 'sistema'),
(2, 'PRD-002', 'Aceite Vegetal 1 L',     'Botella aceite vegetal 1 litro',     7.50,  15, 30, 'Abarrotes', 'ACTIVO',   1, '2026-10-20 10:01:00.000000', '2026-10-20 10:01:00.000000', 'sistema', 'sistema'),
(3, 'PRD-003', 'Azucar Estilizada 1 kg', 'Azucar blanca estilizada 1 kg',      4.20,   0, 10, 'Abarrotes', 'ACTIVO',   1, '2026-10-20 10:02:00.000000', '2026-10-20 10:02:00.000000', 'sistema', 'sistema'),
(4, 'PRD-004', 'Leche Evaporada 400 g',  'Lata leche evaporada 400 g',         5.90, 200, 40, 'Lacteos',   'ACTIVO',   2, '2026-10-20 10:03:00.000000', '2026-10-20 10:03:00.000000', 'sistema', 'sistema'),
(5, 'PRD-005', 'Fideos Spaghetti 1 kg',  'Paquete fideos spaghetti 1 kg',      4.80, 150, 25, 'Abarrotes', 'ACTIVO',   2, '2026-10-20 10:04:00.000000', '2026-10-20 10:04:00.000000', 'sistema', 'sistema'),
(6, 'PRD-006', 'Atun en Aceite 170 g',   'Lata atun en aceite 170 g',          6.30,  90, 20, 'Conservas', 'INACTIVO', 2, '2026-10-20 10:05:00.000000', '2026-10-20 10:05:00.000000', 'sistema', 'sistema');

INSERT INTO stock_movimientos (id, producto_id, operacion, cantidad, stock_anterior, stock_nuevo, usuario, fecha, motivo) VALUES
(1, 1, 'INCREMENTAR', 120, 0, 120, 'sistema', '2026-10-20 10:00:00.000000', 'Carga inicial de inventario');

USE bd_compra;
INSERT INTO compras (id, numero_compra, proveedor_id, fecha, estado, total, observaciones, fecha_creacion, fecha_modificacion, creado_por, modificado_por) VALUES
(1, 'C-2026-000001', 1, '2026-10-21 10:30:00.000000', 'REGISTRADA', 265.00, 'Compra de prueba del laboratorio', '2026-10-21 10:30:05.000000', '2026-10-21 10:30:05.000000', 'sistema', 'sistema');

INSERT INTO compra_detalles (id, compra_id, producto_id, cantidad, precio_unitario, subtotal) VALUES
(1, 1, 1,  2, 95.00, 190.00),
(2, 1, 2, 10,  7.50,  75.00);

INSERT INTO compra_auditoria (id, compra_id, accion, estado_anterior, estado_nuevo, usuario, fecha, detalle) VALUES
(1, 1, 'REGISTRADA', NULL, 'REGISTRADA', 'sistema', '2026-10-21 10:30:05.000000', 'Compra registrada con datos de prueba');

-- =====================================================================
-- 5) Consultas de verificación (para capturas)
-- =====================================================================
-- Proveedores: 2 ACTIVO, 1 INACTIVO
-- SELECT id, ruc, razon_social, estado FROM bd_proveedor.proveedores;

-- Productos con bajo stock mínimo (PRD-002 y PRD-003 deben salir)
-- SELECT id, codigo, stock, stock_minimo, (stock <= stock_minimo) AS bajo_stock_minimo
-- FROM bd_producto.productos;

-- Coherencia del total de la compra 1: ambas columnas deben dar 265.00
-- SELECT c.total AS total_guardado, SUM(d.subtotal) AS suma_detalles
-- FROM bd_compra.compras c
-- JOIN bd_compra.compra_detalles d ON d.compra_id = c.id
-- WHERE c.id = 1 GROUP BY c.total;

-- Historial de auditoría de la compra 1
-- SELECT accion, estado_anterior, estado_nuevo, usuario, fecha, detalle
-- FROM bd_compra.compra_auditoria WHERE compra_id = 1 ORDER BY fecha;
