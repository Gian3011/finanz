package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AporteAhorro {

    private int idAporte;
    private int idMeta;
    private BigDecimal montoAporte;
    private LocalDate fecha;

    public AporteAhorro() {
    }

    public AporteAhorro(int idMeta, BigDecimal montoAporte, LocalDate fecha) {
        this.idMeta = idMeta;
        this.montoAporte = montoAporte;
        this.fecha = fecha;
    }

    public int getIdAporte() {
        return idAporte;
    }

    public void setIdAporte(int idAporte) {
        this.idAporte = idAporte;
    }

    public int getIdMeta() {
        return idMeta;
    }

    public void setIdMeta(int idMeta) {
        this.idMeta = idMeta;
    }

    public BigDecimal getMontoAporte() {
        return montoAporte;
    }

    public void setMontoAporte(BigDecimal montoAporte) {
        this.montoAporte = montoAporte;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
