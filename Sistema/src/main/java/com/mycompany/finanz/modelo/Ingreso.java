package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Ingreso extends Movimiento {

    private String fuente; // procedencia del ingreso (empleador, cliente, etc.)

    public Ingreso() {
        super();
        this.tipoMovimiento = "INGRESO";
    }

    public Ingreso(int idUsuario, int idCategoria, BigDecimal monto, LocalDate fecha, String descripcion, String fuente) {
        super(idUsuario, idCategoria, monto, fecha, descripcion);
        this.tipoMovimiento = "INGRESO";
        this.fuente = fuente;
    }

    @Override
    public BigDecimal aplicarABalance() {
        return monto; // un ingreso suma al balance
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }
}
