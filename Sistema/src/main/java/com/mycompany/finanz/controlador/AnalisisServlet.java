package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.servicio.EstadisticaServicio;
import com.mycompany.finanz.servicio.RecomendacionServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.YearMonth;

/** Análisis de hábitos: comparación entre periodos, gastos hormiga y recomendaciones. */
@WebServlet(name = "AnalisisServlet", urlPatterns = {"/analisis"})
public class AnalisisServlet extends BaseServlet {

    private final EstadisticaServicio estadisticaServicio = new EstadisticaServicio();
    private final RecomendacionServicio recomendacionServicio = new RecomendacionServicio();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {
        YearMonth mesB = Validar.mes(request.getParameter("mesB"));
        String textoA = request.getParameter("mesA");
        YearMonth mesA = textoA == null || textoA.isBlank() ? mesB.minusMonths(1) : Validar.mes(textoA);

        request.setAttribute("resumenA", estadisticaServicio.resumenMes(idUsuario, mesA));
        request.setAttribute("resumenB", estadisticaServicio.resumenMes(idUsuario, mesB));

        YearMonth actual = YearMonth.now();
        request.setAttribute("gastosHormiga", movimientoDAO.listarGastosHormiga(
                idUsuario, actual.minusMonths(2).atDay(1), actual.atEndOfMonth()));
        request.setAttribute("ultimosMeses", estadisticaServicio.ultimosMeses(idUsuario, 6));
        request.setAttribute("recomendaciones", recomendacionServicio.generar(idUsuario));
        vista(request, response, "analisis");
    }
}
