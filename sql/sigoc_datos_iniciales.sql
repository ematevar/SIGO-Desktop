-- ============================================================
-- SIGOC — sigoc_datos_iniciales.sql
-- Roles según los tres perfiles del SRS (RF-01):
--   1. ADMIN (Administrador)
--   2. RESIDENTE (Residente de Obra)
--   3. ALMACENERO (Almacenero)
-- ============================================================
USE sigoc_db;

INSERT INTO rol(nombre_rol)
SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='ADMIN');

INSERT INTO rol(nombre_rol)
SELECT 'RESIDENTE' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='RESIDENTE');

INSERT INTO rol(nombre_rol)
SELECT 'ALMACENERO' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='ALMACENERO');

-- Cuenta inicial de Administrador (el sistema solicita cambiar la clave en el primer inicio)
INSERT INTO usuario(nombre_completo, usuario_login, contrasena_hash, id_rol, estado, activo)
SELECT 'Administrador SIGOC', 'admin', 'PBKDF2$120000$U0lHT0MtREVNT1NUUkFDSU9OLVNBTFQ=$rTzG6iZdG+6OFOOfY11QKZauNQ6Az1Yie4YbgDMcUT8=', id_rol, 1, 1
FROM rol WHERE nombre_rol='ADMIN' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario_login='admin');

INSERT INTO material(nombre, unidad_medida, stock_actual, stock_minimo, costo_unitario_ref)
SELECT 'Cemento Portland Tipo I', 'bolsa', 0, 20, 30.00 WHERE NOT EXISTS (SELECT 1 FROM material WHERE nombre='Cemento Portland Tipo I');

INSERT INTO material(nombre, unidad_medida, stock_actual, stock_minimo, costo_unitario_ref)
SELECT 'Arena gruesa', 'm3', 0, 5, 55.00 WHERE NOT EXISTS (SELECT 1 FROM material WHERE nombre='Arena gruesa');

INSERT INTO material(nombre, unidad_medida, stock_actual, stock_minimo, costo_unitario_ref)
SELECT 'Piedra chancada', 'm3', 0, 5, 75.00 WHERE NOT EXISTS (SELECT 1 FROM material WHERE nombre='Piedra chancada');
