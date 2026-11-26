package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Gasto encontrado en un PDF importado, antes de registrarlo. El usuario lo
 * revisa (puede corregir fecha, descripción, categoría y monto) y confirma.
 */
public class MovimientoDetectado {

    public static final String SOLES = "PEN";
    public static final String DOLARES = "USD";

    private LocalDate fecha;
    private String descripcion;
    private BigDecimal monto;
    private String moneda = SOLES;
    private String categoriaSugerida;
    private int idCategoria;
    private boolean seleccionado = true;
    private String observacion;

    public MovimientoDetectado() {
    }

    public MovimientoDetectado(LocalDate fecha, String descripcion, BigDecimal monto, String moneda) {
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.monto = monto;
        this.moneda = moneda;
    }

    public boolean enDolares() {
        return DOLARES.equals(moneda);
    }

    /** Desmarca el movimiento y explica por qué (el usuario decide si lo registra igual). */
    public void desmarcar(String motivo) {
        seleccionado = false;
        observacion = observacion == null ? motivo : observacion + " " + motivo;
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

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getCategoriaSugerida() {
        return categoriaSugerida;
    }

    public void setCategoriaSugerida(String categoriaSugerida) {
        this.categoriaSugerida = categoriaSugerida;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
