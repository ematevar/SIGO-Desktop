-- ============================================================
-- SIGOC — sigoc_esquema.sql
-- Creación de la base de datos y las 16 tablas (Fase Desktop)
-- Incluye soporte para borrado lógico (activo, fecha_borrado)
-- ============================================================

CREATE DATABASE IF NOT EXISTS sigoc_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE sigoc_db;

CREATE TABLE IF NOT EXISTS rol (
  id_rol        INT AUTO_INCREMENT PRIMARY KEY,
  nombre_rol    VARCHAR(40) NOT NULL,
  activo        TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
  nombre_completo VARCHAR(120) NOT NULL,
  usuario_login   VARCHAR(40)  NOT NULL UNIQUE,
  contrasena_hash VARCHAR(255) NOT NULL,
  id_rol          INT NOT NULL,
  estado          TINYINT NOT NULL DEFAULT 1,
  activo          TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado   DATETIME NULL,
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS obra (
  id_obra           INT AUTO_INCREMENT PRIMARY KEY,
  nombre            VARCHAR(150) NOT NULL,
  cliente           VARCHAR(150) NOT NULL,
  ubicacion         VARCHAR(200),
  presupuesto_total DECIMAL(12,2) NOT NULL DEFAULT 0,
  fecha_inicio_plan DATE NOT NULL,
  fecha_fin_plan    DATE NOT NULL,
  estado            VARCHAR(20) NOT NULL DEFAULT 'Planificada',
  activo            TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado     DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS partida (
  id_partida          INT AUTO_INCREMENT PRIMARY KEY,
  id_obra             INT NOT NULL,
  nombre              VARCHAR(150) NOT NULL,
  unidad_medida       VARCHAR(20) NOT NULL,
  metrado_planificado DECIMAL(10,2) NOT NULL,
  peso_presupuestal   DECIMAL(5,2) NOT NULL,
  activo              TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado       DATETIME NULL,
  CONSTRAINT fk_partida_obra FOREIGN KEY (id_obra) REFERENCES obra(id_obra)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS avance_partida (
  id_avance         INT AUTO_INCREMENT PRIMARY KEY,
  id_partida        INT NOT NULL,
  fecha_reporte     DATE NOT NULL,
  porcentaje_avance DECIMAL(5,2) NOT NULL,
  observaciones     VARCHAR(255),
  id_usuario        INT NOT NULL,
  activo            TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado     DATETIME NULL,
  CONSTRAINT fk_avance_partida FOREIGN KEY (id_partida) REFERENCES partida(id_partida),
  CONSTRAINT fk_avance_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  CONSTRAINT chk_avance_porcentaje CHECK (porcentaje_avance BETWEEN 0 AND 100),
  INDEX idx_avance_partida_fecha(id_partida, fecha_reporte)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS material (
  id_material        INT AUTO_INCREMENT PRIMARY KEY,
  nombre             VARCHAR(120) NOT NULL,
  unidad_medida      VARCHAR(20) NOT NULL,
  stock_actual       DECIMAL(10,2) NOT NULL DEFAULT 0,
  stock_minimo       DECIMAL(10,2) NOT NULL DEFAULT 0,
  costo_unitario_ref DECIMAL(10,2) NOT NULL DEFAULT 0,
  activo             TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado      DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS proveedor (
  id_proveedor  INT AUTO_INCREMENT PRIMARY KEY,
  razon_social  VARCHAR(150) NOT NULL,
  ruc           VARCHAR(11) NOT NULL UNIQUE,
  telefono      VARCHAR(20),
  direccion     VARCHAR(200),
  activo        TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS compra_material (
  id_compra          INT AUTO_INCREMENT PRIMARY KEY,
  id_proveedor       INT NOT NULL,
  tipo_comprobante   VARCHAR(20) NOT NULL DEFAULT 'Factura',
  fecha_compra       DATE NOT NULL,
  numero_comprobante VARCHAR(30) NOT NULL,
  total              DECIMAL(12,2) NOT NULL DEFAULT 0,
  activo             TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado      DATETIME NULL,
  CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS detalle_compra (
  id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
  id_compra       INT NOT NULL,
  id_material     INT NOT NULL,
  cantidad        DECIMAL(10,2) NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  subtotal        DECIMAL(12,2) NOT NULL,
  activo          TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado   DATETIME NULL,
  CONSTRAINT fk_detalle_compra FOREIGN KEY (id_compra) REFERENCES compra_material(id_compra),
  CONSTRAINT fk_detalle_material FOREIGN KEY (id_material) REFERENCES material(id_material)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS consumo_material (
  id_consumo    INT AUTO_INCREMENT PRIMARY KEY,
  id_partida    INT NOT NULL,
  id_material   INT NOT NULL,
  fecha_consumo DATE NOT NULL,
  cantidad      DECIMAL(10,2) NOT NULL,
  id_usuario    INT NOT NULL,
  activo        TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado DATETIME NULL,
  CONSTRAINT fk_consumo_partida FOREIGN KEY (id_partida) REFERENCES partida(id_partida),
  CONSTRAINT fk_consumo_material FOREIGN KEY (id_material) REFERENCES material(id_material),
  CONSTRAINT fk_consumo_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  INDEX idx_consumo_material_fecha(id_material, fecha_consumo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS empleado (
  id_empleado       INT AUTO_INCREMENT PRIMARY KEY,
  nombres_apellidos VARCHAR(150) NOT NULL,
  dni               VARCHAR(8) NOT NULL UNIQUE,
  cargo             VARCHAR(60) NOT NULL,
  jornal_diario     DECIMAL(8,2) NOT NULL,
  fecha_ingreso     DATE NOT NULL,
  activo            TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado     DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS asistencia (
  id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
  id_empleado   INT NOT NULL,
  id_obra       INT NOT NULL,
  fecha         DATE NOT NULL,
  hora_entrada  TIME,
  hora_salida   TIME,
  estado        VARCHAR(15) NOT NULL,
  activo        TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado DATETIME NULL,
  CONSTRAINT fk_asistencia_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id_empleado),
  CONSTRAINT fk_asistencia_obra FOREIGN KEY (id_obra) REFERENCES obra(id_obra),
  INDEX idx_asistencia_obra_fecha(id_obra, fecha)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS produccion_empleado (
  id_produccion      INT AUTO_INCREMENT PRIMARY KEY,
  id_empleado        INT NOT NULL,
  id_partida         INT NOT NULL,
  fecha              DATE NOT NULL,
  cantidad_producida DECIMAL(10,2) NOT NULL,
  activo             TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado      DATETIME NULL,
  CONSTRAINT fk_produccion_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id_empleado),
  CONSTRAINT fk_produccion_partida FOREIGN KEY (id_partida) REFERENCES partida(id_partida)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cierre_planilla (
  id_cierre            INT AUTO_INCREMENT PRIMARY KEY,
  id_obra              INT NOT NULL,
  fecha_inicio_periodo DATE NOT NULL,
  fecha_fin_periodo    DATE NOT NULL,
  fecha_cierre         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  monto_total          DECIMAL(12,2) NOT NULL DEFAULT 0,
  id_usuario           INT NOT NULL,
  activo               TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado        DATETIME NULL,
  CONSTRAINT fk_cierre_obra FOREIGN KEY (id_obra) REFERENCES obra(id_obra),
  CONSTRAINT fk_cierre_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ingreso (
  id_ingreso    INT AUTO_INCREMENT PRIMARY KEY,
  id_obra       INT NOT NULL,
  fecha         DATE NOT NULL,
  concepto      VARCHAR(150) NOT NULL,
  monto         DECIMAL(12,2) NOT NULL,
  activo        TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado DATETIME NULL,
  CONSTRAINT fk_ingreso_obra FOREIGN KEY (id_obra) REFERENCES obra(id_obra)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS egreso (
  id_egreso     INT AUTO_INCREMENT PRIMARY KEY,
  id_obra       INT NOT NULL,
  fecha         DATE NOT NULL,
  tipo_egreso   VARCHAR(20) NOT NULL,
  concepto      VARCHAR(150) NOT NULL,
  monto         DECIMAL(12,2) NOT NULL,
  id_compra     INT NULL,
  id_cierre     INT NULL,
  activo        TINYINT(1) NOT NULL DEFAULT 1,
  fecha_borrado DATETIME NULL,
  CONSTRAINT fk_egreso_obra FOREIGN KEY (id_obra) REFERENCES obra(id_obra),
  CONSTRAINT fk_egreso_compra FOREIGN KEY (id_compra) REFERENCES compra_material(id_compra),
  CONSTRAINT fk_egreso_cierre FOREIGN KEY (id_cierre) REFERENCES cierre_planilla(id_cierre),
  INDEX idx_egreso_obra_fecha(id_obra, fecha)
) ENGINE=InnoDB;
