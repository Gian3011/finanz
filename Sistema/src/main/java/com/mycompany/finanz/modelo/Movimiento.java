package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Clase abstracta que representa un movimiento financiero.
 * Ingreso y Gasto heredan de esta clase y sobreescriben
 * aplicarABalance() según su propio comportamiento.
 */
public abstract class Movimiento {

    protected int idMovimiento;
    protected int idUsuario;
    protected int idCategoria;
    protected String nombreCategoria; // solo lectura (JOIN con categoria)
    protected BigDecimal monto;
    protected LocalDate fecha;
    protected String descripcion;
    protected String tipoMovimiento; // "INGRESO" o "GASTO"

    public Movimiento() {
    }

    public Movimiento(int idUsuario, int idCategoria, BigDecimal monto, LocalDate fecha, String descripcion) {
        this.idUsuario = idUsuario;
        this.idCategoria = idCategoria;
        this.monto = monto;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    /**
     * Cada subclase define cómo afecta este movimiento al balance del usuario.
     * Un Ingreso suma, un Gasto resta.
     */
    public abstract BigDecimal aplicarABalance();

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
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

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }
}
