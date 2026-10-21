package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Presupuesto {

    private int idPresupuesto;
    private int idUsuario;
    private LocalDate periodo; // representa el mes como su primer día, ej. 2026-09-01
    private BigDecimal montoPresupuesto;

    /** Mes al que corresponde el presupuesto (ej. 2026-10). */
    public java.time.YearMonth getMes() {
        return java.time.YearMonth.from(periodo);
    }

    public Presupuesto() {
    }

    public Presupuesto(int idUsuario, LocalDate periodo, BigDecimal montoPresupuesto) {
        this.idUsuario = idUsuario;
        this.periodo = periodo;
        this.montoPresupuesto = montoPresupuesto;
    }

    public int getIdPresupuesto() {
        return idPresupuesto;
    }

    public void setIdPresupuesto(int idPresupuesto) {
        this.idPresupuesto = idPresupuesto;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDate getPeriodo() {
        return periodo;
    }

    public void setPeriodo(LocalDate periodo) {
        this.periodo = periodo;
    }

    public BigDecimal getMontoPresupuesto() {
        return montoPresupuesto;
    }

    public void setMontoPresupuesto(BigDecimal montoPresupuesto) {
        this.montoPresupuesto = montoPresupuesto;
    }
}
