package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.Cuestionario;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Opcion;
import com.mycompany.finanz.modelo.Pregunta;
import com.mycompany.finanz.modelo.SimulacionAhorro;
import com.mycompany.finanz.modelo.SimulacionAhorro.Frecuencia;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las reglas de negocio que no dependen de la base de datos. */
class ReglasNegocioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    @Test
    void nivelDeAlertaDelPresupuesto() {
        BigDecimal tope = new BigDecimal("1000");
        assertEquals(PresupuestoServicio.NivelAlerta.NINGUNA, PresupuestoServicio.nivel(new BigDecimal("799.99"), tope));
        assertEquals(PresupuestoServicio.NivelAlerta.CERCA, PresupuestoServicio.nivel(new BigDecimal("800"), tope));
        assertEquals(PresupuestoServicio.NivelAlerta.CERCA, PresupuestoServicio.nivel(new BigDecimal("1000"), tope));
        assertEquals(PresupuestoServicio.NivelAlerta.SUPERADO, PresupuestoServicio.nivel(new BigDecimal("1000.01"), tope));
    }

    @Test
    void soloLosGastosMenoresAVeinteSolesPuedenSerHormiga() {
        assertTrue(GastoHormigaServicio.esMontoPequeno(new BigDecimal("19.99")));
        assertFalse(GastoHormigaServicio.esMontoPequeno(new BigDecimal("20.00")));
    }

    @Test
    void simuladorCalculaPeriodosYFecha() throws ValidacionException {
        SimulacionAhorro s = new SimuladorServicio().simular(1, new BigDecimal("2000"), new BigDecimal("200"),
                new BigDecimal("250"), Frecuencia.MENSUAL, HOY);
        assertEquals(8, s.getNumeroPeriodos()); // 1800 / 250 = 7.2 -> 8 meses
        assertEquals(HOY.plusMonths(8), s.getFechaObjetivo());
    }

    @Test
    void simuladorCalculaAporteNecesario() throws ValidacionException {
        BigDecimal aporte = new SimuladorServicio().aporteNecesario(new BigDecimal("1200"), BigDecimal.ZERO,
                HOY.plusMonths(4), Frecuencia.MENSUAL, HOY);
        assertEquals(new BigDecimal("300.00"), aporte);
    }

    @Test
    void simuladorRechazaFechaSinPeriodos() {
        assertThrows(ValidacionException.class, () -> new SimuladorServicio().aporteNecesario(
                new BigDecimal("1200"), BigDecimal.ZERO, HOY.plusDays(3), Frecuencia.SEMANAL, HOY));
    }

    @Test
    void progresoYAporteMensualDeUnaMeta() {
        MetaAhorro meta = new MetaAhorro(1, "Laptop", new BigDecimal("2800"), HOY.plusMonths(5));
        meta.setMontoAhorrado(new BigDecimal("400"));
        assertEquals(14, com.mycompany.finanz.servicio.MetaAhorroServicio.calcularProgreso(meta));
        assertEquals(new BigDecimal("480.00"), MetaAhorroServicio.aporteMensualNecesario(meta, HOY));

        meta.setMontoAhorrado(new BigDecimal("5000"));
        assertEquals(100, MetaAhorroServicio.calcularProgreso(meta));
        assertEquals(BigDecimal.ZERO, MetaAhorroServicio.aporteMensualNecesario(meta, HOY));
    }

    @Test
    void validaPoliticaDeContrasenas() {
        assertDoesNotThrow(() -> Validar.contrasenaSegura("Finanz2026"));
        assertThrows(ValidacionException.class, () -> Validar.contrasenaSegura("1234"));
        assertThrows(ValidacionException.class, () -> Validar.contrasenaSegura("solotexto"));
        assertThrows(ValidacionException.class, () -> Validar.contrasenaSegura("12345678"));
    }

    @Test
    void validaMontosYCorreos() throws ValidacionException {
        assertEquals(new BigDecimal("1234.50"), Validar.monto("1,234.50", "monto"));
        assertThrows(ValidacionException.class, () -> Validar.monto("-5", "monto"));
        assertThrows(ValidacionException.class, () -> Validar.monto("abc", "monto"));
        assertThrows(ValidacionException.class, () -> Validar.monto("1.234", "monto"));
        assertEquals("juan@correo.com", Validar.email("  Juan@Correo.com "));
        assertThrows(ValidacionException.class, () -> Validar.email("juan@correo"));
    }

    @Test
    void calificaCuestionario() throws ValidacionException {
        Cuestionario c = new Cuestionario();
        c.getPreguntas().add(pregunta(1, 10, 11));
        c.getPreguntas().add(pregunta(2, 20, 21));

        Map<Integer, Integer> respuestas = new HashMap<>();
        respuestas.put(1, 10); // correcta
        respuestas.put(2, 21); // incorrecta
        assertEquals(1, EducacionServicio.contarCorrectas(c, respuestas));

        respuestas.remove(2);
        assertThrows(ValidacionException.class, () -> EducacionServicio.contarCorrectas(c, respuestas));
    }

    private Pregunta pregunta(int id, int idCorrecta, int idIncorrecta) {
        Pregunta p = new Pregunta();
        p.setIdPregunta(id);
        Opcion buena = new Opcion();
        buena.setIdOpcion(idCorrecta);
        buena.setEsCorrecta(true);
        Opcion mala = new Opcion();
        mala.setIdOpcion(idIncorrecta);
        p.getOpciones().add(buena);
        p.getOpciones().add(mala);
        return p;
    }
}
