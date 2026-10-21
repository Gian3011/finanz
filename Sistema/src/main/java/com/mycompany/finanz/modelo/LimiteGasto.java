package com.mycompany.finanz.modelo;

import java.math.BigDecimal;

public class LimiteGasto {

    private int idLimite;
    private int idPresupuesto;
    private int idCategoria;
    private String nombreCategoria; // solo lectura (JOIN con categoria)
    private BigDecimal montoLimite;

    public LimiteGasto() {
    }

    public LimiteGasto(int idPresupuesto, int idCategoria, BigDecimal montoLimite) {
        this.idPresupuesto = idPresupuesto;
        this.idCategoria = idCategoria;
        this.montoLimite = montoLimite;
    }

    public int getIdLimite() {
        return idLimite;
    }

    public void setIdLimite(int idLimite) {
        this.idLimite = idLimite;
    }

    public int getIdPresupuesto() {
        return idPresupuesto;
    }

    public void setIdPresupuesto(int idPresupuesto) {
        this.idPresupuesto = idPresupuesto;
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

    public BigDecimal getMontoLimite() {
        return montoLimite;
    }

    public void setMontoLimite(BigDecimal montoLimite) {
        this.montoLimite = montoLimite;
    }
}
