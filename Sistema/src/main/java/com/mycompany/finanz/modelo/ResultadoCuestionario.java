package com.mycompany.finanz.modelo;

import java.time.LocalDateTime;

public class ResultadoCuestionario {

    private int idResultado;
    private int idUsuario;
    private int idCuestionario;
    private String tituloCuestionario; // solo lectura (JOIN con cuestionario)
    private int puntaje;               // respuestas correctas
    private int totalPreguntas;
    private LocalDateTime fecha;

    public ResultadoCuestionario() {
    }

    public ResultadoCuestionario(int idUsuario, int idCuestionario, int puntaje, int totalPreguntas) {
        this.idUsuario = idUsuario;
        this.idCuestionario = idCuestionario;
        this.puntaje = puntaje;
        this.totalPreguntas = totalPreguntas;
    }

    /** Nota en escala vigesimal (0-20), como en la universidad. */
    public int getNotaVigesimal() {
        return totalPreguntas == 0 ? 0 : Math.round(puntaje * 20f / totalPreguntas);
    }

    public int getIdResultado() {
        return idResultado;
    }

    public void setIdResultado(int idResultado) {
        this.idResultado = idResultado;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdCuestionario() {
        return idCuestionario;
    }

    public void setIdCuestionario(int idCuestionario) {
        this.idCuestionario = idCuestionario;
    }

    public String getTituloCuestionario() {
        return tituloCuestionario;
    }

    public void setTituloCuestionario(String tituloCuestionario) {
        this.tituloCuestionario = tituloCuestionario;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }

    public int getTotalPreguntas() {
        return totalPreguntas;
    }

    public void setTotalPreguntas(int totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
