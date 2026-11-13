package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.FormatoReporte;
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
 * /reportes            -> sección para elegir el mes y el formato, e historial de reportes
 * /reportes/descargar  -> descarga el resumen financiero del mes (?mes=AAAA-MM&formato=pdf|xlsx|docx)
 * /reportes/pdf        -> atajo anterior, equivale a formato=pdf
 */
@WebServlet(name = "ReporteServlet", urlPatterns = {"/reportes", "/reportes/descargar", "/reportes/pdf"})
public class ReporteServlet extends BaseServlet {

    private final ReporteServicio reporteServicio = new ReporteServicio();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {

        if (!request.getServletPath().equals("/reportes")) {
            YearMonth mes = Validar.mes(request.getParameter("mes"));
            if (mes.isAfter(YearMonth.now())) {
                throw new ValidacionException("No se puede generar el reporte de un mes futuro.");
            }
            FormatoReporte formato = request.getServletPath().endsWith("/pdf")
                    ? FormatoReporte.PDF
                    : FormatoReporte.desde(request.getParameter("formato"));
            if (formato == null) {
                throw new ValidacionException("Elige un formato válido: PDF, Excel o Word.");
            }

            byte[] archivo = reporteServicio.generar(usuarioDAO.buscarPorId(idUsuario), mes, formato);
            response.setContentType(formato.getTipoContenido());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"FinanZ-resumen-" + mes + "." + formato.getExtension() + "\"");
            response.setContentLength(archivo.length);
            response.getOutputStream().write(archivo);
            return;
        }

        request.setAttribute("historial", reporteServicio.historial(idUsuario));
        vista(request, response, "reportes");
    }
}
