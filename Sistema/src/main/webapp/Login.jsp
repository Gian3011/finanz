<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<%@page import="com.mycompany.finanz.util.FraseUtil"%>
<%@page import="com.mycompany.finanz.util.Html"%>
<%
    // Si ya hay una sesión activa, no tiene sentido volver a iniciar sesión
    jakarta.servlet.http.HttpSession sesion = request.getSession(false);
    if (sesion != null && sesion.getAttribute("idUsuario") != null) {
        response.sendRedirect("dashboard.jsp");
        return;
    }
    String frase = FraseUtil.obtenerAleatoria();
%>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>FinanZ - Iniciar sesión</title>
        <link rel="icon" href="image/gatito_finanz.png">
        <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Fraunces:opsz,wght@9..144,700&display=swap" rel="stylesheet">
        <link href="css/Logincss.css?v=2" rel="stylesheet" type="text/css"/>
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

                <p class="frase">"<%= Html.esc(frase) %>"</p>
            </div>

            <!-- Panel derecho -->
            <div class="panel-derecho">
                <div class="form-wrapper">
                    <h1>¡Bienvenido de nuevo!</h1>
                    <p class="subtitulo">Organiza tus ingresos, gastos y metas en un solo lugar.</p>

                    <% if (request.getAttribute("error") != null) { %>
                    <p class="mensaje-error" role="alert"><%= Html.esc(request.getAttribute("error")) %></p>
                    <% } else if ("cerrada".equals(request.getParameter("sesion"))) { %>
                    <p class="mensaje-error" role="alert">Tu sesión se cerró o expiró. Vuelve a iniciar sesión.</p>
                    <% } %>

                    <form action="login" method="post" class="login-form">

                        <div class="campo">
                            <label for="correo">Correo electrónico</label>
                            <input type="email" id="correo" name="correo" autocomplete="username" required maxlength="150"
                                   value="<%= Html.esc(request.getAttribute("correo")) %>">
                        </div>

                        <div class="campo campo-password">
                            <label for="contrasena">Contraseña</label>
                            <input type="password" id="contrasena" name="contrasena" autocomplete="current-password" required>
                            <button type="button" class="icono-ojo" data-toggle-password="contrasena" aria-label="Mostrar contraseña">👁</button>
                        </div>

                        <button type="submit" class="btn-login">Iniciar sesión</button>

                        <div class="enlaces">
                            <p class="link-registro">
                                ¿Todavía no tienes una cuenta? <a href="registro">Regístrate</a>
                            </p>
                        </div>
                    </form>

                    <div class="demo">
                        <strong>Cuenta de demostración</strong>
                        <span>juan@correo.com · Finanz2026</span>
                    </div>
                </div>
            </div>

        </div>

        <script>
            // Mostrar / ocultar contraseña
            document.querySelectorAll('[data-toggle-password]').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    var campo = document.getElementById(boton.getAttribute('data-toggle-password'));
                    campo.type = campo.type === 'password' ? 'text' : 'password';
                });
            });
        </script>
    </body>
</html>
