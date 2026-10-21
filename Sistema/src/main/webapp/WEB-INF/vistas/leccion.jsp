<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.util.*"%>
<%
    ContenidoEducativo contenido = (ContenidoEducativo) request.getAttribute("contenido");
    Cuestionario cuestionario = (Cuestionario) request.getAttribute("cuestionario");
    ResultadoCuestionario resultado = (ResultadoCuestionario) request.getAttribute("resultado");
    Map<Integer, Integer> respuestas = (Map<Integer, Integer>) request.getAttribute("respuestas");
    if (respuestas == null) respuestas = Collections.emptyMap();
    boolean corregido = resultado != null;
%>
<a href="#educacion" class="mb-4 inline-flex items-center gap-1 text-sm font-semibold" style="color:var(--celeste)">← Volver a las lecciones</a>

<article class="surface rounded-2xl p-6 lg:p-8">
    <span class="chip chip-ok"><%= Html.esc(contenido.getTema()) %></span>
    <h2 class="brand-serif mt-3 text-3xl font-bold"><%= Html.esc(contenido.getTitulo()) %></h2>
    <p class="mt-2 muted"><%= Html.esc(contenido.getDescripcion()) %></p>
    <div class="leccion-texto mt-6 max-w-3xl"><%= Html.esc(contenido.getContenido()) %></div>
</article>

<% if (cuestionario != null) { %>
<section class="surface mt-6 rounded-2xl p-6 lg:p-8" aria-labelledby="titulo-quiz">
    <h3 id="titulo-quiz" class="text-xl font-bold"><%= Html.esc(cuestionario.getTitulo()) %></h3>
    <p class="mt-1 text-sm muted"><%= Html.esc(cuestionario.getDescripcion()) %></p>

    <%@include file="_mensajes.jspf"%>

    <% if (corregido) { %>
    <div class="mensaje mt-4 <%= resultado.getNotaVigesimal() >= 13 ? "mensaje-exito" : "mensaje-aviso" %>" role="status">
        Obtuviste <%= resultado.getPuntaje() %> de <%= resultado.getTotalPreguntas() %> respuestas correctas
        (nota <%= resultado.getNotaVigesimal() %>/20). <%= resultado.getNotaVigesimal() >= 13 ? "¡Muy bien!" : "Repasa la lección e inténtalo de nuevo." %>
    </div>
    <% } %>

    <form action="educacion" method="post" class="mt-5 space-y-6">
        <input type="hidden" name="idCuestionario" value="<%= cuestionario.getIdCuestionario() %>">
        <input type="hidden" name="leccion" value="<%= contenido.getIdContenido() %>">
        <% int numero = 1;
           for (Pregunta p : cuestionario.getPreguntas()) {
               Integer elegida = respuestas.get(p.getIdPregunta()); %>
        <fieldset>
            <legend class="mb-2 font-semibold"><%= numero++ %>. <%= Html.esc(p.getEnunciado()) %></legend>
            <div class="grid gap-2 sm:grid-cols-2">
                <% for (Opcion o : p.getOpciones()) {
                    boolean marcada = elegida != null && elegida == o.getIdOpcion();
                    String clase = "";
                    if (corregido && o.isEsCorrecta()) clase = "correcta";
                    else if (corregido && marcada) clase = "incorrecta";
                %>
                <label class="opcion-quiz <%= clase %>">
                    <input type="radio" name="p<%= p.getIdPregunta() %>" value="<%= o.getIdOpcion() %>" required <%= marcada ? "checked" : "" %>>
                    <span class="text-sm"><%= Html.esc(o.getTexto()) %></span>
                </label>
                <% } %>
            </div>
        </fieldset>
        <% } %>
        <button type="submit" class="btn btn-primario"><%= corregido ? "Volver a intentar" : "Enviar respuestas" %></button>
    </form>
</section>
<% } %>
