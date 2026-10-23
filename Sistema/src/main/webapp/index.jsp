<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<%
    // Página de inicio: si hay sesión va al dashboard, si no al login.
    // session="false": no crea sesiones vacías para visitantes sin login.
    jakarta.servlet.http.HttpSession sesion = request.getSession(false);
    boolean logueado = sesion != null && sesion.getAttribute("idUsuario") != null;
    response.sendRedirect(logueado ? "dashboard.jsp" : "Login.jsp");
%>
