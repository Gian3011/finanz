package com.mycompany.finanz.modelo;

import java.time.LocalDateTime;

public class ReporteFinanciero {

    private int idReporte;
    private int idUsuario;
    private LocalDateTime fechaGeneracion;
    private String periodo; // "yyyy-MM"
    private String formato; // "PDF"

    public ReporteFinanciero() {
    }

    public ReporteFinanciero(int idUsuario, String periodo, String formato) {
        this.idUsuario = idUsuario;
        this.periodo = periodo;
        this.formato = formato;
    }

    public int getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(int idReporte) {
        this.idReporte = idReporte;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }
}
