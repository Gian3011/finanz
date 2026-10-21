package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Gasto extends Movimiento {

    private boolean esGastoHormiga;

    public Gasto() {
        super();
        this.tipoMovimiento = "GASTO";
    }

    public Gasto(int idUsuario, int idCategoria, BigDecimal monto, LocalDate fecha, String descripcion) {
        super(idUsuario, idCategoria, monto, fecha, descripcion);
        this.tipoMovimiento = "GASTO";
    }

    @Override
    public BigDecimal aplicarABalance() {
        return monto.negate(); // un gasto resta del balance
    }

    public boolean isEsGastoHormiga() {
        return esGastoHormiga;
    }

    public void setEsGastoHormiga(boolean esGastoHormiga) {
        this.esGastoHormiga = esGastoHormiga;
    }
}
