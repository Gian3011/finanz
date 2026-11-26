package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.modelo.MovimientoDetectado;
import com.mycompany.finanz.modelo.ResultadoImportacion;
import com.mycompany.finanz.modelo.ResultadoImportacion.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lectura de estados de cuenta y comprobantes (texto ya extraído del PDF) y categorización. */
class ImportacionServicioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 26);

    static final String ESTADO_CUENTA = String.join("\n",
            "BANCO DEMO PERU",
            "ESTADO DE CUENTA - TARJETA DE CREDITO VISA",
            "Titular: JUAN PEREZ          Fecha de corte: 20/11/2026",
            "Fecha consumo  Fecha proceso  Descripción                     Importe",
            "21/10  22/10  WONG BENAVIDES LIMA PE                 S/ 245.90",
            "23OCT  24OCT  UBER *TRIP HELP.UBER.COM                 18.40",
            "25/10  25/10  NETFLIX.COM                               44.90",
            "28/10  29/10  PAGO RECIBIDO - GRACIAS                  -800.00",
            "02/11  03/11  STARBUCKS LARCOMAR                        14.50",
            "05/11  06/11  INKAFARMA MIRAFLORES                  1,120.00",
            "08/11  08/11  AMAZON.COM                            US$ 35.00",
            "10/11  11/11  DEVOLUCION TIENDA XYZ                  59.90 CR",
            "12/11  12/11  FERRETERIA EL PERNO                   87.00",
            "TOTAL CONSUMOS DEL PERIODO                          1,525.70",
            "PAGO MINIMO                                           120.00");

    static final String BOLETA = String.join("\n",
            "CINEPLANET S.A.C.",
            "RUC: 20429683581",
            "Av. Larco 1234 - Miraflores",
            "BOLETA DE VENTA ELECTRÓNICA",
            "B015 - 00045871",
            "Fecha de emisión: 14/11/2026   Hora: 19:45",
            "Cliente: JUAN PEREZ  DNI 45678912",
            "2 UND ENTRADA 2D GENERAL       18.00   36.00",
            "1 UND COMBO CANCHITA           24.50   24.50",
            "OP. GRAVADA                            51.69",
            "IGV 18%                                 9.31",
            "IMPORTE TOTAL  S/                      60.50",
            "Representación impresa de la Boleta de Venta Electrónica");

    @Test
    void leeLosConsumosDelEstadoDeCuentaYOmitePagosYAbonos() throws ValidacionException {
        ResultadoImportacion r = ImportacionServicio.interpretar(ESTADO_CUENTA, HOY);

        assertEquals(TipoDocumento.ESTADO_CUENTA, r.getTipo());
        List<MovimientoDetectado> m = r.getMovimientos();
        assertEquals(7, m.size(), "WONG, UBER, NETFLIX, STARBUCKS, INKAFARMA, AMAZON y FERRETERIA");
        assertEquals(2, r.getOmitidos(), "el pago a la tarjeta y la devolución no son gastos");

        assertEquals(LocalDate.of(2026, 10, 21), m.get(0).getFecha());
        assertEquals("WONG BENAVIDES LIMA PE", m.get(0).getDescripcion());
        assertEquals(new BigDecimal("245.90"), m.get(0).getMonto());
        assertEquals(LocalDate.of(2026, 10, 23), m.get(1).getFecha(), "fecha con mes en letras (23OCT)");
        assertEquals(new BigDecimal("1120.00"), m.get(4).getMonto(), "separador de miles");
        assertTrue(m.get(5).enDolares());
        assertFalse(m.get(0).enDolares());

        assertEquals("Alimentación", m.get(0).getCategoriaSugerida());
        assertEquals("Transporte", m.get(1).getCategoriaSugerida());
        assertEquals("Suscripciones", m.get(2).getCategoriaSugerida());
        assertEquals("Snacks y café", m.get(3).getCategoriaSugerida());
        assertEquals("Salud", m.get(4).getCategoriaSugerida());
        assertEquals("Vivienda", m.get(6).getCategoriaSugerida());
    }

    @Test
    void sinAnioUsaElDeLaFechaDeCorte() {
        LocalDate corte = LocalDate.of(2026, 1, 20);
        assertEquals(LocalDate.of(2025, 12, 28), ImportacionServicio.fecha("28/12", corte), "diciembre en el estado de enero");
        assertEquals(LocalDate.of(2026, 1, 5), ImportacionServicio.fecha("05ENE", corte));
        assertEquals(LocalDate.of(2026, 9, 15), ImportacionServicio.fecha("15-Set-26", corte));
        assertEquals(LocalDate.of(2026, 3, 4), ImportacionServicio.fecha("04/03/2026", corte));
        assertNull(ImportacionServicio.fecha("10 UND", corte), "no es una fecha");
        assertNull(ImportacionServicio.fecha("31/02", corte));
    }

    @Test
    void entiendeImportesConComaODecimalesEuropeos() {
        assertEquals(new BigDecimal("1234.56"), ImportacionServicio.monto("1,234.56"));
        assertEquals(new BigDecimal("1234.56"), ImportacionServicio.monto("1.234,56"));
        assertEquals(new BigDecimal("45.90"), ImportacionServicio.monto("S/ 45.90"));
        assertEquals(new BigDecimal("12.00"), ImportacionServicio.monto("US$ 12,00"));
    }

    @Test
    void leeUnaBoletaElectronicaComoUnSoloGasto() throws ValidacionException {
        ResultadoImportacion r = ImportacionServicio.interpretar(BOLETA, HOY);

        assertEquals(TipoDocumento.COMPROBANTE, r.getTipo());
        assertEquals(1, r.getMovimientos().size());
        MovimientoDetectado g = r.getMovimientos().get(0);
        assertEquals(new BigDecimal("60.50"), g.getMonto(), "importe total, no la base gravada ni el IGV");
        assertEquals(LocalDate.of(2026, 11, 14), g.getFecha());
        assertEquals("Boleta B015-45871 · CINEPLANET S.A.C.", g.getDescripcion());
        assertEquals("Entretenimiento", g.getCategoriaSugerida());
    }

    @Test
    void leeUnaFacturaConElTotalEnLaLineaSiguienteYFechaIso() throws ValidacionException {
        String factura = String.join("\n",
                "LUZ DEL SUR S.A.A.",
                "R.U.C. 20331898008",
                "FACTURA ELECTRÓNICA F001-123",
                "Fecha de Emisión: 2026-11-03",
                "Consumo de energía octubre",
                "Total a pagar",
                "S/ 132.40");
        MovimientoDetectado g = ImportacionServicio.interpretar(factura, HOY).getMovimientos().get(0);

        assertEquals(new BigDecimal("132.40"), g.getMonto());
        assertEquals(LocalDate.of(2026, 11, 3), g.getFecha());
        assertEquals("Factura F001-123 · LUZ DEL SUR S.A.A.", g.getDescripcion());
        assertEquals("Servicios", g.getCategoriaSugerida());
    }

    @Test
    void reconoceAlEmisorAunqueElTituloQuedeEnLaMismaLinea() {
        assertEquals("BOTICAS SALUD TOTAL S.A.C.", ImportacionServicio.emisor(List.of(
                "BOTICAS SALUD TOTAL S.A.C. BOLETA DE VENTA ELECTRÓNICA", "RUC: 20999999991 B015 - 00045871")));
        assertEquals("Bodega La Esquina", ImportacionServicio.emisor(List.of(
                "Bodega La Esquina BOLETA DE VENTA ELECTRÓNICA", "RUC 10456789123")));
        assertEquals("CAFE AROMA", ImportacionServicio.emisor(List.of(
                "BOLETA DE VENTA ELECTRÓNICA", "Cant. Descripción Importe", "CAFE AROMA")));
    }

    @Test
    void avisaCuandoElPdfNoTieneConsumos() {
        ValidacionException sinNada = assertThrows(ValidacionException.class,
                () -> ImportacionServicio.interpretar("Hola, este es un documento cualquiera.", HOY));
        assertTrue(sinNada.getMessage().startsWith("No encontramos consumos"));

        ValidacionException soloPagos = assertThrows(ValidacionException.class, () -> ImportacionServicio.interpretar(
                "ESTADO DE CUENTA\n05/11 05/11 PAGO RECIBIDO - GRACIAS 500.00-", HOY));
        assertTrue(soloPagos.getMessage().contains("solo tiene pagos"));
    }

    @Test
    void sugiereCategoriasPorComercio() {
        assertEquals("Alimentación", CategorizadorGastos.sugerir("PLAZA VEA SAN ISIDRO"));
        assertEquals("Transporte", CategorizadorGastos.sugerir("PRIMAX GRIFO 125"));
        assertEquals("Transporte", CategorizadorGastos.sugerir("Metropolitano recarga"), "no lo confunde con METRO");
        assertEquals("Servicios", CategorizadorGastos.sugerir("Movistar recibo"));
        assertEquals("Educación", CategorizadorGastos.sugerir("UTP pension"));
        assertEquals("Suscripciones", CategorizadorGastos.sugerir("SPOTIFY P2A5B"));
        assertEquals("Salud", CategorizadorGastos.sugerir("Clínica Ricardo Palma"));
        assertEquals(CategorizadorGastos.OTROS, CategorizadorGastos.sugerir("COMERCIO DESCONOCIDO SAC"));
    }

    @Test
    void asignaCategoriaRecordadaYDesmarcaLoQueHayQueRevisar() throws ValidacionException {
        ResultadoImportacion r = ImportacionServicio.interpretar(ESTADO_CUENTA, HOY);

        List<Movimiento> historial = new ArrayList<>();
        // El usuario ya clasificó la ferretería como "Otros gastos" (id 10), no como Vivienda
        Gasto ferreteria = new Gasto(1, 10, new BigDecimal("20.00"), LocalDate.of(2026, 9, 1), "FERRETERIA EL PERNO");
        historial.add(ferreteria);
        // Mismo importe y fecha que el consumo de Netflix: posible duplicado
        historial.add(new Gasto(1, 9, new BigDecimal("44.90"), LocalDate.of(2026, 10, 25), "Netflix"));

        ImportacionServicio.completar(r, categorias(), historial, HOY);
        List<MovimientoDetectado> m = r.getMovimientos();

        assertEquals(1, m.get(0).getIdCategoria(), "Alimentación");
        assertEquals(10, m.get(6).getIdCategoria(), "categoría que el usuario usó antes para ese comercio");
        assertTrue(m.get(0).isSeleccionado());
        assertFalse(m.get(2).isSeleccionado(), "duplicado");
        assertTrue(m.get(2).getObservacion().startsWith("Posible duplicado"));
        assertFalse(m.get(5).isSeleccionado(), "en dólares");
        assertTrue(m.get(5).getObservacion().contains("dólares"));
    }

    @Test
    void desmarcaConsumosFueraDelRangoDeFechasPermitido() throws ValidacionException {
        ResultadoImportacion r = ImportacionServicio.interpretar(
                "ESTADO DE CUENTA\n15/12/2024 15/12/2024 WONG SURCO 80.00\n15/01/2025 15/01/2025 TOTTUS 40.00", HOY);
        ImportacionServicio.completar(r, categorias(), List.of(), HOY);

        assertFalse(r.getMovimientos().get(0).isSeleccionado(), "2024 ya no se acepta");
        assertTrue(r.getMovimientos().get(0).getObservacion().contains("2024"));
        assertTrue(r.getMovimientos().get(1).isSeleccionado());
        assertEquals(new BigDecimal("40.00"), r.totalSeleccionado());
    }

    private static List<Categoria> categorias() {
        String[] nombres = {"Alimentación", "Transporte", "Entretenimiento", "Educación", "Salud",
                "Vivienda", "Servicios", "Snacks y café", "Suscripciones", "Otros gastos"};
        List<Categoria> lista = new ArrayList<>();
        for (int i = 0; i < nombres.length; i++) {
            Categoria c = new Categoria(null, nombres[i], "GASTO");
            c.setIdCategoria(i + 1);
            lista.add(c);
        }
        return lista;
    }
}
