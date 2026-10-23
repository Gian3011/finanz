package com.mycompany.finanz.filtro;

import com.mycompany.finanz.util.Redireccion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Filtro que se ejecuta antes de cualquier página o servlet:
 *  1. Fuerza UTF-8 (tildes y ñ correctas en los formularios).
 *  2. Agrega cabeceras de seguridad y de caché.
 *  3. Bloquea el acceso sin sesión iniciada (redirige al login o responde 401 en AJAX).
 *  4. Valida el token CSRF en todas las peticiones POST de usuarios logueados.
 */
@WebFilter(filterName = "SeguridadFiltro", urlPatterns = {"/*"})
public class SeguridadFiltro extends HttpFilter {

    public static final String ATRIBUTO_USUARIO = "idUsuario";
    public static final String ATRIBUTO_CSRF = "csrfToken";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String ruta = request.getRequestURI().substring(request.getContextPath().length());

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "same-origin");

        if (esRecursoEstatico(ruta)) {
            response.setHeader("Cache-Control", "public, max-age=3600");
            chain.doFilter(request, response);
            return;
        }
        // Las páginas con datos personales no se guardan en caché del navegador
        response.setHeader("Cache-Control", "no-store");

        if (esRutaPublica(ruta)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        boolean logueado = session != null && session.getAttribute(ATRIBUTO_USUARIO) != null;
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (!logueado) {
            if (ajax) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                Redireccion.a(response, request.getContextPath() + "/Login.jsp");
            }
            return;
        }

        // Indica a la página qué usuario tiene la sesión: si en el mismo navegador se
        // inició sesión con otra cuenta (otra pestaña), app.js lo detecta y avisa.
        response.setHeader("X-FinanZ-Usuario", String.valueOf(session.getAttribute(ATRIBUTO_USUARIO)));

        if ("POST".equalsIgnoreCase(request.getMethod()) && !tokenCsrfValido(request, session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token de seguridad inválido. Recarga la página.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean esRecursoEstatico(String ruta) {
        return ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/image/")
                || ruta.equals("/favicon.ico");
    }

    private boolean esRutaPublica(String ruta) {
        return ruta.equals("/") || ruta.equals("/index.jsp") || ruta.equals("/Login.jsp")
                || ruta.equals("/login") || ruta.equals("/registro");
    }

    private boolean tokenCsrfValido(HttpServletRequest request, HttpSession session) {
        Object esperado = session.getAttribute(ATRIBUTO_CSRF);
        String recibido = request.getHeader("X-CSRF-Token");
        if (recibido == null) {
            recibido = request.getParameter("_csrf");
        }
        return esperado != null && recibido != null
                && MessageDigest.isEqual(esperado.toString().getBytes(StandardCharsets.UTF_8),
                                         recibido.getBytes(StandardCharsets.UTF_8));
    }
}
