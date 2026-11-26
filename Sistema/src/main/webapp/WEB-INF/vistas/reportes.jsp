<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.EnvioReporte"%>
<%@page import="com.mycompany.finanz.modelo.FormatoReporte"%>
<%@page import="com.mycompany.finanz.modelo.ReporteFinanciero"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.time.YearMonth"%>
<%@page import="java.util.List"%>
<%
    List<ReporteFinanciero> historial = (List<ReporteFinanciero>) request.getAttribute("historial");
    List<EnvioReporte> envios = (List<EnvioReporte>) request.getAttribute("envios");
    String correoCuenta = (String) request.getAttribute("correoCuenta");
    YearMonth actual = YearMonth.now();
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Reportes</h2>
    <p class="mt-1 muted">Exporta el resumen financiero de un mes en PDF, Excel o Word (generado con JasperReports) o envíalo por correo.</p>
</div>

<div class="grid gap-6 lg:grid-cols-2">
    <article class="surface rounded-2xl p-6">
        <h3 class="text-lg font-bold">Resumen financiero</h3>
        <p class="mt-1 text-sm muted">Incluye ingresos, gastos, balance, presupuesto, gastos por categoría, comparación con el mes
            anterior, metas, recomendaciones y el detalle de tus movimientos.</p>

        <!-- Descarga directa (no AJAX): el navegador guarda el archivo. El botón pulsado envía el formato. -->
        <form action="reportes/descargar" method="get" class="mt-5" data-nativo>
            <label class="text-xs font-semibold muted">Mes
                <input type="month" id="mesReporte" name="mes" value="<%= actual %>" max="<%= actual %>" required class="campo mt-1">
            </label>
            <div class="mt-4 flex flex-wrap gap-3">
                <button type="submit" name="formato" value="pdf" class="btn btn-primario"><i data-lucide="file-down"></i>PDF</button>
                <button type="submit" name="formato" value="xlsx" class="btn btn-verde"><i data-lucide="file-spreadsheet"></i>Excel</button>
                <button type="submit" name="formato" value="docx" class="btn btn-suave"><i data-lucide="file-text"></i>Word</button>
                <button type="button" class="btn btn-suave" data-abrir-modal="modal-enviar-correo"><i data-lucide="mail"></i>Enviar por correo</button>
            </div>
        </form>
    </article>

    <article class="surface rounded-2xl p-6">
        <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold">Reportes generados</h3>
            <a href="#reportes" data-navegar="reportes" class="text-sm font-semibold" style="color:var(--celeste)">Actualizar</a>
        </div>
        <% if (historial.isEmpty()) { %>
            <p class="mt-4 text-sm muted">Aún no generaste reportes.</p>
        <% } else { %>
        <ul class="mt-4 space-y-2">
            <% for (ReporteFinanciero r : historial) {
                YearMonth mes = YearMonth.parse(r.getPeriodo());
                FormatoReporte f = FormatoReporte.desde(r.getFormato());
                if (f == null) { f = FormatoReporte.PDF; } %>
            <li class="flex items-center justify-between rounded-xl px-3 py-2" style="background:var(--paper)">
                <span class="text-sm"><strong><%= Formato.mes(mes) %></strong> <span class="muted">· <%= Html.esc(r.getFormato()) %> · <%= Formato.fechaHora(r.getFechaGeneracion()) %></span></span>
                <a class="btn btn-icono" href="reportes/descargar?mes=<%= mes %>&amp;formato=<%= f.getExtension() %>" aria-label="Descargar de nuevo"><i data-lucide="download"></i></a>
            </li>
            <% } %>
        </ul>
        <% } %>
    </article>
</div>

<article class="surface mt-6 rounded-2xl p-6">
    <h3 class="text-lg font-bold">Envíos por correo</h3>
    <p class="mt-1 text-sm muted">Puedes enviar hasta 5 reportes por hora. "Verificar entrega" revisa si el correo rebotó.</p>
    <% if (envios.isEmpty()) { %>
        <p class="mt-4 text-sm muted">Aún no enviaste reportes por correo.</p>
    <% } else { %>
    <ul class="mt-4 space-y-2">
        <% for (EnvioReporte e : envios) {
            FormatoReporte f = FormatoReporte.desde(e.getFormato());
            String chip = EnvioReporte.ENVIADO.equals(e.getEstado()) ? "chip-ok"
                    : EnvioReporte.REBOTADO.equals(e.getEstado()) ? "chip-hormiga" : "chip-alerta"; %>
        <li class="flex flex-wrap items-center justify-between gap-2 rounded-xl px-3 py-2" style="background:var(--paper)">
            <span class="min-w-0 text-sm">
                <strong><%= Formato.mes(YearMonth.from(e.getPeriodo())) %></strong>
                <span class="muted">· <%= Html.esc(f == null ? e.getFormato() : f.getNombre()) %> · <%= Formato.fechaHora(e.getFechaEnvio()) %></span>
                <span class="block break-all muted"><%= Html.esc(e.getCorreoDestino()) %></span>
            </span>
            <span class="flex items-center gap-2">
                <span class="chip <%= chip %>"><%= Html.esc(e.getEstado()) %></span>
                <% if (EnvioReporte.ENVIADO.equals(e.getEstado())) { %>
                <form action="reportes/verificar" method="post">
                    <input type="hidden" name="idEnvio" value="<%= e.getIdEnvio() %>">
                    <button type="submit" class="btn btn-suave text-xs"><i data-lucide="mail-check"></i>Verificar entrega</button>
                </form>
                <% } %>
            </span>
        </li>
        <% } %>
    </ul>
    <% } %>
</article>

<!-- Modal: enviar el reporte por correo (se envía por AJAX y muestra el resultado aquí mismo) -->
<div id="modal-enviar-correo" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="titulo-enviar-correo">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 id="titulo-enviar-correo" class="text-lg font-bold">Enviar por correo</h3>
            <button type="button" class="btn btn-icono" data-cerrar-modal aria-label="Cerrar">✕</button>
        </div>
        <form action="reportes/enviar" method="post" class="space-y-4" data-enviar-en-modal>
            <div class="grid gap-3 sm:grid-cols-2">
                <div>
                    <label class="etiqueta" for="mesEnvio">Mes</label>
                    <input type="month" id="mesEnvio" name="mes" value="<%= actual %>" max="<%= actual %>" required class="campo" data-copiar-de="mesReporte">
                </div>
                <div>
                    <label class="etiqueta" for="formatoEnvio">Formato</label>
                    <select id="formatoEnvio" name="formato" class="campo">
                        <option value="pdf">PDF</option>
                        <option value="xlsx">Excel</option>
                        <option value="docx">Word</option>
                    </select>
                </div>
            </div>
            <div>
                <label class="etiqueta" for="correoDestino">Correo destino</label>
                <input type="email" id="correoDestino" name="correoDestino" required maxlength="150" class="campo"
                       autocomplete="email" value="<%= Html.esc(correoCuenta) %>">
                <p class="mt-1 text-xs muted">Puedes cambiarlo si el correo de tu cuenta no es real.</p>
            </div>
            <div data-resultado aria-live="polite"></div>
            <div class="flex gap-3">
                <button type="button" class="btn btn-suave flex-1" data-cerrar-modal>Cancelar</button>
                <button type="submit" class="btn btn-primario flex-1"><i data-lucide="send"></i>Enviar</button>
            </div>
        </form>
    </div>
</div>
