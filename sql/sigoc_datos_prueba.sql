-- ============================================================
-- SIGOC - Datos de Prueba (Transaccional)
-- Ejecutar este archivo poblara el sistema con datos realistas
-- y consistentes (Stock, Finanzas, Avances, etc.)
-- ============================================================
USE sigoc_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE egreso;
TRUNCATE TABLE ingreso;
TRUNCATE TABLE produccion_empleado;
TRUNCATE TABLE asistencia;
TRUNCATE TABLE consumo_material;
TRUNCATE TABLE compra_material;
TRUNCATE TABLE avance_partida;
TRUNCATE TABLE partida;
TRUNCATE TABLE empleado;
TRUNCATE TABLE obra;
TRUNCATE TABLE material;
TRUNCATE TABLE proveedor;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Proveedores
INSERT INTO proveedor (id_proveedor, razon_social, ruc, telefono, direccion) VALUES
(1, 'Ferretería San Martín SAC', '20456789123', '052-411222', 'Av. Bolognesi 450, Tacna'),
(2, 'Distribuidora Constructor Tacna EIRL', '20567891234', '052-423555', 'Av. Collpa 812, Tacna'),
(3, 'Materiales del Sur SAC', '20678912345', '052-455009', 'Calle Zela 233, Tacna');

-- 2. Materiales (El stock inicial es el remanente despues de las compras y consumos)
INSERT INTO material (id_material, nombre, unidad_medida, stock_actual, stock_minimo, costo_unitario_ref) VALUES
(1, 'Cemento Portland Tipo I', 'bolsa', 300, 50, 26.50),
(2, 'Fierro corrugado 1/2 pulg.', 'varilla', 450, 100, 32.00),
(3, 'Ladrillo King Kong 18 huecos', 'unidad', 5000, 2000, 0.85),
(4, 'Arena gruesa', 'm3', 25, 10, 45.00),
(5, 'Arena fina', 'm3', 0, 10, 50.00),
(6, 'Piedra chancada 1/2', 'm3', 0, 10, 48.00),
(7, 'Alambre negro N°16', 'kg', 0, 20, 5.50),
(8, 'Clavos para madera 3 pulg.', 'kg', 0, 15, 6.00),
(9, 'Madera tornillo (cuartón)', 'unidad', 0, 20, 12.00),
(10, 'Pintura látex (galón)', 'galón', 0, 10, 38.00);

-- 3. Obras
INSERT INTO obra (id_obra, nombre, cliente, ubicacion, presupuesto_total, fecha_inicio_plan, fecha_fin_plan, estado) VALUES
(1, 'Construcción Colegio San José', 'Ministerio de Educación', 'Av. Las Flores 123', 1500000.00, '2026-09-01', '2027-05-31', 'En Ejecución'),
(2, 'Pavimentación Calle Real', 'Municipalidad Provincial', 'Calle Real cdras 1-5', 450000.00, '2026-09-15', '2027-01-30', 'En Ejecución');

-- 4. Partidas
INSERT INTO partida (id_partida, id_obra, nombre, unidad_medida, metrado_planificado, peso_presupuestal) VALUES
(1, 1, 'Movimiento de tierras', 'm3', 500.00, 10.00),
(2, 1, 'Vaciado de zapatas y cimientos', 'm3', 150.00, 25.00),
(3, 1, 'Muros de ladrillo KK', 'm2', 800.00, 30.00),
(4, 2, 'Trazo y replanteo topográfico', 'm2', 1200.00, 5.00),
(5, 2, 'Colocación de carpeta asfáltica', 'm2', 1200.00, 60.00);

-- 5. Empleados
INSERT INTO empleado (id_empleado, nombres_apellidos, dni, cargo, jornal_diario, fecha_ingreso) VALUES
(1, 'Juan Perez Mendoza', '40123456', 'Maestro de Obra', 120.00, '2026-09-01'),
(2, 'Carlos Ramirez Soto', '41234567', 'Operario', 90.00, '2026-09-01'),
(3, 'Luis Mamani Quispe', '42345678', 'Peón', 60.00, '2026-09-01'),
(4, 'Ana Rojas Silva', '43456789', 'Ingeniera Residente', 200.00, '2026-09-01');

-- 6. Transacciones: Ingresos financieros (Adelantos)
INSERT INTO ingreso (id_obra, fecha, concepto, monto) VALUES
(1, '2026-09-01', 'Adelanto directo (30%)', 450000.00),
(1, '2026-09-15', 'Adelanto de materiales', 100000.00),
(2, '2026-09-15', 'Adelanto directo (20%)', 90000.00);

-- 7. Transacciones: Compras de material (Cabecera)
INSERT INTO compra_material (id_compra, id_proveedor, tipo_comprobante, fecha_compra, numero_comprobante, total) VALUES
(1, 1, 'Factura', '2026-09-02', 'F001-000456', 13000.00),
(2, 2, 'Factura', '2026-09-03', 'F002-000123', 18900.00),
(3, 3, 'Factura', '2026-09-05', 'F003-000789', 6400.00),
(4, 1, 'Boleta', '2026-09-16', 'B001-000111', 1760.00);

-- 7.1 Detalles de Compra
INSERT INTO detalle_compra (id_compra, id_material, cantidad, precio_unitario, subtotal) VALUES
(1, 1, 500, 26.00, 13000.00),  -- Cemento
(2, 2, 600, 31.50, 18900.00),  -- Fierro
(3, 3, 8000, 0.80, 6400.00),   -- Ladrillo
(4, 4, 40, 44.00, 1760.00);    -- Arena gruesa

-- 8. Transacciones: Egresos (Relacionados a las compras + planilla simulada)
INSERT INTO egreso (id_obra, fecha, tipo_egreso, concepto, monto, id_compra, id_cierre) VALUES
(1, '2026-09-02', 'COMPRA', 'Compra material (Fac: F001-000456)', 13000.00, 1, NULL),
(1, '2026-09-03', 'COMPRA', 'Compra material (Fac: F002-000123)', 18900.00, 2, NULL),
(1, '2026-09-05', 'COMPRA', 'Compra material (Fac: F003-000789)', 6400.00, 3, NULL),
(2, '2026-09-16', 'COMPRA', 'Compra material (Bol: B001-000111)', 1760.00, 4, NULL),
(1, '2026-09-30', 'MANUAL', 'Pago de planilla - Setiembre', 11000.00, NULL, NULL);

-- 9. Transacciones: Consumos de material (Disminuye stock)
INSERT INTO consumo_material (id_material, id_partida, id_usuario, fecha_consumo, cantidad) VALUES
(1, 2, 1, '2026-09-10', 200),
(2, 2, 1, '2026-09-11', 150),
(3, 3, 1, '2026-09-15', 3000),
(4, 5, 1, '2026-09-20', 15);

-- 10. Transacciones: Avance de Partida
INSERT INTO avance_partida (id_partida, fecha_reporte, porcentaje_avance, observaciones, id_usuario) VALUES
(1, '2026-09-05', 40.00, 'Movimiento de tierras sector A completado', 1),
(1, '2026-09-12', 100.00, 'Movimiento de tierras finalizado', 1),
(2, '2026-09-15', 35.00, 'Vaciado de cimientos zapata 1 a 5', 1),
(3, '2026-09-18', 20.00, 'Avance en pabellón principal', 1),
(4, '2026-09-17', 100.00, 'Trazo topográfico concluido', 1);

-- 11. Transacciones: Asistencia
INSERT INTO asistencia (id_empleado, id_obra, fecha, hora_entrada, hora_salida, estado) VALUES
(1, 1, '2026-09-15', '07:55:00', '17:05:00', 'PRESENTE'),
(2, 1, '2026-09-15', '08:00:00', '17:00:00', 'PRESENTE'),
(3, 1, '2026-09-15', '08:15:00', '17:00:00', 'TARDANZA'),
(4, 1, '2026-09-15', '07:50:00', '18:00:00', 'PRESENTE'),
(1, 1, '2026-09-16', '07:55:00', '17:00:00', 'PRESENTE'),
(2, 1, '2026-09-16', '08:00:00', '17:00:00', 'PRESENTE'),
(3, 1, '2026-09-16', null, null, 'FALTA');

-- 12. Transacciones: Producción (Destajo)
INSERT INTO produccion_empleado (id_empleado, id_partida, fecha, cantidad_producida) VALUES
(2, 3, '2026-09-15', 15.50),
(2, 3, '2026-09-16', 18.00);
