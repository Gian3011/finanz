package com.mycompany.finanz.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Resumen financiero enviado por correo y el resultado de su verificación de entrega. */
public class EnvioReporte {

    public static final String ENVIADO = "ENVIADO";
    public static final String REBOTADO = "REBOTADO";
    public static final String ERROR = "ERROR";

    private int idEnvio;
    private int idUsuario;
    private LocalDate periodo; // primer día del mes del reporte
    private String formato;    // "PDF", "XLSX" o "DOCX"
    private String correoDestino;
    private LocalDateTime fechaEnvio;
    private String estado;

    public EnvioReporte() {
    }

    public EnvioReporte(int idUsuario, LocalDate periodo, String formato, String correoDestino, String estado) {
        this.idUsuario = idUsuario;
        this.periodo = periodo;
        this.formato = formato;
        this.correoDestino = correoDestino;
        this.estado = estado;
    }

    public int getIdEnvio() {
        return idEnvio;
    }

    public void setIdEnvio(int idEnvio) {
        this.idEnvio = idEnvio;
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

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getCorreoDestino() {
        return correoDestino;
    }

    public void setCorreoDestino(String correoDestino) {
        this.correoDestino = correoDestino;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
