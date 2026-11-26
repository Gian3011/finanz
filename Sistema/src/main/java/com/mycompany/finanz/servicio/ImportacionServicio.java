package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.modelo.MovimientoDetectado;
import com.mycompany.finanz.modelo.ResultadoImportacion;
import com.mycompany.finanz.modelo.ResultadoImportacion.TipoDocumento;
import com.mycompany.finanz.util.LectorPdf;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importación de gastos desde PDF (RF22):
 *  - Estado de cuenta de tarjeta de crédito: cada línea que empieza con una fecha
 *    y termina con un importe es un consumo. Pagos, abonos y devoluciones se omiten.
 *  - Comprobante electrónico (boleta, factura, ticket): un solo gasto con el
 *    importe total, la fecha de emisión y el nombre del emisor.
 * El resultado se muestra al usuario para que lo revise; nada se registra
 * hasta que lo confirma.
 */
public class ImportacionServicio {

    /** Límite de filas por archivo, para que la vista previa siga siendo manejable. */
    public static final int MAX_MOVIMIENTOS = 300;

    private static final String MESES = "ENE|FEB|MAR|ABR|MAY|JUN|JUL|AGO|SET|SEP|OCT|NOV|DIC|JAN|APR|AUG|DEC";

    /** Fecha corta o completa: 15/10, 15-10-2026, 15/10/26, 15OCT, 15 OCT, 15-Oct-26, 15 OCT 2026. */
    private static final String FECHA = "\\d{1,2}(?:[/.\\-]\\d{1,2}|[ /.\\-]?[A-Za-z]{3}\\.?)(?:[/.\\-](?:\\d{4}|\\d{2})|\\s\\d{4})?";
    private static final Pattern PARTES_FECHA = Pattern.compile(
            "(\\d{1,2})(?:[/.\\-](\\d{1,2})|[ /.\\-]?([A-Za-z]{3})\\.?)(?:[/.\\-](\\d{4}|\\d{2})|\\s(\\d{4}))?");

    private static final String NUMERO = "(?:\\d{1,3}(?:[,.]\\d{3})+|\\d+)[.,]\\d{2}";
    private static final String IMPORTE = "-?\\s?(?:S/\\.?|US\\$|\\$)?\\s?-?" + NUMERO + "(?:\\s?-|\\s?CR)?";
    private static final Pattern PATRON_IMPORTE = Pattern.compile(IMPORTE, Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_NUMERO = Pattern.compile(NUMERO);

    /** Línea de consumo: fecha de consumo, fecha de proceso (opcional), descripción e importe(s). */
    private static final Pattern LINEA_CONSUMO = Pattern.compile(
            "^\\s*(" + FECHA + ")(?:\\s+" + FECHA + ")?\\s+(.+?)((?:\\s+" + IMPORTE + "){1,2})\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** Movimientos que no son gastos: pagos a la tarjeta, abonos, devoluciones y totales. */
    private static final Pattern NO_ES_CONSUMO = Pattern.compile(
            "^(SU\\s+)?PAGO\\b|PAGO\\s+(RECIBIDO|TARJETA|BANCA|EN LINEA|GRACIAS|MINIMO|TOTAL)|GRACIAS POR SU PAGO"
            + "|\\bABONO\\b|DEVOLUCION|EXTORNO|NOTA DE CREDITO|REVERSION|\\bSALDO\\b|\\bTOTAL\\b|DEUDA\\b");

    private static final Pattern ES_ESTADO_CUENTA = Pattern.compile(
            "ESTADO DE CUENTA|TARJETA DE CREDITO|LINEA DE CREDITO|FECHA DE (CORTE|CIERRE|FACTURACION)|PAGO MINIMO");
    private static final Pattern ES_COMPROBANTE = Pattern.compile(
            "BOLETA|FACTURA|COMPROBANTE|TICKET|RECIBO|CONSTANCIA DE PAGO|NOTA DE VENTA");

    private static final Pattern FECHA_REFERENCIA = Pattern.compile(
            "FECHA\\s+DE\\s+(?:CORTE|CIERRE|FACTURACION)\\s*:?\\s*(\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{4})");
    private static final Pattern FECHA_COMPLETA = Pattern.compile(
            "(?<!\\d)(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4})(?!\\d)|(?<!\\d)(\\d{4})-(\\d{2})-(\\d{2})(?!\\d)");
    private static final Pattern FECHA_EMISION = Pattern.compile(
            "FECHA(?:\\s+DE)?\\s*(?:EMISION|EMIS\\.?|COMPRA|PAGO|OPERACION|VENTA)?\\s*:?\\s*"
            + "((?:\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{4})|(?:\\d{4}-\\d{2}-\\d{2}))");

    private static final Pattern TOTAL_FUERTE = Pattern.compile(
            "IMPORTE\\s+TOTAL|TOTAL\\s+A\\s+PAGAR|TOTAL\\s+PAGADO|MONTO\\s+TOTAL|TOTAL\\s+VENTA|TOTAL\\s+COBRADO|MONTO\\s+PAGADO");
    private static final Pattern TOTAL_SIMPLE = Pattern.compile("^\\s*TOTAL\\b");
    private static final Pattern NO_ES_TOTAL = Pattern.compile(
            "SUB\\s*-?\\s*TOTAL|GRAVAD|EXONERAD|INAFECT|IGV|I\\.G\\.V|DESCUENTO|ICBPER|ISC|ANTICIPO|REDONDEO|ITEMS|CANTIDAD");
    private static final Pattern MONTO_SIMPLE = Pattern.compile("^\\s*(MONTO|IMPORTE)\\b");
    private static final Pattern SERIE_NUMERO = Pattern.compile("\\b([BFE][A-Z0-9]{3})\\s*-\\s*0*(\\d{1,8})\\b");
    private static final Pattern NO_ES_EMISOR = Pattern.compile(
            "BOLETA|FACTURA|ELECTRONIC|R\\.?U\\.?C|COMPROBANTE|TICKET|RECIBO|FECHA|DIRECCION|DOMICILIO|TELEF|"
            + "^(AV|JR|CALLE|CAL|PSJE|MZ)\\b|SENOR|CLIENTE|DNI|^\\d|CANT\\b|CANTIDAD|DESCRIPCION|P\\. ?UNIT|IMPORTE");
    private static final Pattern RAZON_SOCIAL = Pattern.compile(
            "[A-Za-zÁÉÍÓÚÑáéíóúñ&].*?\\b(S\\.\\s?A\\.\\s?C\\.?|S\\.\\s?A\\.\\s?A\\.?|E\\.\\s?I\\.\\s?R\\.\\s?L\\.?|S\\.\\s?R\\.\\s?L\\.?|S\\.\\s?A\\.|SAC|SAA|EIRL|SRL)(?![A-Za-z])");
    private static final Pattern DATO_COMPROBANTE = Pattern.compile(
            "BOLETA|FACTURA|R\\.?\\s?U\\.?\\s?C\\b|COMPROBANTE|TICKET|RECIBO|NOTA DE VENTA", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOLARES = Pattern.compile("US\\$|\\bUSD\\b|DOLAR");

    private final MovimientoServicio movimientoServicio;

    public ImportacionServicio() {
        this(new MovimientoServicio());
    }

    public ImportacionServicio(MovimientoServicio movimientoServicio) {
        this.movimientoServicio = movimientoServicio;
    }

    /** Lee el PDF y prepara la vista previa con categoría sugerida y avisos por fila. */
    public ResultadoImportacion analizar(int idUsuario, byte[] pdf, String clave, String nombreArchivo)
            throws ValidacionException, SQLException {
        String texto = LectorPdf.extraerTexto(pdf, clave);
        LocalDate hoy = LocalDate.now();
        ResultadoImportacion resultado = interpretar(texto, hoy);
        resultado.setNombreArchivo(nombreArchivo);
        completar(resultado, movimientoServicio.categoriasPorTipo(idUsuario, "GASTO"),
                movimientoServicio.listar(idUsuario), hoy);
        return resultado;
    }

    // ------------------------------------------------------------------
    // Interpretación del texto (sin base de datos: se prueba con JUnit)
    // ------------------------------------------------------------------

    static ResultadoImportacion interpretar(String texto, LocalDate hoy) throws ValidacionException {
        String normal = CategorizadorGastos.normalizar(texto);
        List<String> lineas = lineas(texto);
        boolean esEstadoCuenta = ES_ESTADO_CUENTA.matcher(normal).find();
        boolean esComprobante = ES_COMPROBANTE.matcher(normal).find();

        if (esComprobante && !esEstadoCuenta) {
            MovimientoDetectado comprobante = leerComprobante(lineas, normal);
            if (comprobante != null) {
                return new ResultadoImportacion(TipoDocumento.COMPROBANTE, List.of(comprobante), 0);
            }
        }

        int[] omitidos = {0};
        List<MovimientoDetectado> consumos = leerEstadoDeCuenta(lineas, fechaReferencia(normal, hoy), omitidos);
        if (!consumos.isEmpty()) {
            if (consumos.size() > MAX_MOVIMIENTOS) {
                throw new ValidacionException("El PDF tiene más de " + MAX_MOVIMIENTOS + " consumos. Divide el archivo e impórtalo por partes.");
            }
            return new ResultadoImportacion(TipoDocumento.ESTADO_CUENTA, consumos, omitidos[0]);
        }
        if (esComprobante) {
            MovimientoDetectado comprobante = leerComprobante(lineas, normal);
            if (comprobante != null) {
                return new ResultadoImportacion(TipoDocumento.COMPROBANTE, List.of(comprobante), 0);
            }
        }
        if (omitidos[0] > 0) {
            throw new ValidacionException("El PDF solo tiene pagos o abonos a la tarjeta: no hay consumos para registrar.");
        }
        throw new ValidacionException("No encontramos consumos en el PDF. Sube el estado de cuenta de tu tarjeta "
                + "o un comprobante electrónico (boleta o factura) en PDF.");
    }

    static List<MovimientoDetectado> leerEstadoDeCuenta(List<String> lineas, LocalDate referencia, int[] omitidos) {
        List<MovimientoDetectado> consumos = new ArrayList<>();
        for (String linea : lineas) {
            Matcher m = LINEA_CONSUMO.matcher(linea);
            if (!m.matches()) {
                continue;
            }
            LocalDate fecha = fecha(m.group(1), referencia);
            if (fecha == null) {
                continue; // ej. "10 UND ARROZ 25.00" no empieza con una fecha real
            }
            String descripcion = limpiarDescripcion(m.group(2));
            String importe = elegirImporte(m.group(3));
            if (descripcion.isEmpty() || importe == null) {
                continue;
            }
            boolean esCredito = importe.contains("-") || importe.toUpperCase().endsWith("CR");
            BigDecimal monto = monto(importe);
            if (esCredito || NO_ES_CONSUMO.matcher(CategorizadorGastos.normalizar(descripcion)).find()) {
                omitidos[0]++;
                continue;
            }
            if (monto == null || monto.signum() <= 0) {
                continue;
            }
            String moneda = DOLARES.matcher(CategorizadorGastos.normalizar(linea)).find()
                    ? MovimientoDetectado.DOLARES : MovimientoDetectado.SOLES;
            MovimientoDetectado consumo = new MovimientoDetectado(fecha, recortar(descripcion, 150), monto, moneda);
            consumo.setCategoriaSugerida(CategorizadorGastos.sugerir(descripcion));
            consumos.add(consumo);
        }
        return consumos;
    }

    static MovimientoDetectado leerComprobante(List<String> lineas, String normal) {
        BigDecimal total = null;
        String lineaTotal = null;
        for (Pattern etiqueta : new Pattern[]{TOTAL_FUERTE, TOTAL_SIMPLE, MONTO_SIMPLE}) {
            for (int i = 0; i < lineas.size() && total == null; i++) {
                String n = CategorizadorGastos.normalizar(lineas.get(i));
                if (!etiqueta.matcher(n).find() || (etiqueta != TOTAL_FUERTE && NO_ES_TOTAL.matcher(n).find())) {
                    continue;
                }
                BigDecimal importe = ultimoImporte(n);
                String origen = n;
                if (importe == null && i + 1 < lineas.size()) { // "IMPORTE TOTAL" y el monto en la línea siguiente
                    origen = CategorizadorGastos.normalizar(lineas.get(i + 1));
                    importe = ultimoImporte(origen);
                }
                if (importe != null && importe.signum() > 0) {
                    total = importe;
                    lineaTotal = n + " " + origen;
                }
            }
            if (total != null) {
                break;
            }
        }
        if (total == null) {
            return null;
        }

        String emisor = emisor(lineas);
        String tipo = normal.contains("FACTURA") ? "Factura" : normal.contains("BOLETA") ? "Boleta"
                : normal.contains("TICKET") ? "Ticket" : "Comprobante";
        Matcher serie = SERIE_NUMERO.matcher(normal);
        StringBuilder descripcion = new StringBuilder(tipo);
        if (serie.find()) {
            descripcion.append(' ').append(serie.group(1)).append('-').append(serie.group(2));
        }
        if (emisor != null) {
            descripcion.append(" · ").append(emisor);
        }

        Matcher emision = FECHA_EMISION.matcher(normal);
        LocalDate fecha = emision.find() ? fechaCompleta(emision.group(1)) : null;
        if (fecha == null) {
            Matcher cualquiera = FECHA_COMPLETA.matcher(normal);
            fecha = cualquiera.find() ? fechaCompleta(cualquiera.group()) : null;
        }

        boolean enDolares = DOLARES.matcher(lineaTotal).find()
                || Pattern.compile("MONEDA\\s*:?\\s*(USD|DOLAR)").matcher(normal).find();
        MovimientoDetectado gasto = new MovimientoDetectado(fecha, recortar(descripcion.toString(), 150), total,
                enDolares ? MovimientoDetectado.DOLARES : MovimientoDetectado.SOLES);
        gasto.setCategoriaSugerida(CategorizadorGastos.sugerir(List.of(emisor == null ? "" : emisor, normal)));
        return gasto;
    }

    /** Razón social del emisor: la primera línea con texto que no es un dato del comprobante. */
    static String emisor(List<String> lineas) {
        List<String> primeras = lineas.subList(0, Math.min(lineas.size(), 10));
        // 1) La razón social suele terminar en S.A.C., S.A.A., S.A., E.I.R.L. o S.R.L.
        for (String linea : primeras) {
            Matcher sociedad = RAZON_SOCIAL.matcher(linea);
            if (sociedad.find()) {
                return recortar(linea.substring(0, sociedad.end()).trim(), 80);
            }
        }
        // 2) Si no, la primera línea con texto; en el PDF el título del comprobante puede
        //    quedar en la misma línea ("TIENDA X    BOLETA DE VENTA"): se toma lo anterior
        for (String linea : primeras) {
            Matcher dato = DATO_COMPROBANTE.matcher(linea);
            String antes = (dato.find() ? linea.substring(0, dato.start()) : linea).trim();
            String n = CategorizadorGastos.normalizar(antes);
            if (n.replaceAll("[^A-Z]", "").length() >= 3 && !NO_ES_EMISOR.matcher(n).find()) {
                return recortar(antes, 80);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Categoría, duplicados y rango de fechas
    // ------------------------------------------------------------------

    /**
     * Asigna la categoría de cada fila (la que el usuario usó antes para la misma
     * descripción o, si no, la sugerida) y desmarca las filas que conviene revisar.
     */
    static void completar(ResultadoImportacion resultado, List<Categoria> categoriasGasto,
                          List<Movimiento> historial, LocalDate hoy) {
        Map<String, Integer> porNombre = new HashMap<>();
        for (Categoria c : categoriasGasto) {
            porNombre.put(CategorizadorGastos.normalizar(c.getNombreCategoria()), c.getIdCategoria());
        }
        Integer otros = porNombre.get(CategorizadorGastos.normalizar(CategorizadorGastos.OTROS));
        int porDefecto = otros != null ? otros : categoriasGasto.isEmpty() ? 0 : categoriasGasto.get(0).getIdCategoria();

        Map<String, Integer> aprendidas = new HashMap<>();
        for (Movimiento m : historial) { // del más reciente al más antiguo
            if (m instanceof Gasto && porNombre.containsValue(m.getIdCategoria())) {
                aprendidas.putIfAbsent(clave(m.getDescripcion()), m.getIdCategoria());
            }
        }

        for (MovimientoDetectado d : resultado.getMovimientos()) {
            Integer id = aprendidas.get(clave(d.getDescripcion()));
            if (id == null) {
                id = porNombre.get(CategorizadorGastos.normalizar(d.getCategoriaSugerida()));
            }
            d.setIdCategoria(id != null ? id : porDefecto);

            if (d.getFecha() == null) {
                d.desmarcar("No encontramos la fecha: indícala antes de registrarlo.");
            } else {
                try {
                    MovimientoServicio.validarFecha(d.getFecha(), hoy);
                } catch (ValidacionException e) {
                    d.desmarcar(e.getMessage());
                }
            }
            if (d.enDolares()) {
                d.desmarcar("El importe está en dólares: escríbelo en soles antes de registrarlo.");
            }
            if (d.getFecha() != null && yaRegistrado(d, historial)) {
                d.desmarcar("Posible duplicado: ya tienes un gasto por el mismo importe en esa fecha.");
            }
        }
    }

    private static boolean yaRegistrado(MovimientoDetectado d, List<Movimiento> historial) {
        for (Movimiento m : historial) {
            if (m instanceof Gasto && d.getFecha().equals(m.getFecha()) && d.getMonto().compareTo(m.getMonto()) == 0) {
                return true;
            }
        }
        return false;
    }

    /** Clave para reconocer el mismo comercio: sin números ni símbolos (cambian en cada compra). */
    static String clave(String descripcion) {
        return CategorizadorGastos.normalizar(descripcion).replaceAll("[^A-Z ]", " ").replaceAll("\\s+", " ").trim();
    }

    // ------------------------------------------------------------------
    // Utilidades de texto, fechas e importes
    // ------------------------------------------------------------------

    static List<String> lineas(String texto) {
        List<String> lineas = new ArrayList<>();
        for (String linea : texto.split("\\R")) {
            String limpia = linea.replace('\u00A0', ' ').replaceAll("[\\t ]+", " ").trim();
            if (!limpia.isEmpty()) {
                lineas.add(limpia);
            }
        }
        return lineas;
    }

    /** Fecha de corte del estado de cuenta; da el año a los consumos que solo dicen día y mes. */
    static LocalDate fechaReferencia(String normal, LocalDate hoy) {
        Matcher corte = FECHA_REFERENCIA.matcher(normal);
        if (corte.find()) {
            LocalDate fecha = fechaCompleta(corte.group(1));
            if (fecha != null) {
                return fecha;
            }
        }
        LocalDate mayor = null;
        Matcher m = FECHA_COMPLETA.matcher(normal);
        while (m.find()) {
            LocalDate fecha = fechaCompleta(m.group());
            if (fecha != null && !fecha.isAfter(hoy) && (mayor == null || fecha.isAfter(mayor))) {
                mayor = fecha;
            }
        }
        return mayor != null ? mayor : hoy;
    }

    /** Convierte "15/10", "15OCT", "15-Oct-26" o "15/10/2026"; sin año usa el de la fecha de referencia. */
    static LocalDate fecha(String texto, LocalDate referencia) {
        Matcher m = PARTES_FECHA.matcher(texto.trim());
        if (!m.matches()) {
            return null;
        }
        int dia = Integer.parseInt(m.group(1));
        int mes = m.group(2) != null ? Integer.parseInt(m.group(2)) : mes(m.group(3));
        String anioTexto = m.group(4) != null ? m.group(4) : m.group(5);
        try {
            if (anioTexto != null) {
                int anio = Integer.parseInt(anioTexto);
                return LocalDate.of(anio < 100 ? 2000 + anio : anio, mes, dia);
            }
            LocalDate fecha = LocalDate.of(referencia.getYear(), mes, dia);
            // Un consumo de diciembre en el estado de cuenta de enero es del año anterior
            return fecha.isAfter(referencia) ? fecha.minusYears(1) : fecha;
        } catch (DateTimeException e) {
            return null;
        }
    }

    static LocalDate fechaCompleta(String texto) {
        Matcher m = FECHA_COMPLETA.matcher(texto);
        if (!m.find()) {
            return null;
        }
        try {
            if (m.group(1) != null) {
                return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            }
            return LocalDate.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static int mes(String abreviatura) {
        String[] nombres = MESES.split("\\|");
        int[] numeros = {1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 10, 11, 12, 1, 4, 8, 12};
        String a = CategorizadorGastos.normalizar(abreviatura);
        for (int i = 0; i < nombres.length; i++) {
            if (nombres[i].equals(a)) {
                return numeros[i];
            }
        }
        return 0; // LocalDate.of falla y la línea se descarta
    }

    /** Con dos columnas (ej. soles y dólares) se toma la última que tiene importe. */
    private static String elegirImporte(String columnas) {
        Matcher m = PATRON_IMPORTE.matcher(columnas);
        String elegido = null;
        while (m.find()) {
            String importe = m.group().trim();
            BigDecimal valor = monto(importe);
            if (elegido == null || (valor != null && valor.signum() != 0)) {
                elegido = importe;
            }
        }
        return elegido;
    }

    private static BigDecimal ultimoImporte(String linea) {
        Matcher m = PATRON_NUMERO.matcher(linea);
        String ultimo = null;
        while (m.find()) {
            ultimo = m.group();
        }
        return ultimo == null ? null : monto(ultimo);
    }

    /** "1,234.56", "1.234,56", "S/ 45.90", "US$ 12.00", "125.50-" -> importe positivo. */
    static BigDecimal monto(String texto) {
        Matcher m = PATRON_NUMERO.matcher(texto);
        if (!m.find()) {
            return null;
        }
        String numero = m.group();
        String entero = numero.substring(0, numero.length() - 3).replaceAll("[.,]", "");
        return new BigDecimal(entero + "." + numero.substring(numero.length() - 2));
    }

    private static String limpiarDescripcion(String texto) {
        return texto.replaceAll("\\s+", " ").replaceAll("^[\\s\\-*·|]+|[\\s\\-*·|]+$", "").trim();
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo).trim();
    }
}
