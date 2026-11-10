package Negocio;

import config.ClsConexionBD;
import config.ClsConstantes;
import Entidad.ClsUsuario;
import NegocioDAO.ClsAvanceDAO;
import NegocioDAO.ClsCatalogoDAO;
import NegocioDAO.ClsMaterialDAO;
import NegocioDAO.ClsOperacionDAO;
import NegocioDAO.ClsUsuarioDAO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Suite completa de pruebas unitarias y de integración del SRS V3/V4 para SIGOC Desktop
 * (RF-01 a RF-16 y RNF-03 a RNF-08).
 */
public class SigocSrsComplianceTest {

    // =========================================================================
    // RF-01 y RF-02: Autenticación, Captcha, Bloqueo 5 intentos, Clave inicial y Roles
    // =========================================================================

    @Test
    public void testRf01CaptchaIncorrectoBloqueo5IntentosYClaveInicial() throws Exception {
        ClsCaptchaService captcha = new ClsCaptchaService();
        String code = captcha.generar();
        Assert.assertEquals(6, code.length());
        Assert.assertTrue(captcha.validar(code, code.toLowerCase()));
        Assert.assertFalse(captcha.validar(code, "XXXXXX"));

        ClsUsuarioDAO fakeDao = new ClsUsuarioDAO() {
            @Override
            public ClsUsuario autenticar(String u, String p) {
                if ("admin".equals(u) && "ClaveSegura123".equals(p)) {
                    return new ClsUsuario(1, "admin", "ADMIN", true);
                }
                return null;
            }
        };
        ClsUsuarioService svc = new ClsUsuarioService(fakeDao, captcha);

        // Captcha incorrecto debe rechazar (intento 1)
        try {
            svc.iniciarSesion("admin", "ClaveSegura123", code, "WRONGC");
            Assert.fail("Debió rechazar captcha incorrecto");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().toLowerCase().contains("captcha"));
        }

        // Login válido detecta requiereCambioClave para el admin sembrado
        ClsUsuario sesion = svc.iniciarSesion("admin", "ClaveSegura123", code, code);
        Assert.assertNotNull(sesion);
        Assert.assertEquals("ADMIN", sesion.getRol());
        Assert.assertTrue("El admin inicial debe activar requiereCambioClave", sesion.isRequiereCambioClave());

        // 5 intentos fallidos seguidos activan bloqueo temporal
        for (int i = 1; i <= 5; i++) {
            try {
                svc.iniciarSesion("admin", "ClaveErronea" + i, code, code);
                Assert.fail("Debió fallar intento " + i);
            } catch (IllegalArgumentException expected) {
                // esperado
            }
        }
        try {
            svc.iniciarSesion("admin", "ClaveSegura123", code, code);
            Assert.fail("El 6to intento inmediato debe ser rechazado por bloqueo temporal");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue("Debe informar bloqueo temporal", ex.getMessage().contains("Demasiados intentos fallidos"));
        }
    }

    @Test
    public void testRf01RestriccionPorRolEnServicios() throws Exception {
        ClsUsuarioService usrSvc = new ClsUsuarioService(new ClsUsuarioDAO(), new ClsCaptchaService());
        try {
            usrSvc.listar("RESIDENTE");
            Assert.fail("Residente no debe poder listar usuarios");
        } catch (SecurityException expected) {
            Assert.assertTrue(expected.getMessage().contains("Acceso denegado"));
        }
    }

    @Test
    public void testRf02ImpedirDesactivarUnicoAdministrador() throws Exception {
        ClsUsuarioDAO fakeDao = new ClsUsuarioDAO() {
            @Override
            public Map<String, Object> obtenerUsuarioRol(int idUsuario) {
                return Map.of("nombre_rol", "ADMIN", "activo", true);
            }

            @Override
            public int contarAdminsActivos() {
                return 1;
            }
        };
        ClsUsuarioService svc = new ClsUsuarioService(fakeDao, new ClsCaptchaService());
        try {
            svc.cambiarEstado(1, false, "ADMIN");
            Assert.fail("No debe permitir desactivar al único Administrador activo");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("único Administrador"));
        }
    }

    // =========================================================================
    // RF-03 y RF-04: Gestión de Obras y Partidas
    // =========================================================================

    @Test
    public void testRf03ObraPresupuestoMayorACeroYNoEliminarConMovimientos() throws Exception {
        ClsCatalogoDAO fakeDao = new ClsCatalogoDAO() {
            @Override
            public boolean obraTieneMovimientos(int idObra) {
                return idObra == 10;
            }

            @Override
            public void guardar(String nombre, String pk, Object id, Map<String, Object> values) {
                // OK
            }
        };
        ClsCatalogoService svc = new ClsCatalogoService(fakeDao);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Obra Test");
        datos.put("cliente", "Cliente Test");
        datos.put("presupuesto_total", "0");
        datos.put("fecha_inicio_plan", "2026-09-01");
        datos.put("fecha_fin_plan", "2026-12-31");

        try {
            svc.guardar("obra", "id_obra", null, datos, "ADMIN");
            Assert.fail("Presupuesto 0 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("mayor que cero"));
        }

        try {
            svc.eliminar("obra", "id_obra", 10, "ADMIN");
            Assert.fail("Obra con movimientos no debe eliminarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RF-03"));
        }
    }

    @Test
    public void testRf04PartidaPesosSumenMaximo100YObraActiva() throws Exception {
        ClsCatalogoDAO fakeDao = new ClsCatalogoDAO() {
            @Override
            public boolean esObraActiva(int idObra) {
                return idObra == 1;
            }

            @Override
            public double sumaPesosPartidas(int idObra, Object idPartidaExcluir) {
                return 80.0;
            }

            @Override
            public void guardar(String nombre, String pk, Object id, Map<String, Object> values) {
                // OK
            }
        };
        ClsCatalogoService svc = new ClsCatalogoService(fakeDao);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id_obra", 1);
        datos.put("nombre", "Partida Exceso");
        datos.put("unidad_medida", "m3");
        datos.put("metrado_planificado", 100);
        datos.put("peso_presupuestal", 21.0); // 80 + 21 = 101 > 100

        try {
            svc.guardar("partida", "id_partida", null, datos, "RESIDENTE");
            Assert.fail("Suma de pesos 101% debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("100 %"));
        }

        // Metrado 0 o peso 0 rechazado
        datos.put("peso_presupuestal", 0);
        try {
            svc.guardar("partida", "id_partida", null, datos, "RESIDENTE");
            Assert.fail("Peso 0 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("mayor que cero"));
        }

        // Obra inactiva rechazada
        datos.put("id_obra", 99);
        datos.put("peso_presupuestal", 15.0);
        try {
            svc.guardar("partida", "id_partida", null, datos, "RESIDENTE");
            Assert.fail("Obra inactiva debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("obra activa"));
        }
    }

    // =========================================================================
    // RF-05: Avance físico y avance acumulado de la obra
    // =========================================================================

    @Test
    public void testRf05AvanceFisicoNoDisminuyeYSoloResidente() throws Exception {
        ClsAvanceDAO fakeDao = new ClsAvanceDAO() {
            @Override
            public Double ultimoPorcentaje(int idPartida) {
                return 90.0;
            }

            @Override
            public double registrar(int idPartida, LocalDate fecha, double porcentaje, String observaciones, int idUsuario) {
                return 45.5;
            }
        };
        ClsAvanceService svc = new ClsAvanceService(fakeDao);

        try {
            svc.registrar(1, LocalDate.now(), 95.0, "Obs", 1, "ALMACENERO");
            Assert.fail("Almacenero no puede registrar avance");
        } catch (SecurityException expected) {
            // OK
        }

        try {
            svc.registrar(1, LocalDate.now(), 80.0, "Obs", 1, "RESIDENTE");
            Assert.fail("Avance menor al anterior debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("90.0"));
        }

        try {
            svc.registrar(1, LocalDate.now(), 101.0, "Obs", 1, "RESIDENTE");
            Assert.fail("Avance > 100% debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("100 %"));
        }

        double avanceObra = svc.registrar(1, LocalDate.now(), 95.0, "Avance OK", 1, "RESIDENTE");
        Assert.assertEquals(45.5, avanceObra, 0.01);
    }

    // =========================================================================
    // RF-06, RF-07 y RF-08: Materiales, Compras y Consumos
    // =========================================================================

    @Test
    public void testRf06BajaMaterialConStockMayorACeroRechazada() throws Exception {
        ClsCatalogoDAO fakeDao = new ClsCatalogoDAO() {
            @Override
            public double stockActualMaterial(int idMaterial) {
                return 25.0;
            }
        };
        ClsCatalogoService svc = new ClsCatalogoService(fakeDao);
        try {
            svc.eliminar("material", "id_material", 1, "ALMACENERO");
            Assert.fail("No debe permitir baja de material con stock > 0");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("stock mayor a cero"));
        }
    }

    @Test
    public void testRf07CompraValidacionesDeProveedorCantidadPrecioYComprobante() throws Exception {
        ClsOperacionDAO fakeDao = new ClsOperacionDAO() {
            @Override
            public void registrarCompra(int proveedor, int obra, int material, String tipoComprobante, String comprobante, LocalDate fecha, double cantidad, double precio) {
                if (proveedor == 99) {
                    throw new IllegalArgumentException("El proveedor seleccionado no existe o se encuentra inactivo (RF-07).");
                }
                if ("F001-000456".equalsIgnoreCase(comprobante)) {
                    throw new IllegalArgumentException("Ya existe una compra registrada con el comprobante " + comprobante + " para este proveedor.");
                }
            }
        };
        ClsOperacionService svc = new ClsOperacionService(fakeDao);

        // Cantidad 0 rechazada
        try {
            svc.compra(1, 1, 1, "Factura", "F001-999", LocalDate.now(), 0, 25.0, "ALMACENERO");
            Assert.fail("Cantidad 0 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("cantidad o precio"));
        }

        // Precio 0 rechazado
        try {
            svc.compra(1, 1, 1, "Factura", "F001-999", LocalDate.now(), 10, 0, "ALMACENERO");
            Assert.fail("Precio 0 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("cantidad o precio"));
        }

        // Proveedor inactivo rechazado
        try {
            svc.compra(99, 1, 1, "Factura", "F001-999", LocalDate.now(), 10, 25.0, "ALMACENERO");
            Assert.fail("Proveedor inactivo debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("inactivo"));
        }

        // Comprobante duplicado rechazado
        try {
            svc.compra(1, 1, 1, "Factura", "F001-000456", LocalDate.now(), 10, 25.0, "ALMACENERO");
            Assert.fail("Comprobante duplicado debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("F001-000456"));
        }
    }

    @Test
    public void testRf08AlertaStockIgualMinimoYObraInactivaRechazada() throws Exception {
        ClsMaterialDAO fakeDao = new ClsMaterialDAO() {
            @Override
            public void registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad) {
                if (partida == 99) {
                    throw new IllegalArgumentException("Solo se puede registrar consumo de materiales en partidas de una obra activa (RF-08).");
                }
            }

            @Override
            public double stockMinimo(int material) {
                return 0.0; // stock_actual == stock_minimo -> diferencia 0.0
            }
        };
        ClsMaterialService svc = new ClsMaterialService(fakeDao);
        boolean alerta = svc.registrarConsumo(1, 1, 2, LocalDate.now(), 10.0, "RESIDENTE");
        Assert.assertTrue("Debe disparar alerta cuando el stock queda exactamente igual al mínimo (<= 0)", alerta);

        try {
            svc.registrarConsumo(1, 99, 2, LocalDate.now(), 5.0, "RESIDENTE");
            Assert.fail("Partida de obra inactiva debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("obra activa"));
        }
    }

    // =========================================================================
    // RF-09 y RF-10: Proveedores (RUC único) y Trabajadores (DNI único, jornal > 0)
    // =========================================================================

    @Test
    public void testRf09ProveedorRucDuplicado() throws Exception {
        ClsCatalogoDAO fakeDao = new ClsCatalogoDAO() {
            @Override
            public boolean existeRuc(String ruc, Object idExcluir) {
                return "20456789123".equals(ruc);
            }
        };
        ClsCatalogoService svc = new ClsCatalogoService(fakeDao);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("razon_social", "Empresa Duplicada");
        d.put("ruc", "20456789123");
        try {
            svc.guardar("proveedor", "id_proveedor", null, d, "ADMIN");
            Assert.fail("RUC duplicado debe rechazarse con mensaje claro");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RUC 20456789123"));
        }
    }

    @Test
    public void testRf10TrabajadorDniDuplicadoYJornalCero() throws Exception {
        ClsCatalogoDAO fakeDao = new ClsCatalogoDAO() {
            @Override
            public boolean existeDni(String dni, Object idExcluir) {
                return "40123456".equals(dni);
            }
        };
        ClsCatalogoService svc = new ClsCatalogoService(fakeDao);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("nombres_apellidos", "Juan Test");
        d.put("dni", "40123456");
        d.put("cargo", "Operario");
        d.put("jornal_diario", 90.0);
        d.put("fecha_ingreso", "2026-09-01");
        try {
            svc.guardar("empleado", "id_empleado", null, d, "ADMIN");
            Assert.fail("DNI duplicado debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("DNI 40123456"));
        }

        d.put("dni", "99887766");
        d.put("jornal_diario", 0);
        try {
            svc.guardar("empleado", "id_empleado", null, d, "ADMIN");
            Assert.fail("Jornal 0 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("mayor que cero"));
        }
    }

    // =========================================================================
    // RF-11, RF-12, RF-13, RF-14, RF-15: Asistencia, Producción, Ingresos, Planilla y Egreso
    // =========================================================================

    @Test
    public void testRf11AsistenciaFaltaLimpiaHorasYSalidaMayorEntrada() throws Exception {
        final String[] guardado = new String[3];
        ClsOperacionDAO fakeDao = new ClsOperacionDAO() {
            @Override
            public void registrarAsistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado) {
                guardado[0] = entrada;
                guardado[1] = salida;
                guardado[2] = estado;
            }
        };
        ClsOperacionService svc = new ClsOperacionService(fakeDao);

        svc.asistencia(1, 1, LocalDate.now(), "08:00:00", "17:00:00", "FALTA", "RESIDENTE");
        Assert.assertEquals("", guardado[0]);
        Assert.assertEquals("", guardado[1]);
        Assert.assertEquals("FALTA", guardado[2]);

        try {
            svc.asistencia(1, 1, LocalDate.now(), "17:00:00", "08:00:00", "PRESENTE", "RESIDENTE");
            Assert.fail("Salida anterior a entrada debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("posterior"));
        }
    }

    @Test
    public void testRf12ProduccionSinAsistenciaYEnFaltaRechazada() throws Exception {
        ClsOperacionDAO fakeDao = new ClsOperacionDAO() {
            @Override
            public void registrarProduccion(int empleado, int partida, LocalDate fecha, double cantidad) {
                if (empleado == 3) {
                    throw new IllegalArgumentException("No se puede registrar producción porque el trabajador figura con estado 'FALTA' en esa fecha (RF-12).");
                }
                if (empleado == 4) {
                    throw new IllegalArgumentException("El trabajador no tiene asistencia registrada en la obra de esta partida para la fecha " + fecha + " (RF-12).");
                }
            }
        };
        ClsOperacionService svc = new ClsOperacionService(fakeDao);

        try {
            svc.produccion(3, 1, LocalDate.now(), 10.0, "RESIDENTE");
            Assert.fail("Trabajador con FALTA no puede registrar producción");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RF-12"));
        }

        try {
            svc.produccion(4, 1, LocalDate.now(), 10.0, "RESIDENTE");
            Assert.fail("Trabajador sin asistencia ese día en esa obra no puede registrar producción");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RF-12"));
        }
    }

    @Test
    public void testRf13IngresoFueraDelPeriodoDeObraRechazado() throws Exception {
        ClsOperacionDAO fakeDao = new ClsOperacionDAO() {
            @Override
            public void registrarIngreso(int obra, LocalDate fecha, String concepto, double monto) {
                if (fecha.isBefore(LocalDate.of(2026, 9, 1))) {
                    throw new IllegalArgumentException("La fecha del ingreso (" + fecha + ") debe estar dentro del periodo de ejecución de la obra (2026-09-01 al 2027-05-31) (RF-13).");
                }
            }
        };
        ClsOperacionService svc = new ClsOperacionService(fakeDao);
        try {
            svc.ingreso(1, LocalDate.of(2026, 8, 15), "Adelanto fuera de rango", 5000.0, "ADMIN");
            Assert.fail("Ingreso fuera del periodo de ejecución de la obra debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RF-13"));
        }
    }

    @Test
    public void testRf14PlanillaSoloAdminPeriodosSuperpuestosYPeriodoCerradoInmutable() throws Exception {
        ClsOperacionDAO fakeDao = new ClsOperacionDAO() {
            @Override
            public double cerrarPlanilla(int obra, LocalDate desde, LocalDate hasta, int usuario) {
                if (desde.equals(LocalDate.of(2026, 9, 15))) {
                    throw new IllegalArgumentException("El periodo seleccionado se superpone con un cierre de planilla ya registrado (2026-09-15 al 2026-09-16) (RF-14).");
                }
                return 840.0;
            }

            @Override
            public void registrarAsistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado) {
                if (fecha.equals(LocalDate.of(2026, 9, 15))) {
                    throw new IllegalArgumentException("No se puede registrar ni modificar asistencia en un periodo de planilla ya cerrado (RF-14).");
                }
            }
        };
        ClsOperacionService svc = new ClsOperacionService(fakeDao);

        try {
            svc.cerrarPlanilla(1, LocalDate.now().minusDays(7), LocalDate.now(), 1, "RESIDENTE");
            Assert.fail("Solo el Administrador puede cerrar planilla");
        } catch (SecurityException expected) {
            // OK
        }

        try {
            svc.cerrarPlanilla(1, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 20), 1, "ADMIN");
            Assert.fail("Cierre superpuesto debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("superpone"));
        }

        try {
            svc.asistencia(1, 1, LocalDate.of(2026, 9, 15), "08:00:00", "17:00:00", "PRESENTE", "RESIDENTE");
            Assert.fail("Asistencia en periodo cerrado debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("periodo de planilla ya cerrado"));
        }

        try {
            svc.egreso(1, LocalDate.now(), "   ", 100.0, "ADMIN");
            Assert.fail("Egreso sin concepto debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("concepto"));
        }
    }

    // =========================================================================
    // RF-16 y RNF-05: Exportación PDF/Excel (.xlsx) multipágina y Presentación sin SQL directo
    // =========================================================================

    @Test
    public void testRf16ExportacionPdfMultipaginaYExcelXlsx() throws Exception {
        ClsReporteService repSvc = new ClsReporteService();
        List<Object[]> muchasFilas = new ArrayList<>();
        for (int i = 1; i <= 75; i++) {
            muchasFilas.add(new Object[]{"Obra N° " + i + " <Especial & Única>", 40.0 + (i % 50), 45.5 + (i % 45)});
        }
        ClsReporteService.Reporte rep = new ClsReporteService.Reporte(
                "Reporte Multipágina Curva S",
                List.of("Obra", "Avance Planificado (%)", "Avance Ejecutado (%)"),
                muchasFilas
        );

        Path pdfPath = Files.createTempFile("sigoc_test_", ".pdf");
        Path xlsxPath = Files.createTempFile("sigoc_test_", ".xlsx");
        try {
            repSvc.exportarPdf(rep, pdfPath);
            repSvc.exportarExcelXlsx(rep, xlsxPath);

            Assert.assertTrue(Files.size(pdfPath) > 500);
            String pdfContent = Files.readString(pdfPath, java.nio.charset.StandardCharsets.ISO_8859_1);
            Assert.assertTrue(pdfContent.startsWith("%PDF-1.4"));
            Assert.assertTrue("75 filas con 34 por página deben generar 3 páginas", pdfContent.contains("/Count 3"));

            Assert.assertTrue(Files.size(xlsxPath) > 500);
            byte[] xlsxBytes = Files.readAllBytes(xlsxPath);
            Assert.assertEquals('P', (char) xlsxBytes[0]);
            Assert.assertEquals('K', (char) xlsxBytes[1]);
        } finally {
            Files.deleteIfExists(pdfPath);
            Files.deleteIfExists(xlsxPath);
        }
    }

    @Test
    public void testRnf05PresentacionSinAccesoDirectoABaseDeDatos() throws Exception {
        Path dirPresentacion = Path.of("src", "Presentacion");
        if (Files.isDirectory(dirPresentacion)) {
            try (var stream = Files.list(dirPresentacion)) {
                for (Path p : stream.filter(f -> f.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(p);
                    Assert.assertFalse("El archivo " + p.getFileName() + " no debe importar ClsConexionBD (RNF-05)",
                            content.contains("ClsConexionBD"));
                }
            }
        }
    }

    // =========================================================================
    // PRUEBA DE INTEGRACIÓN REAL CONTRA MYSQL 8 (RF-01 a RF-16)
    // Se ejecuta automáticamente cuando la base de datos está accesible.
    // =========================================================================

    @Test
    public void testIntegracionCompletaContraBaseDeDatosReal() throws Exception {
        try {
            ClsConexionBD.getInstancia().probarConexion();
        } catch (Exception ex) {
            // Si se corre en un entorno sin red/MySQL, no falla el test unitario offline
            return;
        }

        ClsCatalogoService catSvc = new ClsCatalogoService();
        ClsAvanceService avaSvc = new ClsAvanceService();
        ClsMaterialService matSvc = new ClsMaterialService();
        ClsOperacionService opSvc = new ClsOperacionService();
        ClsReporteService repSvc = new ClsReporteService();

        // 1. RF-03: Bloquear eliminación de la Obra 1 porque tiene partidas y movimientos reales
        try {
            catSvc.eliminar("obra", "id_obra", 1, "ADMIN");
            Assert.fail("BD Real: No debe permitir eliminar la Obra 1 con partidas/movimientos");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RF-03"));
        }

        // 2. RF-04: Suma de pesos de Obra 1 es 10 + 25 + 30 = 65%. Intentar agregar 40% (105%) debe fallar en BD real
        Map<String, Object> partidaExceso = new LinkedHashMap<>();
        partidaExceso.put("id_obra", 1);
        partidaExceso.put("nombre", "Partida Exceso BD");
        partidaExceso.put("unidad_medida", "m2");
        partidaExceso.put("metrado_planificado", 50.0);
        partidaExceso.put("peso_presupuestal", 40.0);
        try {
            catSvc.guardar("partida", "id_partida", null, partidaExceso, "RESIDENTE");
            Assert.fail("BD Real: Suma de pesos 105% debe ser rechazada");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("65.00 %"));
        }

        // 3. RF-06: Baja de Material 1 (Cemento con stock 300 > 0) debe rechazarse en BD real
        try {
            catSvc.eliminar("material", "id_material", 1, "ALMACENERO");
            Assert.fail("BD Real: Baja de material con stock 300 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("300.0"));
        }

        // 4. RF-12: Empleado 3 tuvo FALTA el 2026-09-16 en Obra 1; intentar registrar producción ese día debe fallar
        try {
            opSvc.produccion(3, 3, LocalDate.of(2026, 9, 16), 10.0, "RESIDENTE");
            Assert.fail("BD Real: Empleado con FALTA el 2026-09-16 no puede registrar producción");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("FALTA"));
        }

        // 5. RF-12: Empleado 1 no tiene asistencia el 2026-09-10; intentar registrar producción ese día debe fallar
        try {
            opSvc.produccion(1, 2, LocalDate.of(2026, 9, 10), 5.0, "RESIDENTE");
            Assert.fail("BD Real: Empleado sin asistencia el 2026-09-10 no puede registrar producción");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("no tiene asistencia"));
        }

        // 6. RF-13: Obra 1 inicia el 2026-09-01; ingreso el 2026-08-01 debe rechazarse en BD real
        try {
            opSvc.ingreso(1, LocalDate.of(2026, 8, 1), "Ingreso fuera de periodo", 1000.0, "ADMIN");
            Assert.fail("BD Real: Ingreso antes de fecha_inicio_plan debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("RF-13"));
        }

        // 7. RF-14: Probar cierre de planilla real (2026-09-15 al 2026-09-16 en Obra 1),
        // verificar 1 egreso por trabajador, rechazo de cierre superpuesto y rechazo de asistencia en periodo cerrado,
        // y luego restaurar el estado limpio de cierre_planilla.
        double totalCerrado = opSvc.cerrarPlanilla(1, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 16), 1, "ADMIN");
        // Asistencias válidas 15 y 16 sep en Obra 1:
        // Emp 1 (120 * 2 = 240) + Emp 2 (90 * 2 = 180) + Emp 3 (60 * 1 tardanza = 60) + Emp 4 (200 * 1 = 200) = 680.00
        Assert.assertEquals(680.0, totalCerrado, 0.01);

        try {
            opSvc.cerrarPlanilla(1, LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 20), 1, "ADMIN");
            Assert.fail("BD Real: Cierre superpuesto en 2026-09-16 debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("superpone"));
        }

        try {
            opSvc.asistencia(4, 1, LocalDate.of(2026, 9, 16), "08:00:00", "17:00:00", "PRESENTE", "RESIDENTE");
            Assert.fail("BD Real: No debe permitir registrar asistencia en un periodo ya cerrado");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("periodo de planilla ya cerrado"));
        }

        // Verificar que se crearon 4 egresos tipo Planilla (uno por trabajador con asistencia válida)
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM egreso WHERE tipo_egreso='Planilla' AND id_cierre IS NOT NULL")) {
            Assert.assertTrue(rs.next());
            Assert.assertEquals(4, rs.getInt(1));
        } finally {
            // Limpiar el cierre temporal de prueba para dejar los datos de prueba intactos
            try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
                 var st = c.createStatement()) {
                st.executeUpdate("DELETE FROM egreso WHERE tipo_egreso='Planilla' AND id_cierre IS NOT NULL");
                st.executeUpdate("DELETE FROM cierre_planilla");
            }
        }

        // 8. RF-16: Verificar los valores calculados de los 4 reportes contra los datos de prueba
        ClsReporteService.Reporte r1 = repSvc.generar(0, 1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "ADMIN");
        Assert.assertFalse("Reporte 1 (Curva S) debe retornar filas para Obra 1", r1.filas().isEmpty());

        ClsReporteService.Reporte r2 = repSvc.generar(1, null, null, null, "ADMIN");
        Assert.assertFalse("Reporte 2 (Consumo vs Presupuesto) no debe estar vacío", r2.filas().isEmpty());
        boolean halladoCementoConDesviacionPositiva = false;
        for (Object[] fila : r2.filas()) {
            if ("Cemento Portland Tipo I".equals(fila[0])) {
                double presup = ((Number) fila[2]).doubleValue();
                double real = ((Number) fila[3]).doubleValue();
                double desvAbs = ((Number) fila[4]).doubleValue();
                double desvPct = ((Number) fila[5]).doubleValue();
                Assert.assertEquals(180.0, presup, 0.01);
                Assert.assertEquals(200.0, real, 0.01);
                Assert.assertEquals(20.0, desvAbs, 0.01);
                Assert.assertEquals(11.11, desvPct, 0.05);
                halladoCementoConDesviacionPositiva = true;
            }
        }
        Assert.assertTrue("Cemento debe mostrar desviación +11.11% desde partida_material", halladoCementoConDesviacionPositiva);

        ClsReporteService.Reporte r3 = repSvc.generar(2, 1, null, null, "ADMIN");
        Assert.assertEquals("Reporte 3 (Personal) debe listar a los 4 empleados", 4, r3.filas().size());
        // El primero del ranking es Carlos Ramirez Soto (producción 33.50 en 2 asistencias -> 16.75 promedio)
        Assert.assertEquals("Carlos Ramirez Soto", r3.filas().get(0)[1]);
        Assert.assertEquals(16.75, ((Number) r3.filas().get(0)[6]).doubleValue(), 0.01);

        ClsReporteService.Reporte r4 = repSvc.generar(3, 1, null, null, "ADMIN");
        Assert.assertEquals(1, r4.filas().size());
        Object[] finObra1 = r4.filas().get(0);
        Assert.assertEquals(550000.0, ((Number) finObra1[1]).doubleValue(), 0.01); // Ingresos
        Assert.assertEquals(38300.0, ((Number) finObra1[2]).doubleValue(), 0.01);  // Egresos Material
        Assert.assertEquals(2500.0, ((Number) finObra1[4]).doubleValue(), 0.01);   // Otros Egresos
        Assert.assertEquals(40800.0, ((Number) finObra1[5]).doubleValue(), 0.01);  // Total Egresos
        Assert.assertEquals(509200.0, ((Number) finObra1[6]).doubleValue(), 0.01); // Utilidad
        Assert.assertEquals(92.58, ((Number) finObra1[7]).doubleValue(), 0.05);    // Rentabilidad %
    }
}
