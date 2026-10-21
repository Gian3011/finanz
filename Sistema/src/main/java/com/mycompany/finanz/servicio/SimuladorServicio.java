package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.SimulacionAhorro;
import com.mycompany.finanz.modelo.SimulacionAhorro.Frecuencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulador de escenarios de ahorro (cálculos puros, sin base de datos).
 *  - simular(): aportando X cada periodo, ¿cuándo llego al objetivo?
 *  - aporteNecesario(): para llegar en una fecha, ¿cuánto debo aportar cada periodo?
 */
public class SimuladorServicio {

    public static final int MAX_PERIODOS = 600;

    public SimulacionAhorro simular(int idUsuario, BigDecimal objetivo, BigDecimal inicial, BigDecimal aporte,
                                    Frecuencia frecuencia, LocalDate desde) throws ValidacionException {
        if (inicial.compareTo(objetivo) >= 0) {
            throw new ValidacionException("El monto inicial ya cubre el objetivo: ¡meta lograda!");
        }
        BigDecimal faltante = objetivo.subtract(inicial);
        long periodos = faltante.divide(aporte, 0, RoundingMode.CEILING).longValue();
        if (periodos > MAX_PERIODOS) {
            throw new ValidacionException("Con ese aporte necesitarías " + periodos + " " + frecuencia.getPlural()
                    + ". Prueba con un aporte mayor.");
        }

        SimulacionAhorro s = new SimulacionAhorro();
        s.setIdUsuario(idUsuario);
        s.setMontoObjetivo(objetivo);
        s.setMontoInicial(inicial);
        s.setAportePeriodico(aporte);
        s.setFrecuencia(frecuencia);
        s.setNumeroPeriodos((int) periodos);
        s.setFechaObjetivo(frecuencia.avanzar(desde, periodos));
        return s;
    }

    /** Aporte por periodo necesario para llegar al objetivo en la fecha indicada. */
    public BigDecimal aporteNecesario(BigDecimal objetivo, BigDecimal inicial, LocalDate fechaObjetivo,
                                      Frecuencia frecuencia, LocalDate desde) throws ValidacionException {
        long periodos = periodosHasta(desde, fechaObjetivo, frecuencia);
        if (periodos <= 0) {
            throw new ValidacionException("La fecha objetivo debe estar al menos a un periodo ("
                    + frecuencia.getSingular() + ") de distancia.");
        }
        BigDecimal faltante = objetivo.subtract(inicial);
        if (faltante.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return faltante.divide(BigDecimal.valueOf(periodos), 2, RoundingMode.CEILING);
    }

    public static long periodosHasta(LocalDate desde, LocalDate hasta, Frecuencia frecuencia) {
        switch (frecuencia) {
            case SEMANAL: return ChronoUnit.WEEKS.between(desde, hasta);
            case QUINCENAL: return ChronoUnit.DAYS.between(desde, hasta) / 15;
            default: return ChronoUnit.MONTHS.between(desde, hasta);
        }
    }

    /**
     * Saldo acumulado al final de cada periodo (para el gráfico de proyección).
     * Si hay muchos periodos se toman puntos espaciados para no saturar el gráfico.
     */
    public List<BigDecimal> proyeccion(SimulacionAhorro s) {
        List<BigDecimal> puntos = new ArrayList<>();
        int total = s.getNumeroPeriodos();
        int paso = Math.max(1, (int) Math.ceil(total / 24.0));
        for (int p = 0; p <= total; p += paso) {
            puntos.add(saldoEn(s, p));
        }
        if ((total % paso) != 0) {
            puntos.add(saldoEn(s, total));
        }
        return puntos;
    }

    public List<Integer> etiquetasProyeccion(SimulacionAhorro s) {
        List<Integer> etiquetas = new ArrayList<>();
        int total = s.getNumeroPeriodos();
        int paso = Math.max(1, (int) Math.ceil(total / 24.0));
        for (int p = 0; p <= total; p += paso) {
            etiquetas.add(p);
        }
        if ((total % paso) != 0) {
            etiquetas.add(total);
        }
        return etiquetas;
    }

    private BigDecimal saldoEn(SimulacionAhorro s, int periodo) {
        BigDecimal saldo = s.getMontoInicial().add(s.getAportePeriodico().multiply(BigDecimal.valueOf(periodo)));
        return saldo.min(s.getMontoObjetivo());
    }
}
