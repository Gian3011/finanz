package com.mycompany.finanz.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Formatos de presentación usados en las vistas (moneda, fechas, meses).
 */
public final class Formato {

    private static final Locale ES = new Locale("es", "PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    /** 1234.5 -> "S/ 1,234.50" */
    public static String soles(BigDecimal monto) {
        BigDecimal valor = monto == null ? BigDecimal.ZERO : monto;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.00", simbolos);
        String texto = df.format(valor.abs().setScale(2, RoundingMode.HALF_UP));
        return (valor.signum() < 0 ? "- S/ " : "S/ ") + texto;
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(FECHA_HORA);
    }

    /** 2026-10 -> "octubre 2026" */
    public static String mes(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.FULL, ES) + " " + mes.getYear();
    }

    /** 2026-10 -> "oct 26" (para ejes de gráficos) */
    public static String mesCorto(YearMonth mes) {
        String nombre = mes.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return nombre + " " + String.valueOf(mes.getYear()).substring(2);
    }

    /** Porcentaje entero (0 si el total es cero). */
    public static int porcentaje(BigDecimal parte, BigDecimal total) {
        if (parte == null || total == null || total.signum() == 0) {
            return 0;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }
}
