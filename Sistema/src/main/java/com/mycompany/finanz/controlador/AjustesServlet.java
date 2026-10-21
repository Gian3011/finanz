package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.servicio.UsuarioServicio;
import com.mycompany.finanz.servicio.ValidacionException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Ajustes de la cuenta: perfil, contraseña y tema visual. */
@WebServlet(name = "AjustesServlet", urlPatterns = {"/ajustes"})
public class AjustesServlet extends BaseServlet {

    private final UsuarioServicio usuarioServicio = new UsuarioServicio(new UsuarioDAO());

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ServletException, IOException {
        request.setAttribute("usuario", usuarioServicio.buscar(idUsuario));
        vista(request, response, "ajustes");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException {
        if ("perfil".equals(request.getParameter("accion"))) {
            usuarioServicio.actualizarPerfil(idUsuario, request.getParameter("nombres"), request.getParameter("apellidos"));
            request.getSession().setAttribute("nombreUsuario", request.getParameter("nombres").trim());
            exito(request, "Perfil actualizado.");
        } else if ("contrasena".equals(request.getParameter("accion"))) {
            usuarioServicio.cambiarContrasena(idUsuario, request.getParameter("actual"),
                    request.getParameter("nueva"), request.getParameter("confirmacion"));
            exito(request, "Contraseña actualizada correctamente.");
        }
    }
}
