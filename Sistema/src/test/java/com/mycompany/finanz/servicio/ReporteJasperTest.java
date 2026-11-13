package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.FormatoReporte;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Ingreso;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.modelo.Recomendacion;
import com.mycompany.finanz.modelo.ResumenPeriodo;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.util.ReporteJasper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Genera el resumen financiero con JasperReports en los tres formatos, sin base de datos. */
class ReporteJasperTest {

    private static final YearMonth MES = YearMonth.of(2026, 10);

    @Test
    void reconoceLosFormatos() {
        assertEquals(FormatoReporte.PDF, FormatoReporte.desde("pdf"));
        assertEquals(FormatoReporte.EXCEL, FormatoReporte.desde("XLSX"));
        assertEquals(FormatoReporte.WORD, FormatoReporte.desde("docx"));
        assertNull(FormatoReporte.desde("exe"));
    }

    @Test
    void generaPdf() throws IOException {
        byte[] pdf = generar(datosDeEjemplo(), FormatoReporte.PDF);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void generaExcel() throws IOException {
        byte[] xlsx = generar(datosDeEjemplo(), FormatoReporte.EXCEL);
        String hoja = entrada(xlsx, "xl/sharedStrings.xml");
        assertTrue(hoja.contains("Resumen financiero de octubre 2026"), "título en la hoja");
        assertTrue(hoja.contains("Alimentación"), "tildes correctas en las categorías");
    }

    @Test
    void generaWord() throws IOException {
        byte[] docx = generar(datosDeEjemplo(), FormatoReporte.WORD);
        String documento = entrada(docx, "word/document.xml");
        assertTrue(documento.contains("Metas de ahorro"));
        assertTrue(documento.contains("Café de la mañana"));
    }

    @Test
    void generaReporteSinDatos() {
        Map<String, Object> vacio = ReporteServicio.parametros(new Usuario("Ana", "Pérez", "ana@correo.com"), MES,
                new ResumenPeriodo(MES), new ResumenPeriodo(MES.minusMonths(1)), null, null,
                List.of(), List.of(), List.of(), LocalDateTime.of(2026, 11, 1, 9, 0));
        for (FormatoReporte f : FormatoReporte.values()) {
            assertTrue(ReporteJasper.generar(vacio, f).length > 0, "reporte vacío en " + f);
        }
    }

    @Test
    void lasCeldasVaciasNoMuestranNull() throws IOException {
        Map<String, Object> vacio = ReporteServicio.parametros(new Usuario("Ana", "Pérez", "ana@correo.com"), MES,
                new ResumenPeriodo(MES), new ResumenPeriodo(MES.minusMonths(1)), null, null,
                List.of(), List.of(), List.of(), LocalDateTime.of(2026, 11, 1, 9, 0));
        String hoja = entrada(ReporteJasper.generar(vacio, FormatoReporte.EXCEL), "xl/sharedStrings.xml");
        assertTrue(hoja.contains("No hay movimientos registrados en este mes."));
        assertFalse(hoja.contains(">null<"), "una celda vacía muestra 'null'");
    }

    private static byte[] generar(Map<String, Object> parametros, FormatoReporte formato) throws IOException {
        byte[] archivo = ReporteJasper.generar(parametros, formato);
        assertTrue(archivo.length > 1000, "archivo " + formato + " con contenido");
        // Copia para revisarlo a mano: target/reportes-prueba/
        Path carpeta = Path.of("target", "reportes-prueba");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve("resumen." + formato.getExtension()), archivo);
        return archivo;
    }

    private static String entrada(byte[] zip, String nombre) throws IOException {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.getName().equals(nombre)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        fail("No se encontró " + nombre);
        return "";
    }

    private static Map<String, Object> datosDeEjemplo() {
        Usuario usuario = new Usuario("Juan", "Pérez", "juan@correo.com");

        ResumenPeriodo resumen = new ResumenPeriodo(MES);
        resumen.setIngresos(new BigDecimal("2850.00"));
        resumen.setGastos(new BigDecimal("1394.90"));
        resumen.setAhorroMetas(new BigDecimal("650.00"));
        resumen.setGastosHormiga(new BigDecimal("51.50"));
        Map<String, BigDecimal> categorias = new LinkedHashMap<>();
        categorias.put("Vivienda", new BigDecimal("800.00"));
        categorias.put("Alimentación", new BigDecimal("420.40"));
        categorias.put("Snacks y café", new BigDecimal("51.50"));
        categorias.put("Transporte", new BigDecimal("123.00"));
        resumen.setGastosPorCategoria(categorias);

        ResumenPeriodo anterior = new ResumenPeriodo(MES.minusMonths(1));
        anterior.setIngresos(new BigDecimal("2500.00"));
        anterior.setGastos(new BigDecimal("1990.00"));
        anterior.setAhorroMetas(new BigDecimal("300.00"));

        Presupuesto presupuesto = new Presupuesto(1, MES.atDay(1), new BigDecimal("1800.00"));

        MetaAhorro laptop = new MetaAhorro(1, "Laptop nueva", new BigDecimal("2800.00"), LocalDate.of(2027, 3, 31));
        laptop.setMontoAhorrado(new BigDecimal("400.00"));
        MetaAhorro emergencia = new MetaAhorro(1, "Fondo de emergencia", new BigDecimal("3000.00"), LocalDate.of(2027, 6, 30));
        emergencia.setMontoAhorrado(new BigDecimal("850.00"));

        List<Recomendacion> recomendaciones = List.of(
                new Recomendacion(1, "¡Buen ritmo de ahorro!", "Este mes conservas el 51 % de tus ingresos.", Recomendacion.LOGRO),
                new Recomendacion(1, "Cuidado con los gastos hormiga", "Llevas S/ 51.50 en gastos hormiga este mes. "
                        + "Si mantienes ese ritmo serían cerca de S/ 618.00 al año.", Recomendacion.ALERTA));

        List<Movimiento> movimientos = new ArrayList<>();
        Ingreso sueldo = new Ingreso(1, 1, new BigDecimal("2850.00"), MES.atDay(1), "Sueldo de octubre", "Trabajo");
        sueldo.setNombreCategoria("Sueldo");
        movimientos.add(sueldo);
        for (int dia = 2; dia <= 30; dia += 2) {
            Gasto cafe = new Gasto(1, 3, new BigDecimal("6.50"), MES.atDay(dia), "Café de la mañana");
            cafe.setNombreCategoria("Snacks y café");
            cafe.setEsGastoHormiga(dia > 8);
            movimientos.add(cafe);
        }

        return ReporteServicio.parametros(usuario, MES, resumen, anterior, presupuesto, new BigDecimal("1394.90"),
                List.of(laptop, emergencia), recomendaciones, movimientos, LocalDateTime.of(2026, 11, 1, 9, 30));
    }
}
