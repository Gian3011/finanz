<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    // Página de inicio: si hay sesión va al dashboard, si no al login.
    boolean logueado = session != null && session.getAttribute("idUsuario") != null;
    response.sendRedirect(logueado ? "dashboard.jsp" : "Login.jsp");
%>
