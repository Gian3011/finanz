package com.mycompany.finanz.modelo;

import java.util.ArrayList;
import java.util.List;

public class Pregunta {

    private int idPregunta;
    private int idCuestionario;
    private String enunciado;
    private List<Opcion> opciones = new ArrayList<>();

    public Opcion getOpcionCorrecta() {
        for (Opcion o : opciones) {
            if (o.isEsCorrecta()) {
                return o;
            }
        }
        return null;
    }

    public int getIdPregunta() {
        return idPregunta;
    }

    public void setIdPregunta(int idPregunta) {
        this.idPregunta = idPregunta;
    }

    public int getIdCuestionario() {
        return idCuestionario;
    }

    public void setIdCuestionario(int idCuestionario) {
        this.idCuestionario = idCuestionario;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public List<Opcion> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<Opcion> opciones) {
        this.opciones = opciones;
    }
}
