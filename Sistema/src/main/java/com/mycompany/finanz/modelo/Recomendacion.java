package com.mycompany.finanz.modelo;

import java.time.LocalDate;

public class Recomendacion {

    /** Tipos de recomendación: definen el color/ícono en la vista. */
    public static final String ALERTA = "ALERTA";
    public static final String AHORRO = "AHORRO";
    public static final String HABITO = "HABITO";
    public static final String EDUCACION = "EDUCACION";
    public static final String LOGRO = "LOGRO";

    private int idRecomendacion;
    private int idUsuario;
    private String titulo;
    private String descripcion;
    private String tipo;
    private LocalDate fecha;

    public Recomendacion() {
    }

    public Recomendacion(int idUsuario, String titulo, String descripcion, String tipo) {
        this.idUsuario = idUsuario;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
    }

    public int getIdRecomendacion() {
        return idRecomendacion;
    }

    public void setIdRecomendacion(int idRecomendacion) {
        this.idRecomendacion = idRecomendacion;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
