<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.Usuario"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%
    Usuario usuario = (Usuario) request.getAttribute("usuario");
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Ajustes</h2>
    <p class="mt-1 muted">Administra tu perfil, tu contraseña y la apariencia de FinanZ.</p>
</div>

<div class="grid max-w-5xl gap-6 lg:grid-cols-2">
    <article class="surface rounded-2xl p-6">
        <h3 class="text-lg font-bold">Perfil</h3>
        <p class="mt-1 text-sm muted">Cuenta creada el <%= Formato.fechaHora(usuario.getFechaRegistro()) %></p>
        <form action="ajustes" method="post" class="mt-5 space-y-4">
            <input type="hidden" name="accion" value="perfil">
            <div>
                <label class="etiqueta" for="nombres">Nombres</label>
                <input type="text" id="nombres" name="nombres" required maxlength="100" class="campo" value="<%= Html.esc(usuario.getNombres()) %>">
            </div>
            <div>
                <label class="etiqueta" for="apellidos">Apellidos</label>
                <input type="text" id="apellidos" name="apellidos" required maxlength="100" class="campo" value="<%= Html.esc(usuario.getApellidos()) %>">
            </div>
            <div>
                <label class="etiqueta" for="correo-perfil">Correo electrónico</label>
                <input type="email" id="correo-perfil" class="campo" value="<%= Html.esc(usuario.getEmail()) %>" disabled>
            </div>
            <button type="submit" class="btn btn-primario">Guardar cambios</button>
        </form>
    </article>

    <article class="surface rounded-2xl p-6">
        <h3 class="text-lg font-bold">Cambiar contraseña</h3>
        <p class="mt-1 text-sm muted">Tu contraseña se guarda cifrada (PBKDF2 con SHA-256).</p>
        <form action="ajustes" method="post" class="mt-5 space-y-4">
            <input type="hidden" name="accion" value="contrasena">
            <div>
                <label class="etiqueta" for="actual">Contraseña actual</label>
                <input type="password" id="actual" name="actual" required autocomplete="current-password" class="campo">
            </div>
            <div>
                <label class="etiqueta" for="nueva">Nueva contraseña</label>
                <input type="password" id="nueva" name="nueva" required minlength="8" autocomplete="new-password" class="campo">
                <p class="mt-1 text-xs muted">Mínimo 8 caracteres, con letras y números.</p>
            </div>
            <div>
                <label class="etiqueta" for="confirmacion">Confirmar nueva contraseña</label>
                <input type="password" id="confirmacion" name="confirmacion" required autocomplete="new-password" class="campo">
            </div>
            <button type="submit" class="btn btn-verde">Actualizar contraseña</button>
        </form>
    </article>

    <article class="surface rounded-2xl p-6 lg:col-span-2">
        <div class="flex flex-wrap items-center justify-between gap-5">
            <div>
                <h3 class="text-lg font-bold">Apariencia</h3>
                <p class="mt-1 text-sm muted">Alterna entre el tema claro y oscuro. Tu preferencia se recuerda en este navegador.</p>
            </div>
            <button type="button" class="theme-toggle-btn btn btn-suave"><i data-lucide="moon"></i>Cambiar tema</button>
        </div>
    </article>
</div>
