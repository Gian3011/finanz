<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.Alerta"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.util.List"%>
<%
    List<Alerta> alertas = (List<Alerta>) request.getAttribute("alertas");
%>
<p class="mb-3 font-bold">Alertas de presupuesto</p>
<% if (alertas.isEmpty()) { %>
    <p class="text-sm muted">No tienes alertas. ¡Tu presupuesto va en orden!</p>
<% } else { %>
<ul class="space-y-2">
    <% for (Alerta a : alertas) {
        boolean superado = Alerta.SUPERADO.equals(a.getNivel()); %>
    <li class="rounded-xl p-3 text-sm" style="background:var(--paper);<%= a.isPendiente() ? "border-left:4px solid " + (superado ? "var(--coral)" : "var(--sun)") : "" %>">
        <p class="font-semibold" style="color:<%= superado ? "var(--coral)" : "var(--sun)" %>"><%= superado ? "Límite superado" : "Cerca del límite" %></p>
        <p class="mt-1"><%= Html.esc(a.getMensaje()) %></p>
        <p class="mt-1 text-xs muted"><%= Formato.fechaHora(a.getFecha()) %></p>
    </li>
    <% } %>
</ul>
<a href="#presupuestos" class="btn btn-suave mt-3 w-full">Ver presupuestos</a>
<% } %>
