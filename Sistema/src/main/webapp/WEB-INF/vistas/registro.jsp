<%@page contentType="text/html" pageEncoding="UTF-8" session="false"%>
<%@page import="com.mycompany.finanz.util.Html"%>
<%!
    private String valor(jakarta.servlet.http.HttpServletRequest request, String nombre) {
        return Html.esc(request.getParameter(nombre));
    }
%>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>FinanZ - Crear cuenta</title>
        <link rel="icon" href="image/gatito_finanz.png">
        <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Fraunces:opsz,wght@9..144,700&display=swap" rel="stylesheet">
        <link href="css/Logincss.css?v=2" rel="stylesheet" type="text/css"/>
    </head>
    <body>
        <div class="login-container">

            <div class="panel-izquierdo">
                <div class="logo">
                    <span class="logo-icono">🐾</span>
                    <span class="logo-texto">FinanZ</span>
                </div>
                <div class="ilustracion">
                    <img src="image/gatito_finanz.png" alt="Gato mascota FinanZ" class="gato-img">
                </div>
                <ul class="beneficios">
                    <li>Registra ingresos y gastos por categoría</li>
                    <li>Controla tu presupuesto con alertas</li>
                    <li>Ahorra con metas y simulaciones</li>
                    <li>Aprende con lecciones y cuestionarios</li>
                </ul>
            </div>

            <div class="panel-derecho">
                <div class="form-wrapper">
                    <h1>Crea tu cuenta</h1>
                    <p class="subtitulo">Es gratis y toma menos de un minuto.</p>

                    <% if (request.getAttribute("error") != null) { %>
                    <p class="mensaje-error" role="alert"><%= Html.esc(request.getAttribute("error")) %></p>
                    <% } %>

                    <form action="registro" method="post" class="login-form">
                        <div class="fila-doble">
                            <div class="campo">
                                <label for="nombres">Nombres</label>
                                <input type="text" id="nombres" name="nombres" required maxlength="100" autocomplete="given-name"
                                       value="<%= valor(request, "nombres") %>">
                            </div>
                            <div class="campo">
                                <label for="apellidos">Apellidos</label>
                                <input type="text" id="apellidos" name="apellidos" required maxlength="100" autocomplete="family-name"
                                       value="<%= valor(request, "apellidos") %>">
                            </div>
                        </div>

                        <div class="campo">
                            <label for="correo">Correo electrónico</label>
                            <input type="email" id="correo" name="correo" required maxlength="150" autocomplete="email"
                                   value="<%= valor(request, "correo") %>">
                        </div>

                        <div class="campo campo-password">
                            <label for="contrasena">Contraseña</label>
                            <input type="password" id="contrasena" name="contrasena" required minlength="8" maxlength="100"
                                   autocomplete="new-password" pattern="(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\d).{8,}"
                                   title="Mínimo 8 caracteres, con letras y números">
                            <button type="button" class="icono-ojo" data-toggle-password="contrasena" aria-label="Mostrar contraseña">👁</button>
                        </div>
                        <p class="ayuda">Mínimo 8 caracteres, con letras y números.</p>

                        <div class="campo">
                            <label for="confirmacion">Confirmar contraseña</label>
                            <input type="password" id="confirmacion" name="confirmacion" required autocomplete="new-password">
                        </div>

                        <button type="submit" class="btn-login">Crear cuenta</button>

                        <div class="enlaces">
                            <p class="link-registro">¿Ya tienes una cuenta? <a href="Login.jsp">Inicia sesión</a></p>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <script>
            document.querySelectorAll('[data-toggle-password]').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    var campo = document.getElementById(boton.getAttribute('data-toggle-password'));
                    campo.type = campo.type === 'password' ? 'text' : 'password';
                });
            });
            document.querySelector('form').addEventListener('submit', function (e) {
                var c = document.getElementById('contrasena'), k = document.getElementById('confirmacion');
                if (c.value !== k.value) {
                    e.preventDefault();
                    k.setCustomValidity('Las contraseñas no coinciden');
                    k.reportValidity();
                }
            });
            document.getElementById('confirmacion').addEventListener('input', function () { this.setCustomValidity(''); });
        </script>
    </body>
</html>
