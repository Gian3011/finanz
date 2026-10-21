package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.servicio.ReporteServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.YearMonth;

/**
 * /reportes      -> sección para elegir el mes e historial de reportes
 * /reportes/pdf  -> descarga el resumen financiero del mes en PDF
 */
@WebServlet(name = "ReporteServlet", urlPatterns = {"/reportes", "/reportes/pdf"})
public class ReporteServlet extends BaseServlet {

    private final ReporteServicio reporteServicio = new ReporteServicio();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {

        if (request.getServletPath().endsWith("/pdf")) {
            YearMonth mes = Validar.mes(request.getParameter("mes"));
            if (mes.isAfter(YearMonth.now())) {
                throw new ValidacionException("No se puede generar el reporte de un mes futuro.");
            }
            byte[] pdf = reporteServicio.generarPdf(usuarioDAO.buscarPorId(idUsuario), mes);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"FinanZ-resumen-" + mes + ".pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
            return;
        }

        request.setAttribute("historial", reporteServicio.historial(idUsuario));
        vista(request, response, "reportes");
    }
}
