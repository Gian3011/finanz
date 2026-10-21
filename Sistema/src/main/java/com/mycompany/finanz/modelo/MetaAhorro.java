package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MetaAhorro {

    public static final String EN_PROGRESO = "EN_PROGRESO";
    public static final String CUMPLIDA = "CUMPLIDA";

    private int idMeta;
    private int idUsuario;
    private String nombreMeta;
    private BigDecimal montoObjetivo;
    private LocalDate fechaObjetivo;
    private String estado;
    private LocalDate fechaCreacion;
    private BigDecimal montoAhorrado = BigDecimal.ZERO; // suma de aportes (calculado)

    public MetaAhorro() {
    }

    public MetaAhorro(int idUsuario, String nombreMeta, BigDecimal montoObjetivo, LocalDate fechaObjetivo) {
        this.idUsuario = idUsuario;
        this.nombreMeta = nombreMeta;
        this.montoObjetivo = montoObjetivo;
        this.fechaObjetivo = fechaObjetivo;
        this.estado = EN_PROGRESO;
    }

    public BigDecimal getMontoFaltante() {
        BigDecimal faltante = montoObjetivo.subtract(montoAhorrado);
        return faltante.signum() < 0 ? BigDecimal.ZERO : faltante;
    }

    public boolean isCumplida() {
        return CUMPLIDA.equals(estado);
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

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public BigDecimal getMontoAhorrado() {
        return montoAhorrado;
    }

    public void setMontoAhorrado(BigDecimal montoAhorrado) {
        this.montoAhorrado = montoAhorrado == null ? BigDecimal.ZERO : montoAhorrado;
    }
}
