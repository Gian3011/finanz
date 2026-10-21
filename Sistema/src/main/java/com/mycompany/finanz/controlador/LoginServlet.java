package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.CredencialesInvalidasException;
import com.mycompany.finanz.servicio.CuentaBloqueadaException;
import com.mycompany.finanz.servicio.LoginServicio;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final LoginServicio loginServicio = new LoginServicio(usuarioDAO);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Si alguien entra directo a /LoginServlet por GET, lo mandamos al formulario
        response.sendRedirect("Login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String correo = request.getParameter("correo");
        String contrasena = request.getParameter("contrasena");

        try {
            Usuario usuario = loginServicio.login(correo, contrasena);

            // Login exitoso: crear sesión y guardar datos básicos del usuario
            HttpSession session = request.getSession();
            session.setAttribute("idUsuario", usuario.getIdUsuario());
            session.setAttribute("nombreUsuario", usuario.getNombres());

            response.sendRedirect("dashboard.jsp");

        } catch (CredencialesInvalidasException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("Login.jsp").forward(request, response);

        } catch (CuentaBloqueadaException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("Login.jsp").forward(request, response);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Ocurrió un error al conectar con el servidor. Intenta nuevamente.");
            request.getRequestDispatcher("Login.jsp").forward(request, response);
        }
    }
}
