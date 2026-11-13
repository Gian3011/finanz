package com.mycompany.finanz.util;

import com.mycompany.finanz.modelo.FormatoReporte;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleDocxReportConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Llena la plantilla JasperReports del resumen financiero y la exporta a PDF, Excel o Word.
 * La plantilla (.jrxml) se compila una sola vez y se reutiliza en cada solicitud.
 */
public final class ReporteJasper {

    private static final String PLANTILLA = "/reportes/resumen_financiero.jrxml";

    private static volatile JasperReport plantilla;

    private ReporteJasper() {
    }

    public static byte[] generar(Map<String, Object> parametros, FormatoReporte formato) {
        try {
            Map<String, Object> valores = new HashMap<>(parametros);
            if (formato == FormatoReporte.EXCEL) {
                // En una hoja de cálculo no hay páginas: sin cortes ni encabezados repetidos
                valores.put(JRParameter.IS_IGNORE_PAGINATION, Boolean.TRUE);
            }
            // El reporte se arma con parámetros y tablas; la banda de detalle se imprime una sola vez
            JasperPrint impreso = JasperFillManager.fillReport(plantilla(), valores, new JREmptyDataSource(1));
            switch (formato) {
                case EXCEL:
                    return exportarExcel(impreso);
                case WORD:
                    return exportarWord(impreso);
                default:
                    return JasperExportManager.exportReportToPdf(impreso);
            }
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el reporte en " + formato, e);
        }
    }

    private static JasperReport plantilla() throws JRException {
        JasperReport r = plantilla;
        if (r == null) {
            synchronized (ReporteJasper.class) {
                r = plantilla;
                if (r == null) {
                    try (InputStream in = ReporteJasper.class.getResourceAsStream(PLANTILLA)) {
                        if (in == null) {
                            throw new JRException("No se encontró la plantilla " + PLANTILLA);
                        }
                        r = JasperCompileManager.compileReport(in);
                    } catch (IOException e) {
                        throw new JRException("No se pudo leer la plantilla " + PLANTILLA, e);
                    }
                    plantilla = r;
                }
            }
        }
        return r;
    }

    private static byte[] exportarExcel(JasperPrint impreso) throws JRException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        JRXlsxExporter exportador = new JRXlsxExporter();
        exportador.setExporterInput(new SimpleExporterInput(impreso));
        exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));

        SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
        config.setOnePagePerSheet(false);
        config.setRemoveEmptySpaceBetweenRows(true);
        config.setRemoveEmptySpaceBetweenColumns(true);
        config.setWhitePageBackground(false);
        config.setIgnorePageMargins(true);
        config.setSheetNames(new String[]{"Resumen"});
        exportador.setConfiguration(config);

        exportador.exportReport();
        return salida.toByteArray();
    }

    private static byte[] exportarWord(JasperPrint impreso) throws JRException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        JRDocxExporter exportador = new JRDocxExporter();
        exportador.setExporterInput(new SimpleExporterInput(impreso));
        exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));

        SimpleDocxReportConfiguration config = new SimpleDocxReportConfiguration();
        config.setFlexibleRowHeight(true);
        exportador.setConfiguration(config);

        exportador.exportReport();
        return salida.toByteArray();
    }
}
