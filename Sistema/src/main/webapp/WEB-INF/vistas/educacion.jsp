<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.util.*"%>
<%
    List<ContenidoEducativo> contenidos = (List<ContenidoEducativo>) request.getAttribute("contenidos");
    List<ResultadoCuestionario> resultados = (List<ResultadoCuestionario>) request.getAttribute("resultados");
    Set<Integer> rendidos = new HashSet<>();
    for (ResultadoCuestionario r : resultados) rendidos.add(r.getIdCuestionario());
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Educación financiera</h2>
    <p class="mt-1 muted">Lecciones cortas y cuestionarios para tomar mejores decisiones con tu dinero.</p>
</div>

<div class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
    <% for (ContenidoEducativo c : contenidos) { %>
    <article class="surface flex flex-col rounded-2xl p-5">
        <span class="chip chip-ok w-fit"><%= Html.esc(c.getTema()) %></span>
        <h3 class="mt-3 text-lg font-bold"><%= Html.esc(c.getTitulo()) %></h3>
        <p class="mt-2 flex-1 text-sm muted"><%= Html.esc(c.getDescripcion()) %></p>
        <div class="mt-4 flex items-center justify-between">
            <a href="#educacion?leccion=<%= c.getIdContenido() %>" class="btn btn-primario"><i data-lucide="book-open"></i>Leer lección</a>
            <% if (c.getIdCuestionario() != null) { %>
            <span class="text-xs font-semibold <%= rendidos.contains(c.getIdCuestionario()) ? "texto-ingreso" : "muted" %>">
                <%= rendidos.contains(c.getIdCuestionario()) ? "✓ Quiz rendido" : "Incluye quiz" %>
            </span>
            <% } %>
        </div>
    </article>
    <% } %>
</div>

<section class="surface mt-6 rounded-2xl p-5" aria-labelledby="titulo-resultados">
    <h3 id="titulo-resultados" class="mb-3 text-lg font-bold">Mis resultados</h3>
    <% if (resultados.isEmpty()) { %>
        <p class="text-sm muted">Todavía no rendiste ningún cuestionario. ¡Elige una lección y ponte a prueba!</p>
    <% } else { %>
    <div class="overflow-x-auto">
        <table class="tabla">
            <thead><tr><th>Cuestionario</th><th>Fecha</th><th class="num">Aciertos</th><th class="num">Nota (0-20)</th></tr></thead>
            <tbody>
            <% for (ResultadoCuestionario r : resultados) { %>
                <tr>
                    <td class="font-semibold"><%= Html.esc(r.getTituloCuestionario()) %></td>
                    <td><%= Formato.fechaHora(r.getFecha()) %></td>
                    <td class="num"><%= r.getPuntaje() %> / <%= r.getTotalPreguntas() %></td>
                    <td class="num font-bold" style="color:<%= r.getNotaVigesimal() >= 13 ? "var(--forest)" : "var(--coral)" %>"><%= r.getNotaVigesimal() %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <% } %>
</section>
