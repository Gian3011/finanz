package com.mycompany.finanz.controlador;

import com.mycompany.finanz.filtro.SeguridadFiltro;
import com.mycompany.finanz.servicio.ValidacionException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base de los controladores de las secciones del dashboard.
 *
 *  - GET  -> mostrar(): carga los datos y reenvía a la vista JSP (fragmento HTML).
 *  - POST -> procesar(): ejecuta la acción del formulario y luego vuelve a mostrar
 *            la sección con el mensaje de éxito o de error.
 *
 * El SeguridadFiltro ya garantiza que hay un usuario logueado.
 */
public abstract class BaseServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());

    protected abstract void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException;

    /** Acción POST por defecto: ninguna. Las secciones con formularios la sobreescriben. */
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            mostrar(request, response, idUsuario(request));
        } catch (ValidacionException e) {
            responderError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error de base de datos en " + getClass().getSimpleName(), e);
            responderError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No se pudo conectar con la base de datos. Intenta nuevamente en unos segundos.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            procesar(request, response, idUsuario(request));
        } catch (ValidacionException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error de base de datos en " + getClass().getSimpleName(), e);
            request.setAttribute("error", "Ocurrió un error al guardar. Intenta nuevamente.");
        }
        if (!response.isCommitted()) {
            doGet(request, response);
        }
    }

    protected int idUsuario(HttpServletRequest request) {
        return (Integer) request.getSession().getAttribute(SeguridadFiltro.ATRIBUTO_USUARIO);
    }

    protected void vista(HttpServletRequest request, HttpServletResponse response, String nombre)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/vistas/" + nombre + ".jsp").forward(request, response);
    }

    protected void exito(HttpServletRequest request, String mensaje) {
        request.setAttribute("exito", mensaje);
    }

    protected void avisos(HttpServletRequest request, List<String> avisos) {
        if (avisos != null && !avisos.isEmpty()) {
            request.setAttribute("avisos", avisos);
        }
    }

    private void responderError(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<p class=\"mensaje-error\">"
                + com.mycompany.finanz.util.Html.esc(mensaje) + "</p>");
    }
}
