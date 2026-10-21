package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.UsuarioServicio;
import com.mycompany.finanz.servicio.ValidacionException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "RegistroServlet", urlPatterns = {"/registro"})
public class RegistroServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(RegistroServlet.class.getName());

    private final UsuarioServicio usuarioServicio = new UsuarioServicio(new UsuarioDAO());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/vistas/registro.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Usuario usuario = usuarioServicio.registrar(
                    request.getParameter("nombres"),
                    request.getParameter("apellidos"),
                    request.getParameter("correo"),
                    request.getParameter("contrasena"),
                    request.getParameter("confirmacion"));

            // Cuenta creada: inicia sesión directamente
            LoginServlet.iniciarSesion(request, usuario);
            response.sendRedirect("dashboard.jsp");
            return;

        } catch (ValidacionException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error al registrar usuario", e);
            request.setAttribute("error", "Ocurrió un error al crear la cuenta. Intenta nuevamente.");
        }
        doGet(request, response);
    }
}
