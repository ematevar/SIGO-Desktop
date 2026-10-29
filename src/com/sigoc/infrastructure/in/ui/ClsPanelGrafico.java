package com.sigoc.infrastructure.in.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Panel gráfico reutilizable para dibujar gráficos de barras y líneas usando
 * Java2D.
 */
public class ClsPanelGrafico extends JPanel {

    public enum TipoGrafico {
        BARRAS, LINEAS
    }

    private String titulo;
    private TipoGrafico tipo;
    private String[] etiquetas;
    private List<SerieGrafico> series;

    private static final Color[] COLORES = {
        new Color(68, 114, 196), // Azul
        new Color(237, 125, 49), // Naranja
        new Color(112, 173, 71), // Verde
        new Color(255, 107, 107), // Rojo
        new Color(155, 89, 182) // Púrpura
    };

    private static final int PAD_TOP = 60;
    private static final int PAD_BOTTOM = 80;
    private static final int PAD_LEFT = 70;
    private static final int PAD_RIGHT = 30;

    private Font fuenteTitulo = new Font("SansSerif", Font.BOLD, 16);
    private Font fuenteEjes = new Font("SansSerif", Font.PLAIN, 11);
    private Font fuenteValores = new Font("SansSerif", Font.PLAIN, 10);

    private class SerieGrafico {

        String nombre;
        double[] valores;
        Color color;

        SerieGrafico(String nombre, double[] valores, Color color) {
            this.nombre = nombre;
            this.valores = valores;
            this.color = color;
        }
    }

    public ClsPanelGrafico(String titulo, TipoGrafico tipo) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.series = new ArrayList<>();
        this.setPreferredSize(new Dimension(800, 400));
        this.setBackground(Color.WHITE);
    }

    public ClsPanelGrafico(TipoGrafico tipo) {
        this("", tipo);
    }

    public void setEtiquetas(String[] etiquetas) {
        this.etiquetas = etiquetas;
        repaint();
    }

    public void agregarSerie(String nombre, double[] valores) {
        Color color = COLORES[series.size() % COLORES.length];
        series.add(new SerieGrafico(nombre, valores, color));
        repaint();
    }

    public void limpiar() {
        series.clear();
        etiquetas = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ancho = getWidth();
        int alto = getHeight();

        // Título
        g2.setFont(fuenteTitulo);
        FontMetrics fmTitulo = g2.getFontMetrics();
        int tituloAncho = fmTitulo.stringWidth(titulo);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(titulo, (ancho - tituloAncho) / 2, 25);

        // Leyenda
        dibujarLeyenda(g2, ancho);

        if (series.isEmpty() || etiquetas == null || etiquetas.length == 0) {
            g2.setFont(fuenteEjes);
            g2.drawString("No hay datos disponibles.", ancho / 2 - 50, alto / 2);
            return;
        }

        // Calcular min y max
        double max = 0;
        double min = 0;
        for (SerieGrafico serie : series) {
            for (double v : serie.valores) {
                if (v > max) {
                    max = v;
                }
                if (v < min) {
                    min = v;
                }
            }
        }
        if (max == min) {
            max += 10;
            min -= 10;
        }

        // Ajuste en caso de puros ceros
        if (max == 0 && min == 0) {
            max = 10;
            min = -10;
        }

        // Ajustar rango
        double rango = max - min;
        double factorY = (double) (alto - PAD_TOP - PAD_BOTTOM) / rango;

        int ceroY = alto - PAD_BOTTOM - (int) ((0 - min) * factorY);

        // Ejes y rejilla
        g2.setStroke(new BasicStroke(1.0f));
        int numLineas = 6;
        for (int i = 0; i <= numLineas; i++) {
            double v = min + (rango * i) / numLineas;
            int y = alto - PAD_BOTTOM - (int) ((v - min) * factorY);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(PAD_LEFT, y, ancho - PAD_RIGHT, y);

            // Etiqueta Y
            g2.setFont(fuenteValores);
            g2.setColor(Color.GRAY);
            String etY = String.format("%.1f", v);
            FontMetrics fmVal = g2.getFontMetrics();
            g2.drawString(etY, PAD_LEFT - fmVal.stringWidth(etY) - 5, y + 4);
        }

        // Ejes principales
        g2.setColor(Color.BLACK);
        g2.drawLine(PAD_LEFT, alto - PAD_BOTTOM, ancho - PAD_RIGHT, alto - PAD_BOTTOM);
        g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, alto - PAD_BOTTOM);

        if (tipo == TipoGrafico.BARRAS) {
            dibujarBarras(g2, ancho, alto, factorY, min, ceroY);
        } else {
            dibujarLineas(g2, ancho, alto, factorY, min);
        }
    }

    private void dibujarLeyenda(Graphics2D g2, int ancho) {
        if (series.isEmpty()) {
            return;
        }
        g2.setFont(fuenteValores);
        int totalLeyendaAncho = 0;
        for (SerieGrafico s : series) {
            totalLeyendaAncho += 15 + g2.getFontMetrics().stringWidth(s.nombre) + 15;
        }
        int startX = (ancho - totalLeyendaAncho) / 2;
        int y = 50;

        for (SerieGrafico s : series) {
            g2.setColor(s.color);
            g2.fillRect(startX, y - 10, 10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(s.nombre, startX + 15, y);
            startX += 15 + g2.getFontMetrics().stringWidth(s.nombre) + 15;
        }
    }

    private void dibujarBarras(Graphics2D g2, int ancho, int alto, double factorY, double min, int ceroY) {
        int numGrupos = etiquetas.length;
        int anchoEspacio = ancho - PAD_LEFT - PAD_RIGHT;
        int anchoGrupo = anchoEspacio / numGrupos;
        int numSeries = series.size();

        int gapGrupo = 4;
        int anchoTotalBarras = anchoGrupo - gapGrupo;
        int anchoBarra = anchoTotalBarras / numSeries;
        int gapBarra = 1; // espacio dentro del grupo
        anchoBarra -= gapBarra;

        for (int i = 0; i < numGrupos; i++) {
            int grupoX = PAD_LEFT + i * anchoGrupo + gapGrupo / 2;

            for (int j = 0; j < numSeries; j++) {
                SerieGrafico serie = series.get(j);
                if (i < serie.valores.length) {
                    double v = serie.valores[i];
                    int barHeight = (int) (Math.abs(v) * factorY);
                    int x = grupoX + j * (anchoBarra + gapBarra);
                    int y = v >= 0 ? ceroY - barHeight : ceroY;

                    g2.setColor(serie.color);
                    g2.fill(new Rectangle2D.Double(x, y, anchoBarra, barHeight));
                }
            }
            dibujarEtiquetaX(g2, etiquetas[i], grupoX + anchoTotalBarras / 2, alto - PAD_BOTTOM + 15);
        }
    }

    private void dibujarLineas(Graphics2D g2, int ancho, int alto, double factorY, double min) {
        int numPuntos = etiquetas.length;
        int anchoEspacio = ancho - PAD_LEFT - PAD_RIGHT;
        int pasoX = numPuntos > 1 ? anchoEspacio / (numPuntos - 1) : anchoEspacio / 2;

        for (int j = 0; j < series.size(); j++) {
            SerieGrafico serie = series.get(j);
            g2.setColor(serie.color);
            g2.setStroke(new BasicStroke(2.5f));

            int antX = -1, antY = -1;
            for (int i = 0; i < numPuntos; i++) {
                if (i < serie.valores.length) {
                    double v = serie.valores[i];
                    int x = PAD_LEFT + i * pasoX;
                    int y = alto - PAD_BOTTOM - (int) ((v - min) * factorY);

                    if (antX != -1) {
                        g2.draw(new Line2D.Double(antX, antY, x, y));
                    }
                    g2.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));

                    antX = x;
                    antY = y;
                }
                if (j == 0) { // Dibujar etiquetas solo una vez por índice
                    dibujarEtiquetaX(g2, etiquetas[i], PAD_LEFT + i * pasoX, alto - PAD_BOTTOM + 15);
                }
            }
        }
    }

    private void dibujarEtiquetaX(Graphics2D g2, String etiqueta, int x, int y) {
        g2.setFont(fuenteEjes);
        g2.setColor(Color.DARK_GRAY);
        AffineTransform original = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(-Math.PI / 4);
        g2.drawString(etiqueta, 0, 0);
        g2.setTransform(original);
    }
}
