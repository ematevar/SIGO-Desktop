package com.sigoc.application.service;

import java.util.List;
import com.sigoc.application.port.in.ReporteUseCase;
import com.sigoc.application.port.out.ReporteRepositoryPort;

/** Consultas gerenciales y exportación CSV compatible con Excel. */
public class ClsReporteService implements ReporteUseCase {
    private final ReporteRepositoryPort dao;

    public ClsReporteService(ReporteRepositoryPort dao) {
        this.dao = dao;
    }

    public record Reporte(String titulo,List<String> columnas,List<Object[]> filas){}

    public Reporte generar(int tipo)throws Exception{
        return dao.generar(tipo);
    }
    public void exportarCsv(Reporte reporte,java.nio.file.Path path)throws Exception{
        try(var w=java.nio.file.Files.newBufferedWriter(path,java.nio.charset.StandardCharsets.UTF_8)){
            w.write("\uFEFF");w.write(reporte.columnas().stream().map(this::csv).collect(java.util.stream.Collectors.joining(",")));w.newLine();
            for(Object[] row:reporte.filas()){for(int i=0;i<row.length;i++){if(i>0)w.write(',');w.write(csv(row[i]==null?"":row[i].toString()));}w.newLine();}
        }
    }
    private String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
}
