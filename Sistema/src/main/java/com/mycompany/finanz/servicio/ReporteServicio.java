package com.mycompany.finanz.servicio;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.ReporteRecomendacionDAO;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.modelo.Recomendacion;
import com.mycompany.finanz.modelo.ReporteFinanciero;
import com.mycompany.finanz.modelo.ResumenPeriodo;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.util.Formato;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Genera el resumen financiero mensual del usuario en formato PDF
 * y registra la generación en la tabla reporte_financiero.
 */
public class ReporteServicio {

    private static final Color AZUL = new Color(18, 63, 120);
    private static final Color VERDE = new Color(23, 107, 74);
    private static final Color CORAL = new Color(228, 102, 77);
    private static final Color GRIS = new Color(102, 115, 108);
    private static final Color FONDO = new Color(241, 245, 250);

    private static final Font TITULO = new Font(Font.HELVETICA, 20, Font.BOLD, AZUL);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 13, Font.BOLD, AZUL);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, GRIS);
    private static final Font CABECERA = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

    private final EstadisticaServicio estadisticaServicio = new EstadisticaServicio();
    private final PresupuestoServicio presupuestoServicio = new PresupuestoServicio();
    private final MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio();
    private final RecomendacionServicio recomendacionServicio = new RecomendacionServicio();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final ReporteRecomendacionDAO reporteDAO = new ReporteRecomendacionDAO();

    public byte[] generarPdf(Usuario usuario, YearMonth mes) throws SQLException {
        int idUsuario = usuario.getIdUsuario();
        ResumenPeriodo resumen = estadisticaServicio.resumenMes(idUsuario, mes);
        ResumenPeriodo anterior = estadisticaServicio.resumenMes(idUsuario, mes.minusMonths(1));
        List<Movimiento> movimientos = movimientoDAO.listarPorUsuarioYPeriodo(idUsuario, mes.atDay(1), mes.atEndOfMonth());
        Presupuesto presupuesto = presupuestoServicio.buscar(idUsuario, mes);
        List<MetaAhorro> metas = metaAhorroServicio.listar(idUsuario);
        List<Recomendacion> recomendaciones = recomendacionServicio.generar(idUsuario);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, salida);
            doc.addTitle("Resumen financiero " + Formato.mes(mes));
            doc.addAuthor("FinanZ");
            doc.open();

            doc.add(new Paragraph("FinanZ", new Font(Font.HELVETICA, 11, Font.BOLD, VERDE)));
            doc.add(new Paragraph("Resumen financiero de " + Formato.mes(mes), TITULO));
            doc.add(new Paragraph(usuario.getNombreCompleto() + "  ·  " + usuario.getEmail()
                    + "  ·  Generado el " + Formato.fechaHora(LocalDateTime.now()), PEQUENA));
            doc.add(Chunk.NEWLINE);

            // Indicadores principales
            PdfPTable kpis = new PdfPTable(4);
            kpis.setWidthPercentage(100);
            kpi(kpis, "Ingresos", Formato.soles(resumen.getIngresos()), VERDE);
            kpi(kpis, "Gastos", Formato.soles(resumen.getGastos()), CORAL);
            kpi(kpis, "Balance", Formato.soles(resumen.getBalance()), resumen.getBalance().signum() < 0 ? CORAL : AZUL);
            kpi(kpis, "Ahorro en metas", Formato.soles(resumen.getAhorroMetas()), AZUL);
            doc.add(kpis);

            // Presupuesto
            seccion(doc, "Presupuesto");
            if (presupuesto == null) {
                doc.add(new Paragraph("No se definió presupuesto para este mes.", NORMAL));
            } else {
                BigDecimal gastado = presupuestoServicio.calcularGastado(presupuesto);
                doc.add(new Paragraph("Presupuesto: " + Formato.soles(presupuesto.getMontoPresupuesto())
                        + "   ·   Gastado: " + Formato.soles(gastado)
                        + " (" + Formato.porcentaje(gastado, presupuesto.getMontoPresupuesto()) + " %)"
                        + "   ·   Disponible: " + Formato.soles(presupuesto.getMontoPresupuesto().subtract(gastado)), NORMAL));
            }

            // Gastos por categoría
            seccion(doc, "Gastos por categoría");
            if (resumen.getGastosPorCategoria().isEmpty()) {
                doc.add(new Paragraph("No hay gastos registrados en este mes.", NORMAL));
            } else {
                PdfPTable tabla = tabla(new float[]{4, 2, 1.2f}, "Categoría", "Monto", "% del total");
                for (Map.Entry<String, BigDecimal> e : resumen.getGastosPorCategoria().entrySet()) {
                    celda(tabla, e.getKey(), Element.ALIGN_LEFT);
                    celda(tabla, Formato.soles(e.getValue()), Element.ALIGN_RIGHT);
                    celda(tabla, Formato.porcentaje(e.getValue(), resumen.getGastos()) + " %", Element.ALIGN_RIGHT);
                }
                doc.add(tabla);
            }
            doc.add(new Paragraph("Gastos hormiga del mes: " + Formato.soles(resumen.getGastosHormiga()), NEGRITA));

            // Comparación con el mes anterior
            seccion(doc, "Comparación con " + Formato.mes(mes.minusMonths(1)));
            PdfPTable comparacion = tabla(new float[]{3, 2, 2, 1.4f}, "Concepto", Formato.mes(mes.minusMonths(1)), Formato.mes(mes), "Variación");
            filaComparacion(comparacion, "Ingresos", anterior.getIngresos(), resumen.getIngresos());
            filaComparacion(comparacion, "Gastos", anterior.getGastos(), resumen.getGastos());
            filaComparacion(comparacion, "Balance", anterior.getBalance(), resumen.getBalance());
            filaComparacion(comparacion, "Ahorro en metas", anterior.getAhorroMetas(), resumen.getAhorroMetas());
            doc.add(comparacion);

            // Metas
            seccion(doc, "Metas de ahorro");
            if (metas.isEmpty()) {
                doc.add(new Paragraph("Aún no hay metas de ahorro registradas.", NORMAL));
            } else {
                PdfPTable tabla = tabla(new float[]{3, 2, 2, 1.2f, 1.6f}, "Meta", "Ahorrado", "Objetivo", "Progreso", "Fecha objetivo");
                for (MetaAhorro m : metas) {
                    celda(tabla, m.getNombreMeta(), Element.ALIGN_LEFT);
                    celda(tabla, Formato.soles(m.getMontoAhorrado()), Element.ALIGN_RIGHT);
                    celda(tabla, Formato.soles(m.getMontoObjetivo()), Element.ALIGN_RIGHT);
                    celda(tabla, MetaAhorroServicio.calcularProgreso(m) + " %", Element.ALIGN_RIGHT);
                    celda(tabla, Formato.fecha(m.getFechaObjetivo()), Element.ALIGN_CENTER);
                }
                doc.add(tabla);
            }

            // Recomendaciones
            seccion(doc, "Recomendaciones");
            for (Recomendacion r : recomendaciones) {
                Paragraph p = new Paragraph();
                p.add(new Chunk("• " + r.getTitulo() + ": ", NEGRITA));
                p.add(new Chunk(r.getDescripcion(), NORMAL));
                p.setSpacingAfter(4);
                doc.add(p);
            }

            // Detalle de movimientos
            seccion(doc, "Movimientos del mes (" + movimientos.size() + ")");
            if (!movimientos.isEmpty()) {
                PdfPTable tabla = tabla(new float[]{1.4f, 3.6f, 2.2f, 1.6f}, "Fecha", "Descripción", "Categoría", "Monto");
                for (Movimiento mov : movimientos) {
                    boolean hormiga = mov instanceof Gasto && ((Gasto) mov).isEsGastoHormiga();
                    celda(tabla, Formato.fecha(mov.getFecha()), Element.ALIGN_CENTER);
                    celda(tabla, (mov.getDescripcion() == null ? "" : mov.getDescripcion()) + (hormiga ? " (hormiga)" : ""), Element.ALIGN_LEFT);
                    celda(tabla, mov.getNombreCategoria(), Element.ALIGN_LEFT);
                    celda(tabla, Formato.soles(mov.aplicarABalance()), Element.ALIGN_RIGHT);
                }
                doc.add(tabla);
            }

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Documento generado automáticamente por FinanZ – Plataforma Web de Educación y "
                    + "Gestión Financiera Personal. La información depende de los datos registrados por el usuario.", PEQUENA));
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        } finally {
            doc.close();
        }

        reporteDAO.insertarReporte(new ReporteFinanciero(idUsuario, mes.toString(), "PDF"));
        return salida.toByteArray();
    }

    public List<ReporteFinanciero> historial(int idUsuario) throws SQLException {
        return reporteDAO.listarReportes(idUsuario, 10);
    }

    private void seccion(Document doc, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, SUBTITULO);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void kpi(PdfPTable tabla, String etiqueta, String valor, Color color) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setBackgroundColor(FONDO);
        celda.setPadding(8);
        celda.addElement(new Paragraph(etiqueta, PEQUENA));
        celda.addElement(new Paragraph(valor, new Font(Font.HELVETICA, 12, Font.BOLD, color)));
        tabla.addCell(celda);
    }

    private PdfPTable tabla(float[] anchos, String... cabeceras) throws DocumentException {
        PdfPTable tabla = new PdfPTable(anchos.length);
        tabla.setWidthPercentage(100);
        tabla.setWidths(anchos);
        tabla.setSpacingAfter(6);
        for (String c : cabeceras) {
            PdfPCell celda = new PdfPCell(new Phrase(c, CABECERA));
            celda.setBackgroundColor(AZUL);
            celda.setPadding(5);
            celda.setBorderColor(AZUL);
            tabla.addCell(celda);
        }
        tabla.setHeaderRows(1);
        return tabla;
    }

    private void celda(PdfPTable tabla, String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, NORMAL));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        celda.setBorderColor(new Color(220, 226, 232));
        tabla.addCell(celda);
    }

    private void filaComparacion(PdfPTable tabla, String concepto, BigDecimal antes, BigDecimal ahora) {
        Integer variacion = EstadisticaServicio.variacion(antes, ahora);
        celda(tabla, concepto, Element.ALIGN_LEFT);
        celda(tabla, Formato.soles(antes), Element.ALIGN_RIGHT);
        celda(tabla, Formato.soles(ahora), Element.ALIGN_RIGHT);
        celda(tabla, variacion == null ? "—" : (variacion > 0 ? "+" : "") + variacion + " %", Element.ALIGN_RIGHT);
    }
}
