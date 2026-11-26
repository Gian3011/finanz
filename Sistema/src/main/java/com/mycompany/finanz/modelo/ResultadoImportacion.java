package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Lo que se encontró al leer un PDF: tipo de documento, gastos detectados y líneas omitidas. */
public class ResultadoImportacion {

    public enum TipoDocumento {
        ESTADO_CUENTA("Estado de cuenta de tarjeta"),
        COMPROBANTE("Comprobante de pago");

        private final String nombre;

        TipoDocumento(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }

    private final TipoDocumento tipo;
    private final List<MovimientoDetectado> movimientos;
    /** Pagos a la tarjeta, abonos y devoluciones: no son gastos y no se importan. */
    private final int omitidos;
    private String nombreArchivo;

    public ResultadoImportacion(TipoDocumento tipo, List<MovimientoDetectado> movimientos, int omitidos) {
        this.tipo = tipo;
        this.movimientos = new ArrayList<>(movimientos);
        this.omitidos = omitidos;
    }

    public BigDecimal totalSeleccionado() {
        BigDecimal total = BigDecimal.ZERO;
        for (MovimientoDetectado m : movimientos) {
            if (m.isSeleccionado() && m.getMonto() != null) {
                total = total.add(m.getMonto());
            }
        }
        return total;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public List<MovimientoDetectado> getMovimientos() {
        return movimientos;
    }

    public int getOmitidos() {
        return omitidos;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
}
