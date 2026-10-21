package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.AlertaDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Notificaciones de presupuesto (campana del encabezado).
 *  GET  /alertas?contador=1 -> número de alertas pendientes (texto plano)
 *  GET  /alertas            -> lista de alertas recientes (fragmento HTML)
 *  POST /alertas            -> marca todas como leídas
 */
@WebServlet(name = "AlertaServlet", urlPatterns = {"/alertas"})
public class AlertaServlet extends BaseServlet {

    private final AlertaDAO alertaDAO = new AlertaDAO();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ServletException, IOException {
        if (request.getParameter("contador") != null) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().print(alertaDAO.contarPendientes(idUsuario));
            return;
        }
        request.setAttribute("alertas", alertaDAO.listarRecientes(idUsuario, 15));
        vista(request, response, "alertas");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException {
        alertaDAO.marcarTodasLeidas(idUsuario);
    }
}
