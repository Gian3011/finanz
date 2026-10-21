package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MetaAhorro {

    private int idMeta;
    private int idUsuario;
    private String nombreMeta;
    private BigDecimal montoObjetivo;
    private LocalDate fechaObjetivo;
    private String estado; // por ejemplo: "EN_PROGRESO", "CUMPLIDA"

    public MetaAhorro() {
    }

    public MetaAhorro(int idUsuario, String nombreMeta, BigDecimal montoObjetivo, LocalDate fechaObjetivo) {
        this.idUsuario = idUsuario;
        this.nombreMeta = nombreMeta;
        this.montoObjetivo = montoObjetivo;
        this.fechaObjetivo = fechaObjetivo;
        this.estado = "EN_PROGRESO";
    }

    public int getIdMeta() {
        return idMeta;
    }

    public void setIdMeta(int idMeta) {
        this.idMeta = idMeta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreMeta() {
        return nombreMeta;
    }

    public void setNombreMeta(String nombreMeta) {
        this.nombreMeta = nombreMeta;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public void setMontoObjetivo(BigDecimal montoObjetivo) {
        this.montoObjetivo = montoObjetivo;
    }

    public LocalDate getFechaObjetivo() {
        return fechaObjetivo;
    }

    public void setFechaObjetivo(LocalDate fechaObjetivo) {
        this.fechaObjetivo = fechaObjetivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
