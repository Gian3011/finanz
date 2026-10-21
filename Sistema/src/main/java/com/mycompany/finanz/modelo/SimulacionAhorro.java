package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Escenario calculado por el simulador de ahorro: cuántos periodos (y hasta
 * qué fecha) toma juntar el monto objetivo aportando un monto fijo.
 */
public class SimulacionAhorro {

    private int idSimulacion;
    private int idUsuario;
    private BigDecimal montoObjetivo;
    private BigDecimal montoInicial = BigDecimal.ZERO;
    private BigDecimal aportePeriodico;
    private Frecuencia frecuencia;
    private int numeroPeriodos;
    private LocalDate fechaObjetivo;
    private LocalDateTime fechaCreacion;

    public enum Frecuencia {
        SEMANAL("semana", "semanas"),
        QUINCENAL("quincena", "quincenas"),
        MENSUAL("mes", "meses");

        private final String singular;
        private final String plural;

        Frecuencia(String singular, String plural) {
            this.singular = singular;
            this.plural = plural;
        }

        public String getSingular() {
            return singular;
        }

        public String getPlural() {
            return plural;
        }

        /** Fecha a la que se llega avanzando "periodos" veces desde "inicio". */
        public LocalDate avanzar(LocalDate inicio, long periodos) {
            switch (this) {
                case SEMANAL: return inicio.plusWeeks(periodos);
                case QUINCENAL: return inicio.plusDays(periodos * 15);
                default: return inicio.plusMonths(periodos);
            }
        }
    }

    public int getIdSimulacion() {
        return idSimulacion;
    }

    public void setIdSimulacion(int idSimulacion) {
        this.idSimulacion = idSimulacion;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public void setMontoObjetivo(BigDecimal montoObjetivo) {
        this.montoObjetivo = montoObjetivo;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial == null ? BigDecimal.ZERO : montoInicial;
    }

    public BigDecimal getAportePeriodico() {
        return aportePeriodico;
    }

    public void setAportePeriodico(BigDecimal aportePeriodico) {
        this.aportePeriodico = aportePeriodico;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(Frecuencia frecuencia) {
        this.frecuencia = frecuencia;
    }

    public int getNumeroPeriodos() {
        return numeroPeriodos;
    }

    public void setNumeroPeriodos(int numeroPeriodos) {
        this.numeroPeriodos = numeroPeriodos;
    }

    public LocalDate getFechaObjetivo() {
        return fechaObjetivo;
    }

    public void setFechaObjetivo(LocalDate fechaObjetivo) {
        this.fechaObjetivo = fechaObjetivo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
