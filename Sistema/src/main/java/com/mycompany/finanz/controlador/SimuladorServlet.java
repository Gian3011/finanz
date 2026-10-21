package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.SimulacionAhorroDAO;
import com.mycompany.finanz.modelo.SimulacionAhorro;
import com.mycompany.finanz.modelo.SimulacionAhorro.Frecuencia;
import com.mycompany.finanz.servicio.MetaAhorroServicio;
import com.mycompany.finanz.servicio.SimuladorServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Simulador de escenarios de ahorro:
 *  - modo "plazo": aporto X cada periodo, ¿cuándo llego?
 *  - modo "aporte": quiero llegar en una fecha, ¿cuánto debo aportar?
 */
@WebServlet(name = "SimuladorServlet", urlPatterns = {"/simulador"})
public class SimuladorServlet extends BaseServlet {

    private final SimuladorServicio simuladorServicio = new SimuladorServicio();
    private final SimulacionAhorroDAO simulacionDAO = new SimulacionAhorroDAO();
    private final MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ServletException, IOException {
        request.setAttribute("historial", simulacionDAO.listarRecientes(idUsuario, 8));
        request.setAttribute("metas", metaAhorroServicio.listar(idUsuario));
        vista(request, response, "simulador");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException {
        if ("eliminar".equals(request.getParameter("accion"))) {
            simulacionDAO.eliminar(Validar.entero(request.getParameter("idSimulacion"), "simulación"), idUsuario);
            return;
        }

        LocalDate hoy = LocalDate.now();
        BigDecimal objetivo = Validar.monto(request.getParameter("montoObjetivo"), "monto objetivo");
        BigDecimal inicial = Validar.montoOpcional(request.getParameter("montoInicial"), "monto inicial");
        Frecuencia frecuencia;
        try {
            frecuencia = Frecuencia.valueOf(request.getParameter("frecuencia"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidacionException("Selecciona una frecuencia de aporte válida.");
        }

        BigDecimal aporte;
        if ("aporte".equals(request.getParameter("modo"))) {
            LocalDate fecha = Validar.fecha(request.getParameter("fechaObjetivo"), "fecha objetivo");
            aporte = simuladorServicio.aporteNecesario(objetivo, inicial, fecha, frecuencia, hoy);
            if (aporte.signum() == 0) {
                throw new ValidacionException("Con el monto inicial ya alcanzas el objetivo.");
            }
            request.setAttribute("modoResultado", "aporte");
        } else {
            aporte = Validar.monto(request.getParameter("aportePeriodico"), "aporte periódico");
            request.setAttribute("modoResultado", "plazo");
        }

        SimulacionAhorro simulacion = simuladorServicio.simular(idUsuario, objetivo, inicial, aporte, frecuencia, hoy);
        simulacionDAO.insertar(simulacion);
        request.setAttribute("simulacion", simulacion);
        request.setAttribute("proyeccion", simuladorServicio.proyeccion(simulacion));
        request.setAttribute("etiquetas", simuladorServicio.etiquetasProyeccion(simulacion));
    }
}
