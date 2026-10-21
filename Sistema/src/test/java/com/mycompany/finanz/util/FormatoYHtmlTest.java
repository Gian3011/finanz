package com.mycompany.finanz.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormatoYHtmlTest {

    @Test
    void formateaSoles() {
        assertEquals("S/ 1,234.50", Formato.soles(new BigDecimal("1234.5")));
        assertEquals("- S/ 20.00", Formato.soles(new BigDecimal("-20")));
        assertEquals("S/ 0.00", Formato.soles(null));
    }

    @Test
    void nombreDelMesEnEspanol() {
        assertEquals("octubre 2026", Formato.mes(YearMonth.of(2026, 10)));
    }

    @Test
    void porcentajeSinDivisionEntreCero() {
        assertEquals(50, Formato.porcentaje(new BigDecimal("50"), new BigDecimal("100")));
        assertEquals(0, Formato.porcentaje(BigDecimal.TEN, BigDecimal.ZERO));
    }

    @Test
    void escapaHtmlParaEvitarXss() {
        assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;", Html.esc("<script>alert('x')</script>"));
        assertEquals("", Html.esc(null));
    }

    @Test
    void generaJsonValido() {
        assertEquals("[\"Caf\\\"e\",\"Luz\"]", Json.textos(List.of("Caf\"e", "Luz")));
        assertEquals("[1.50,0]", Json.numeros(java.util.Arrays.asList(new BigDecimal("1.50"), null)));
    }
}
