package com.mycompany.finanz.modelo;

public class Categoria {

    private int idCategoria;
    private Integer idUsuario; // null = categoría predeterminada del sistema
    private String nombreCategoria;
    private String tipo; // "INGRESO" o "GASTO"

    public Categoria() {
    }

    public Categoria(Integer idUsuario, String nombreCategoria, String tipo) {
        this.idUsuario = idUsuario;
        this.nombreCategoria = nombreCategoria;
        this.tipo = tipo;
    }

    public boolean esPredeterminada() {
        return idUsuario == null;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
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
