package com.mycompany.finanz.modelo;

public class Categoria {

    private int idCategoria;
    private String nombreCategoria;
    private String tipo; // por ejemplo: "INGRESO" o "GASTO"

    public Categoria() {
    }

    public Categoria(String nombreCategoria, String tipo) {
        this.nombreCategoria = nombreCategoria;
        this.tipo = tipo;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
