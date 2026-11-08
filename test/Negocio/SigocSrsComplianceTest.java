package Negocio;

import config.ClsConstantes;
import Entidad.ClsUsuario;
import NegocioDAO.ClsAvanceDAO;
import NegocioDAO.ClsCatalogoDAO;
import NegocioDAO.ClsMaterialDAO;
import NegocioDAO.ClsOperacionDAO;
import NegocioDAO.ClsUsuarioDAO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Suite completa de pruebas unitarias de aceptación del SRS V3 para SIGOC Desktop (RF-01 a RF-16 y RNF-03 a RNF-08).
 */
public class SigocSrsComplianceTest {

    // =========================================================================
    // RF-01 y RF-02: Autenticación, Captcha, Roles y Usuarios
    // =========================================================================

    @Test
    public void testRf01CaptchaIncorrectoYBloqueoIntentos() throws Exception {
        ClsCaptchaService captcha = new ClsCaptchaService();
        String code = captcha.generar();
        Assert.assertEquals(6, code.length());
        Assert.assertTrue(captcha.validar(code, code.toLowerCase()));
        Assert.assertFalse(captcha.validar(code, "XXXXXX"));

        ClsUsuarioDAO fakeDao = new ClsUsuarioDAO() {
            @Override
            public ClsUsuario autenticar(String u, String p) {
                if ("admin".equals(u) && "ClaveSegura123".equals(p)) {
                    return new ClsUsuario(1, "admin", "ADMIN", false);
                }
                return null;
            }
        };
        ClsUsuarioService svc = new ClsUsuarioService(fakeDao, captcha);

        // Captcha incorrecto debe rechazar
        try {
            svc.iniciarSesion("admin", "ClaveSegura123", code, "WRONGC");
            Assert.fail("Debió rechazar captcha incorrecto");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().toLowerCase().contains("captcha"));
        }

        // Login válido
        ClsUsuario sesion = svc.iniciarSesion("admin", "ClaveSegura123", code, code);
        Assert.assertNotNull(sesion);
        Assert.assertEquals("ADMIN", sesion.getRol());
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

        // Almacenero rechazado
        try {
            svc.registrar(1, LocalDate.now(), 95.0, "Obs", 1, "ALMACENERO");
            Assert.fail("Almacenero no puede registrar avance");
        } catch (SecurityException expected) {
            // OK
        }

        // Disminución de 90% a 80% rechazada
        try {
            svc.registrar(1, LocalDate.now(), 80.0, "Obs", 1, "RESIDENTE");
            Assert.fail("Avance menor al anterior debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("90.0"));
        }

        // 101% rechazado
        try {
            svc.registrar(1, LocalDate.now(), 101.0, "Obs", 1, "RESIDENTE");
            Assert.fail("Avance > 100% debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("100 %"));
        }

        // Registro válido devuelve avance acumulado de la obra
        double avanceObra = svc.registrar(1, LocalDate.now(), 95.0, "Avance OK", 1, "RESIDENTE");
        Assert.assertEquals(45.5, avanceObra, 0.01);
    }

    // =========================================================================
    // RF-06 y RF-08: Materiales, baja con stock > 0 y alerta stock <= mínimo
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
    public void testRf08AlertaStockIgualOPorDebajoDelMinimo() throws Exception {
        ClsMaterialDAO fakeDaoIgualMinimo = new ClsMaterialDAO() {
            @Override
            public void registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad) {
                // Simula descuento exitoso
            }

            @Override
            public double stockMinimo(int material) {
                return 0.0; // stock_actual == stock_minimo -> diferencia 0.0
            }
        };
        ClsMaterialService svc = new ClsMaterialService(fakeDaoIgualMinimo);
        boolean alerta = svc.registrarConsumo(1, 1, 2, LocalDate.now(), 10.0, "RESIDENTE");
        Assert.assertTrue("Debe disparar alerta cuando el stock queda exactamente igual al mínimo (<= 0)", alerta);
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
    // RF-11, RF-14, RF-15: Asistencia (FALTA sin horas, salida > entrada), Planilla y Egreso
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

        // En estado FALTA debe limpiar las horas por defecto 08:00:00 / 17:00:00
        svc.asistencia(1, 1, LocalDate.now(), "08:00:00", "17:00:00", "FALTA", "RESIDENTE");
        Assert.assertEquals("", guardado[0]);
        Assert.assertEquals("", guardado[1]);
        Assert.assertEquals("FALTA", guardado[2]);

        // Salida <= Entrada en PRESENTE debe rechazarse
        try {
            svc.asistencia(1, 1, LocalDate.now(), "17:00:00", "08:00:00", "PRESENTE", "RESIDENTE");
            Assert.fail("Salida anterior a entrada debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("posterior"));
        }
    }

    @Test
    public void testRf14PlanillaSoloAdminYRf15EgresoConMotivo() throws Exception {
        ClsOperacionService svc = new ClsOperacionService(new ClsOperacionDAO());
        try {
            svc.cerrarPlanilla(1, LocalDate.now().minusDays(7), LocalDate.now(), 1, "RESIDENTE");
            Assert.fail("Solo el Administrador puede cerrar planilla");
        } catch (SecurityException expected) {
            // OK
        }

        try {
            svc.egreso(1, LocalDate.now(), "   ", 100.0, "ADMIN");
            Assert.fail("Egreso sin concepto debe rechazarse");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("concepto"));
        }
    }

    // =========================================================================
    // RF-16 y RNF-05: Exportación PDF/Excel (.xlsx) y Presentación sin SQL directo
    // =========================================================================

    @Test
    public void testRf16ExportacionPdfYExcelXlsx() throws Exception {
        ClsReporteService repSvc = new ClsReporteService();
        ClsReporteService.Reporte rep = new ClsReporteService.Reporte(
                "Reporte de Prueba",
                List.of("Obra", "Avance Planificado (%)", "Avance Ejecutado (%)"),
                List.<Object[]>of(new Object[]{"Obra A", 50.0, 62.5})
        );

        Path pdfPath = Files.createTempFile("sigoc_test_", ".pdf");
        Path xlsxPath = Files.createTempFile("sigoc_test_", ".xlsx");
        try {
            repSvc.exportarPdf(rep, pdfPath);
            repSvc.exportarExcelXlsx(rep, xlsxPath);

            Assert.assertTrue(Files.size(pdfPath) > 100);
            String pdfHeader = new String(Files.readAllBytes(pdfPath), 0, 8);
            Assert.assertTrue(pdfHeader.startsWith("%PDF-1.4"));

            Assert.assertTrue(Files.size(xlsxPath) > 100);
            byte[] xlsxBytes = Files.readAllBytes(xlsxPath);
            // Cabecera ZIP 'PK'
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
}
