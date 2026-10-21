<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="com.mycompany.finanz.util.Fraseutil" %>

<%
    String frase = Fraseutil.obtenerAleatoria();
%>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <title>FinanZ - Iniciar sesión</title>
        
        <link href="css/Logincss.css" rel="stylesheet" type="text/css"/>

    </head>
    <body>

        <div class="login-container">

            <!-- Panel izquierdo -->
            <div class="panel-izquierdo">
                <div class="logo">
                    <span class="logo-icono">🐾</span>
                    <span class="logo-texto">FinanZ</span>
                </div>

                <div class="ilustracion">
                    <img src="image/gatito_finanz.png" alt="Gato mascota FinanZ" class="gato-img">
                </div>

                <p class="frase">
                    "<%= frase %>"
                </p>
            </div>

            <!-- Panel derecho -->
            <div class="panel-derecho">
                <div class="form-wrapper">
                    <h1>¡Bienvenido de nuevo!</h1>

                    <% if (request.getAttribute("error") != null) { %>
                    <p style="color: #d32f2f; text-align: center; margin-bottom: 16px;">
                        <%= request.getAttribute("error") %>
                    </p>
                    <% } %>

                    <form action="LoginServlet" method="post" class="login-form">

                        <div class="campo">
                            <input type="email" name="correo" placeholder="Correo electrónico" required>
                        </div>

                        <div class="campo campo-password">
                            <input type="password" name="contrasena" id="contrasena" placeholder="Contraseña" required>
                            <span class="icono-ojo" id="toggle-password">👁</span>
                        </div>

                        <button type="submit" class="btn-login">Iniciar sesión</button>

                        <div class="enlaces">
                            <a href="#" class="link-recordar">Recordar contraseña</a>
                            <p class="link-registro">
                                ¿Todavía no tienes una cuenta? <a href="registro.jsp">Regístrate</a>
                            </p>
                        </div>

                    </form>
                </div>
            </div>

        </div>

    </body>
</html>
