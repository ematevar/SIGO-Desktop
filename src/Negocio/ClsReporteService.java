package Negocio;

import config.ClsConexionBD;
import config.ClsConstantes;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio gerencial de reportes con filtros por obra y rango de fechas, cálculo de Curva S,
 * desviaciones de consumo, porcentaje de asistencia/productividad, estado financiero con utilidad y
 * rentabilidad, y exportación a PDF, Excel (.xlsx) y CSV (RF-16).
 */
public class ClsReporteService {

    /**
     * Estructura inmutable que encapsula el resultado de un reporte gerencial.
     *
     * @param titulo Título del reporte.
     * @param columnas Encabezados de las columnas.
     * @param filas Filas de resultados calculados desde la base de datos.
     */
    public record Reporte(String titulo, List<String> columnas, List<Object[]> filas) {}

    /**
     * Genera el reporte indicado sin filtro específico de obra ni fechas (consulta general).
     *
     * @param tipo Índice del reporte (0: Avance Curva S, 1: Consumo vs Presupuesto, 2: Personal, 3: Financiero).
     * @return Instancia de {@link Reporte}.
     * @throws Exception Si ocurre un error SQL.
     */
    public Reporte generar(int tipo) throws Exception {
        return generar(tipo, null, null, null, null);
    }

    /**
     * Genera uno de los 4 reportes gerenciales del SRS filtrando opcionalmente por obra y rango de fechas (RF-16).
     *
     * @param tipo Índice del reporte (0 a 3).
     * @param idObra ID de la obra a filtrar ({@code null} o {@code <= 0} para todas las obras).
     * @param desde Fecha de inicio del filtro ({@code null} sin límite inferior).
     * @param hasta Fecha de fin del filtro ({@code null} sin límite superior).
     * @param rolSesion Rol del usuario en sesión (solo Administrador, RF-16).
     * @return Datos calculados del reporte.
     * @throws Exception Si el rango de fechas es inválido, no tiene permisos o falla la consulta.
     */
    public Reporte generar(int tipo, Integer idObra, LocalDate desde, LocalDate hasta, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final del filtro no puede ser anterior a la fecha inicial.");
        }
        int obraFiltro = (idObra == null || idObra <= 0) ? 0 : idObra;
        return switch (tipo) {
            case 0 -> generarReporteAvanceCurvaS(obraFiltro, desde, hasta);
            case 1 -> generarReporteConsumoMateriales(obraFiltro, desde, hasta);
            case 2 -> generarReportePersonal(obraFiltro, desde, hasta);
            case 3 -> generarReporteFinanciero(obraFiltro, desde, hasta);
            default -> throw new IllegalArgumentException("Reporte desconocido.");
        };
    }

    /**
     * Reporte (a): Avance físico planificado vs. ejecutado por obra (Curva S, RF-16a).
     * Calcula el avance planificado lineal según el calendario de la obra y el avance ejecutado
     * ponderado arrastrando el último porcentaje reportado de cada partida hasta cada fecha de corte.
     */
    private Reporte generarReporteAvanceCurvaS(int idObra, LocalDate desde, LocalDate hasta) throws Exception {
        String sql = "SELECT o.nombre AS obra, fechas.fecha_reporte, "
                + "ROUND(LEAST(100.0, GREATEST(0.0, (DATEDIFF(fechas.fecha_reporte, o.fecha_inicio_plan) * 100.0) / NULLIF(DATEDIFF(o.fecha_fin_plan, o.fecha_inicio_plan), 0))), 2) AS avance_planificado_pct, "
                + "ROUND(SUM(p.peso_presupuestal * COALESCE(("
                + "SELECT a.porcentaje_avance FROM avance_partida a "
                + "WHERE a.id_partida=p.id_partida AND a.activo=1 AND a.fecha_reporte<=fechas.fecha_reporte "
                + "ORDER BY a.fecha_reporte DESC, a.id_avance DESC LIMIT 1"
                + "), 0)) / NULLIF(SUM(p.peso_presupuestal), 0), 2) AS avance_ejecutado_pct "
                + "FROM obra o "
                + "JOIN partida p ON p.id_obra=o.id_obra AND p.activo=1 "
                + "JOIN ("
                + "  SELECT DISTINCT p2.id_obra, a.fecha_reporte "
                + "  FROM avance_partida a JOIN partida p2 ON p2.id_partida=a.id_partida "
                + "  WHERE a.activo=1 AND p2.activo=1 "
                + "  UNION "
                + "  SELECT id_obra, fecha_inicio_plan AS fecha_reporte FROM obra WHERE activo=1"
                + ") fechas ON fechas.id_obra=o.id_obra "
                + "WHERE o.activo=1 "
                + "AND (? = 0 OR o.id_obra = ?) "
                + "AND (? IS NULL OR fechas.fecha_reporte >= ?) "
                + "AND (? IS NULL OR fechas.fecha_reporte <= ?) "
                + "GROUP BY o.id_obra, o.nombre, o.fecha_inicio_plan, o.fecha_fin_plan, fechas.fecha_reporte "
                + "ORDER BY o.nombre, fechas.fecha_reporte";

        List<String> cols = List.of("Obra", "Fecha Corte", "Avance Planificado (%)", "Avance Ejecutado (%)", "Desviación (%)");
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idObra);
            ps.setInt(2, idObra);
            ps.setDate(3, desde == null ? null : java.sql.Date.valueOf(desde));
            ps.setDate(4, desde == null ? null : java.sql.Date.valueOf(desde));
            ps.setDate(5, hasta == null ? null : java.sql.Date.valueOf(hasta));
            ps.setDate(6, hasta == null ? null : java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double plan = rs.getDouble(3);
                    double ejec = rs.getDouble(4);
                    double desv = Math.round((ejec - plan) * 100.0) / 100.0;
                    rows.add(new Object[]{rs.getString(1), rs.getDate(2).toString(), plan, ejec, desv});
                }
            }
        }
        return new Reporte("Avance físico planificado vs. ejecutado (Curva S)", cols, rows);
    }

    /**
     * Reporte (b): Consumo de materiales real vs. presupuestado/adquirido con desviaciones (RF-16b).
     */
    private Reporte generarReporteConsumoMateriales(int idObra, LocalDate desde, LocalDate hasta) throws Exception {
        boolean tieneTablaPresupuesto = existeTablaPartidaMaterial();
        String subPresupuesto = tieneTablaPresupuesto
                ? "COALESCE((SELECT SUM(pm.cantidad_presupuestada) FROM partida_material pm JOIN partida p ON p.id_partida=pm.id_partida WHERE pm.id_material=m.id_material AND pm.activo=1 AND p.activo=1 AND (? = 0 OR p.id_obra = ?)), 0)"
                : "0";

        String sql = "SELECT m.nombre AS material, m.unidad_medida, "
                + "COALESCE(("
                + "  SELECT SUM(dc.cantidad) FROM detalle_compra dc "
                + "  JOIN compra_material cm ON cm.id_compra=dc.id_compra "
                + "  LEFT JOIN egreso eg ON eg.id_compra=cm.id_compra AND eg.activo=1 "
                + "  WHERE dc.id_material=m.id_material AND dc.activo=1 AND cm.activo=1 "
                + "  AND (? = 0 OR eg.id_obra = ?) "
                + "  AND (? IS NULL OR cm.fecha_compra >= ?) "
                + "  AND (? IS NULL OR cm.fecha_compra <= ?)"
                + "), 0) AS cant_comprada, "
                + subPresupuesto + " AS cant_tabla_presup, "
                + "COALESCE(("
                + "  SELECT SUM(c.cantidad) FROM consumo_material c "
                + "  JOIN partida p ON p.id_partida=c.id_partida "
                + "  WHERE c.id_material=m.id_material AND c.activo=1 AND p.activo=1 "
                + "  AND (? = 0 OR p.id_obra = ?) "
                + "  AND (? IS NULL OR c.fecha_consumo >= ?) "
                + "  AND (? IS NULL OR c.fecha_consumo <= ?)"
                + "), 0) AS consumo_real, "
                + "m.stock_actual, m.stock_minimo "
                + "FROM material m WHERE m.activo=1 ORDER BY m.nombre";

        List<String> cols = List.of(
                "Material", "Unidad", "Presupuestado", "Consumo Real",
                "Desviación Abs.", "Desviación (%)", "Stock Actual", "Stock Mínimo"
        );
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, idObra);
            ps.setInt(idx++, idObra);
            ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
            ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
            ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
            ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
            if (tieneTablaPresupuesto) {
                ps.setInt(idx++, idObra);
                ps.setInt(idx++, idObra);
            }
            ps.setInt(idx++, idObra);
            ps.setInt(idx++, idObra);
            ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
            ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
            ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
            ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mat = rs.getString("material");
                    String und = rs.getString("unidad_medida");
                    double comprada = rs.getDouble("cant_comprada");
                    double tablaPres = rs.getDouble("cant_tabla_presup");
                    double real = rs.getDouble("consumo_real");
                    double stock = rs.getDouble("stock_actual");
                    double minimo = rs.getDouble("stock_minimo");

                    double presupuestado = tablaPres > 0 ? tablaPres : (comprada > 0 ? comprada : (real + stock));
                    double desvAbs = Math.round((real - presupuestado) * 100.0) / 100.0;
                    double desvPct = presupuestado > 0 ? Math.round(((real - presupuestado) * 10000.0) / presupuestado) / 100.0 : 0.0;

                    if (idObra == 0 || presupuestado > 0 || real > 0) {
                        rows.add(new Object[]{mat, und, presupuestado, real, desvAbs, desvPct, stock, minimo});
                    }
                }
            }
        }
        return new Reporte("Consumo de materiales real vs. presupuestado", cols, rows);
    }

    /**
     * Reporte (c): Desempeño del personal con % de asistencia/cumplimiento, producción total y
     * productividad promedio por jornada (ranking ordenado por productividad, RF-16c).
     */
    private Reporte generarReportePersonal(int idObra, LocalDate desde, LocalDate hasta) throws Exception {
        String sql = "SELECT e.dni, e.nombres_apellidos, e.cargo, "
                + "(SELECT COUNT(*) FROM asistencia a WHERE a.id_empleado=e.id_empleado AND a.activo=1 "
                + " AND (? = 0 OR a.id_obra = ?) AND (? IS NULL OR a.fecha >= ?) AND (? IS NULL OR a.fecha <= ?)) AS total_registros, "
                + "(SELECT COUNT(*) FROM asistencia a WHERE a.id_empleado=e.id_empleado AND a.activo=1 "
                + " AND UPPER(a.estado) IN ('PRESENTE','TARDANZA','ASISTIO','ASISTIÓ') "
                + " AND (? = 0 OR a.id_obra = ?) AND (? IS NULL OR a.fecha >= ?) AND (? IS NULL OR a.fecha <= ?)) AS asistencias_validas, "
                + "(SELECT COALESCE(SUM(pr.cantidad_producida), 0) FROM produccion_empleado pr "
                + " JOIN partida p ON p.id_partida=pr.id_partida "
                + " WHERE pr.id_empleado=e.id_empleado AND pr.activo=1 AND p.activo=1 "
                + " AND (? = 0 OR p.id_obra = ?) AND (? IS NULL OR pr.fecha >= ?) AND (? IS NULL OR pr.fecha <= ?)) AS produccion_total "
                + "FROM empleado e WHERE e.activo=1";

        long diasRango = (desde != null && hasta != null) ? Math.max(1L, ChronoUnit.DAYS.between(desde, hasta) + 1) : 0L;

        List<String> cols = List.of(
                "DNI", "Trabajador", "Cargo", "Asistencias Válidas",
                "Cumplimiento Asistencia (%)", "Producción Total", "Productividad Promedio"
        );
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (int bloque = 0; bloque < 3; bloque++) {
                ps.setInt(idx++, idObra);
                ps.setInt(idx++, idObra);
                ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
                ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
                ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
                ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int totalReg = rs.getInt("total_registros");
                    int validas = rs.getInt("asistencias_validas");
                    double prodTotal = rs.getDouble("produccion_total");
                    long baseDias = totalReg > 0 ? totalReg : (diasRango > 0 ? diasRango : 1L);
                    double pctAsist = totalReg == 0 && validas == 0 ? 0.0
                            : Math.min(100.0, Math.round((validas * 10000.0) / baseDias) / 100.0);
                    double prodProm = validas > 0 ? Math.round((prodTotal / validas) * 100.0) / 100.0 : 0.0;
                    rows.add(new Object[]{
                        rs.getString("dni"),
                        rs.getString("nombres_apellidos"),
                        rs.getString("cargo"),
                        validas,
                        pctAsist,
                        prodTotal,
                        prodProm
                    });
                }
            }
        }
        rows.sort((a, b) -> Double.compare(((Number) b[6]).doubleValue(), ((Number) a[6]).doubleValue()));
        return new Reporte("Asistencia, cumplimiento y ranking de productividad del personal", cols, rows);
    }

    /**
     * Reporte (d): Estado financiero por obra desglosando ingresos, egresos por tipo (Material, Planilla, Otro),
     * utilidad neta y porcentaje de rentabilidad (RF-16d).
     */
    private Reporte generarReporteFinanciero(int idObra, LocalDate desde, LocalDate hasta) throws Exception {
        String sql = "SELECT o.nombre AS obra, "
                + "COALESCE((SELECT SUM(i.monto) FROM ingreso i WHERE i.id_obra=o.id_obra AND i.activo=1 "
                + " AND (? IS NULL OR i.fecha >= ?) AND (? IS NULL OR i.fecha <= ?)), 0) AS ingresos, "
                + "COALESCE((SELECT SUM(e.monto) FROM egreso e WHERE e.id_obra=o.id_obra AND e.activo=1 "
                + " AND UPPER(e.tipo_egreso) IN ('MATERIAL','COMPRA') "
                + " AND (? IS NULL OR e.fecha >= ?) AND (? IS NULL OR e.fecha <= ?)), 0) AS egr_material, "
                + "COALESCE((SELECT SUM(e.monto) FROM egreso e WHERE e.id_obra=o.id_obra AND e.activo=1 "
                + " AND UPPER(e.tipo_egreso) IN ('PLANILLA') "
                + " AND (? IS NULL OR e.fecha >= ?) AND (? IS NULL OR e.fecha <= ?)), 0) AS egr_planilla, "
                + "COALESCE((SELECT SUM(e.monto) FROM egreso e WHERE e.id_obra=o.id_obra AND e.activo=1 "
                + " AND UPPER(e.tipo_egreso) NOT IN ('MATERIAL','COMPRA','PLANILLA') "
                + " AND (? IS NULL OR e.fecha >= ?) AND (? IS NULL OR e.fecha <= ?)), 0) AS egr_otros "
                + "FROM obra o WHERE o.activo=1 AND (? = 0 OR o.id_obra = ?) ORDER BY o.nombre";

        List<String> cols = List.of(
                "Obra", "Ingresos (S/)", "Egresos Material (S/)", "Egresos Planilla (S/)",
                "Otros Egresos (S/)", "Total Egresos (S/)", "Utilidad / Saldo (S/)", "Rentabilidad (%)"
        );
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (int b = 0; b < 4; b++) {
                ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
                ps.setDate(idx++, desde == null ? null : java.sql.Date.valueOf(desde));
                ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
                ps.setDate(idx++, hasta == null ? null : java.sql.Date.valueOf(hasta));
            }
            ps.setInt(idx++, idObra);
            ps.setInt(idx++, idObra);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double ing = rs.getDouble("ingresos");
                    double mat = rs.getDouble("egr_material");
                    double pla = rs.getDouble("egr_planilla");
                    double otr = rs.getDouble("egr_otros");
                    double totEgr = Math.round((mat + pla + otr) * 100.0) / 100.0;
                    double utilidad = Math.round((ing - totEgr) * 100.0) / 100.0;
                    double rentPct = ing > 0 ? Math.round((utilidad * 10000.0) / ing) / 100.0 : 0.0;
                    rows.add(new Object[]{rs.getString("obra"), ing, mat, pla, otr, totEgr, utilidad, rentPct});
                }
            }
        }
        return new Reporte("Estado financiero, utilidad y rentabilidad por obra", cols, rows);
    }

    private volatile Boolean cacheTablaPartidaMaterial;

    private boolean existeTablaPartidaMaterial() {
        Boolean cached = cacheTablaPartidaMaterial;
        if (cached != null) {
            return cached;
        }
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, "partida_material", null)) {
            boolean exists = rs.next();
            cacheTablaPartidaMaterial = exists;
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Exporta un reporte a formato CSV con BOM UTF-8 compatible con Microsoft Excel.
     *
     * @param reporte Reporte a exportar.
     * @param path Ruta del archivo destino.
     * @throws Exception Si ocurre un error de escritura.
     */
    public void exportarCsv(Reporte reporte, Path path) throws Exception {
        try (var w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("\uFEFF");
            w.write(reporte.columnas().stream().map(this::csv).collect(java.util.stream.Collectors.joining(",")));
            w.newLine();
            for (Object[] row : reporte.filas()) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) {
                        w.write(',');
                    }
                    w.write(csv(row[i] == null ? "" : row[i].toString()));
                }
                w.newLine();
            }
        }
    }

    /**
     * Exporta un reporte a un libro nativo de Microsoft Excel OpenXML ({@code .xlsx}) (RF-16).
     *
     * @param reporte Reporte a exportar.
     * @param path Ruta del archivo {@code .xlsx} destino.
     * @throws Exception Si ocurre un error de entrada/salida.
     */
    public void exportarExcelXlsx(Reporte reporte, Path path) throws Exception {
        try (OutputStream fos = Files.newOutputStream(path);
             ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {

            escribirEntradaZip(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "</Types>");

            escribirEntradaZip(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");

            escribirEntradaZip(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    + "<sheets><sheet name=\"Reporte SIGOC\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                    + "</workbook>");

            escribirEntradaZip(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "</Relationships>");

            StringBuilder sheet = new StringBuilder();
            sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                 .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");

            // Fila 1: Título
            sheet.append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>")
                 .append(escaparXml(reporte.titulo()))
                 .append("</t></is></c></row>");

            // Fila 2: Encabezados
            sheet.append("<row r=\"2\">");
            List<String> cols = reporte.columnas();
            for (int c = 0; c < cols.size(); c++) {
                String ref = columnaExcel(c) + "2";
                sheet.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                     .append(escaparXml(cols.get(c)))
                     .append("</t></is></c>");
            }
            sheet.append("</row>");

            // Filas de datos
            int rowNum = 3;
            for (Object[] fila : reporte.filas()) {
                sheet.append("<row r=\"").append(rowNum).append("\">");
                for (int c = 0; c < fila.length; c++) {
                    String ref = columnaExcel(c) + rowNum;
                    Object val = fila[c];
                    if (val instanceof Number num) {
                        sheet.append("<c r=\"").append(ref).append("\" t=\"n\"><v>")
                             .append(num)
                             .append("</v></c>");
                    } else {
                        String txt = val == null ? "" : val.toString();
                        sheet.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                             .append(escaparXml(txt))
                             .append("</t></is></c>");
                    }
                }
                sheet.append("</row>");
                rowNum++;
            }
            sheet.append("</sheetData></worksheet>");
            escribirEntradaZip(zos, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    /**
     * Exporta un reporte a un documento PDF estándar 1.4 válido y legible por cualquier visor PDF (RF-16).
     *
     * @param reporte Reporte a exportar.
     * @param path Ruta del archivo {@code .pdf} destino.
     * @throws Exception Si ocurre un error de escritura.
     */
    public void exportarPdf(Reporte reporte, Path path) throws Exception {
        List<Object[]> filas = reporte.filas();
        int filasPorPagina = 34;
        int totalPaginas = Math.max(1, (filas.size() + filasPorPagina - 1) / filasPorPagina);

        List<byte[]> streamsPaginas = new ArrayList<>();
        for (int p = 0; p < totalPaginas; p++) {
            StringBuilder stream = new StringBuilder();
            stream.append("BT\n/F1 13 Tf\n36 555 Td\n(")
                  .append(escaparPdf("SIGOC Desktop - " + reporte.titulo() + " (Pag. " + (p + 1) + "/" + totalPaginas + ")"))
                  .append(") Tj\nET\n");

            stream.append("BT\n/F1 8 Tf\n36 535 Td\n(")
                  .append(escaparPdf(formatearFilaPdf(reporte.columnas().toArray())))
                  .append(") Tj\nET\n");

            int y = 518;
            int inicio = p * filasPorPagina;
            int fin = Math.min(filas.size(), inicio + filasPorPagina);
            for (int r = inicio; r < fin; r++) {
                stream.append("BT\n/F1 8 Tf\n36 ").append(y).append(" Td\n(")
                      .append(escaparPdf(formatearFilaPdf(filas.get(r))))
                      .append(") Tj\nET\n");
                y -= 14;
            }
            streamsPaginas.add(stream.toString().getBytes(StandardCharsets.ISO_8859_1));
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        escribirAscii(pdf, "%PDF-1.4\n");

        // Obj 1: Catalog, Obj 2: Pages, Obj 3: Font
        offsets.add(pdf.size());
        escribirAscii(pdf, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder("[");
        for (int p = 0; p < totalPaginas; p++) {
            if (p > 0) {
                kids.append(" ");
            }
            kids.append(4 + (p * 2)).append(" 0 R");
        }
        kids.append("]");

        offsets.add(pdf.size());
        escribirAscii(pdf, "2 0 obj\n<< /Type /Pages /Kids " + kids + " /Count " + totalPaginas + " >>\nendobj\n");

        offsets.add(pdf.size());
        escribirAscii(pdf, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int p = 0; p < totalPaginas; p++) {
            int pageObjId = 4 + (p * 2);
            int contentObjId = pageObjId + 1;
            byte[] contentBytes = streamsPaginas.get(p);

            offsets.add(pdf.size());
            escribirAscii(pdf, pageObjId + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObjId + " 0 R >>\nendobj\n");

            offsets.add(pdf.size());
            escribirAscii(pdf, contentObjId + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            pdf.write(contentBytes);
            escribirAscii(pdf, "\nendstream\nendobj\n");
        }

        int totalObjs = offsets.size() + 1;
        int xrefOffset = pdf.size();
        escribirAscii(pdf, "xref\n0 " + totalObjs + "\n0000000000 65535 f \n");
        for (int off : offsets) {
            escribirAscii(pdf, String.format("%010d 00000 n \n", off));
        }
        escribirAscii(pdf, "trailer\n<< /Size " + totalObjs + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");

        Files.write(path, pdf.toByteArray());
    }

    private String formatearFilaPdf(Object[] celdas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            String s = celdas[i] == null ? "" : celdas[i].toString();
            if (s.length() > 18) {
                s = s.substring(0, 18);
            }
            sb.append(String.format("%-18s", s));
        }
        return sb.toString();
    }

    private String escaparPdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private void escribirAscii(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }

    private void escribirEntradaZip(ZipOutputStream zos, String nombre, String contenido) throws Exception {
        zos.putNextEntry(new ZipEntry(nombre));
        zos.write(contenido.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String columnaExcel(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = (n / 26) - 1;
        }
        return sb.toString();
    }

    private String escaparXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
