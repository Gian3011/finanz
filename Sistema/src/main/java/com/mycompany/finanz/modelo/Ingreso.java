package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Ingreso extends Movimiento {

    public Ingreso() {
        super();
        this.tipoMovimiento = "INGRESO";
    }

    public Ingreso(int idUsuario, int idCategoria, BigDecimal monto, LocalDate fecha, String descripcion) {
        super(idUsuario, idCategoria, monto, fecha, descripcion);
        this.tipoMovimiento = "INGRESO";
    }

    @Override
    public BigDecimal aplicarABalance() {
        return monto; // un ingreso suma al balance
    }
}
