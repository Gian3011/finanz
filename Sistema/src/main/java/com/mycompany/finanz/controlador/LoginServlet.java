package com.mycompany.finanz.controlador;

import com.mycompany.finanz.util.Redireccion;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.filtro.SeguridadFiltro;
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
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoginServicio loginServicio = new LoginServicio(new UsuarioDAO());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Si alguien entra directo a /login por GET, lo mandamos al formulario
        Redireccion.a(response, "Login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String correo = request.getParameter("correo");
        String contrasena = request.getParameter("contrasena");

        try {
            Usuario usuario = loginServicio.login(correo, contrasena);
            iniciarSesion(request, usuario);
            Redireccion.a(response, "dashboard.jsp");

        } catch (CredencialesInvalidasException | CuentaBloqueadaException e) {
            volverAlLogin(request, response, e.getMessage(), correo);

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error al iniciar sesión", e);
            volverAlLogin(request, response, "Ocurrió un error al conectar con el servidor. Intenta nuevamente.", correo);
        }
    }

    /**
     * Crea una sesión nueva (evita la "fijación de sesión") con los datos del
     * usuario y un token aleatorio para proteger los formularios contra CSRF.
     */
    static void iniciarSesion(HttpServletRequest request, Usuario usuario) {
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SeguridadFiltro.ATRIBUTO_USUARIO, usuario.getIdUsuario());
        session.setAttribute("nombreUsuario", usuario.getNombres());

        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        session.setAttribute(SeguridadFiltro.ATRIBUTO_CSRF, Base64.getUrlEncoder().withoutPadding().encodeToString(token));
    }

    private void volverAlLogin(HttpServletRequest request, HttpServletResponse response, String error, String correo)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("correo", correo);
        request.getRequestDispatcher("Login.jsp").forward(request, response);
    }
}
