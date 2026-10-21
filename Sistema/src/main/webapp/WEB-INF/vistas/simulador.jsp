<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.util.*"%>
<%!
    private String param(jakarta.servlet.http.HttpServletRequest request, String nombre, String porDefecto) {
        String v = request.getParameter(nombre);
        return Html.esc(v == null ? porDefecto : v);
    }
%>
<%
    List<SimulacionAhorro> historial = (List<SimulacionAhorro>) request.getAttribute("historial");
    List<MetaAhorro> metas = (List<MetaAhorro>) request.getAttribute("metas");
    SimulacionAhorro sim = (SimulacionAhorro) request.getAttribute("simulacion");
    String modo = request.getParameter("modo") == null ? "plazo" : request.getParameter("modo");
    String frecuencia = request.getParameter("frecuencia") == null ? "MENSUAL" : request.getParameter("frecuencia");
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Simulador de ahorro</h2>
    <p class="mt-1 muted">Prueba distintos escenarios antes de comprometerte con una meta.</p>
</div>

<div class="grid gap-6 xl:grid-cols-5">
    <aside class="surface h-fit rounded-2xl p-5 xl:col-span-2">
        <form action="simulador" method="post" class="space-y-4">
            <fieldset>
                <legend class="etiqueta">¿Qué quieres calcular?</legend>
                <div class="grid grid-cols-2 gap-2">
                    <label class="opcion-quiz text-sm"><input type="radio" name="modo" value="plazo" <%= "plazo".equals(modo) ? "checked" : "" %>> ¿Cuándo llego?</label>
                    <label class="opcion-quiz text-sm"><input type="radio" name="modo" value="aporte" <%= "aporte".equals(modo) ? "checked" : "" %>> ¿Cuánto aportar?</label>
                </div>
            </fieldset>

            <% if (!metas.isEmpty()) { %>
            <p class="text-xs muted">Ideas desde tus metas:
                <% for (MetaAhorro m : metas) { if (m.isCumplida()) continue; %>
                <span class="chip chip-ok"><%= Html.esc(m.getNombreMeta()) %>: faltan <%= Formato.soles(m.getMontoFaltante()) %></span>
                <% } %>
            </p>
            <% } %>

            <div>
                <label class="etiqueta" for="montoObjetivo-sim">Monto objetivo (S/)</label>
                <input type="number" id="montoObjetivo-sim" name="montoObjetivo" step="0.01" min="0.01" required class="campo"
                       value="<%= param(request, "montoObjetivo", "") %>">
            </div>
            <div>
                <label class="etiqueta" for="montoInicial">Ya tengo ahorrado (S/)</label>
                <input type="number" id="montoInicial" name="montoInicial" step="0.01" min="0" class="campo"
                       value="<%= param(request, "montoInicial", "0") %>">
            </div>
            <div>
                <label class="etiqueta" for="frecuencia">Frecuencia de aporte</label>
                <select id="frecuencia" name="frecuencia" class="campo">
                    <% for (SimulacionAhorro.Frecuencia f : SimulacionAhorro.Frecuencia.values()) { %>
                    <option value="<%= f.name() %>" <%= f.name().equals(frecuencia) ? "selected" : "" %>><%= f.name().charAt(0) + f.name().substring(1).toLowerCase() %></option>
                    <% } %>
                </select>
            </div>
            <div data-modo="plazo">
                <label class="etiqueta" for="aportePeriodico">Aporte por periodo (S/)</label>
                <input type="number" id="aportePeriodico" name="aportePeriodico" step="0.01" min="0.01" required class="campo"
                       value="<%= param(request, "aportePeriodico", "") %>">
            </div>
            <div data-modo="aporte" class="hidden">
                <label class="etiqueta" for="fechaObjetivo-sim">Quiero lograrlo para el</label>
                <input type="date" id="fechaObjetivo-sim" name="fechaObjetivo" min="<%= LocalDate.now().plusDays(7) %>" required class="campo"
                       value="<%= param(request, "fechaObjetivo", "") %>">
            </div>
            <button type="submit" class="btn btn-primario w-full"><i data-lucide="calculator"></i>Simular</button>
        </form>
    </aside>

    <section class="space-y-6 xl:col-span-3">
        <% if (sim != null) {
            List<BigDecimal> proyeccion = (List<BigDecimal>) request.getAttribute("proyeccion");
            List<Integer> etiquetasNum = (List<Integer>) request.getAttribute("etiquetas");
            List<String> etiquetas = new ArrayList<>();
            for (Integer n : etiquetasNum) etiquetas.add(n + "");
            BigDecimal totalAportado = sim.getAportePeriodico().multiply(BigDecimal.valueOf(sim.getNumeroPeriodos()));
            String series = "[{\"nombre\":\"Ahorro acumulado\",\"valores\":" + Json.numeros(proyeccion) + "}]";
            boolean modoAporte = "aporte".equals(request.getAttribute("modoResultado"));
        %>
        <article class="surface rounded-2xl p-5">
            <h3 class="text-lg font-bold">Resultado</h3>
            <div class="mt-4 grid gap-4 sm:grid-cols-3">
                <div class="kpi" style="background:var(--paper)">
                    <p class="text-xs font-semibold muted"><%= modoAporte ? "Debes aportar" : "Aporte" %></p>
                    <p class="valor" style="color:var(--celeste)"><%= Formato.soles(sim.getAportePeriodico()) %></p>
                    <p class="text-xs muted">por <%= sim.getFrecuencia().getSingular() %></p>
                </div>
                <div class="kpi" style="background:var(--paper)">
                    <p class="text-xs font-semibold muted">Tiempo necesario</p>
                    <p class="valor"><%= sim.getNumeroPeriodos() %></p>
                    <p class="text-xs muted"><%= sim.getNumeroPeriodos() == 1 ? sim.getFrecuencia().getSingular() : sim.getFrecuencia().getPlural() %></p>
                </div>
                <div class="kpi" style="background:var(--paper)">
                    <p class="text-xs font-semibold muted">Llegarías el</p>
                    <p class="valor" style="color:var(--forest)"><%= Formato.fecha(sim.getFechaObjetivo()) %></p>
                    <p class="text-xs muted">aportando <%= Formato.soles(totalAportado) %> en total</p>
                </div>
            </div>
            <div class="grafico mt-5">
                <canvas data-grafico="linea" role="img" aria-label="Proyección del ahorro acumulado"
                        data-colores="#2f8fd8"
                        data-etiquetas="<%= Html.esc(Json.textos(etiquetas)) %>"
                        data-series="<%= Html.esc(series) %>"></canvas>
            </div>
            <p class="mt-2 text-center text-xs muted">Eje horizontal: número de <%= sim.getFrecuencia().getPlural() %> desde hoy.</p>
            <a href="#metas" class="btn btn-verde mt-4">Convertir en meta de ahorro</a>
        </article>
        <% } else { %>
        <article class="surface rounded-2xl p-10 text-center">
            <p class="text-4xl">🧮</p>
            <p class="mt-3 font-semibold">Completa el formulario para ver tu proyección.</p>
            <p class="mt-1 text-sm muted">Por ejemplo: S/ 2 000 aportando S/ 250 al mes.</p>
        </article>
        <% } %>

        <article class="surface rounded-2xl p-5">
            <h3 class="mb-3 text-lg font-bold">Simulaciones recientes</h3>
            <% if (historial.isEmpty()) { %>
                <p class="text-sm muted">Aún no tienes simulaciones guardadas.</p>
            <% } else { %>
            <div class="overflow-x-auto">
                <table class="tabla">
                    <thead><tr><th>Objetivo</th><th class="num">Aporte</th><th>Frecuencia</th><th>Llegada</th><th></th></tr></thead>
                    <tbody>
                    <% for (SimulacionAhorro s : historial) { %>
                        <tr>
                            <td class="font-semibold"><%= Formato.soles(s.getMontoObjetivo()) %></td>
                            <td class="num"><%= Formato.soles(s.getAportePeriodico()) %></td>
                            <td><%= s.getNumeroPeriodos() %> <%= s.getFrecuencia().getPlural() %></td>
                            <td><%= Formato.fecha(s.getFechaObjetivo()) %></td>
                            <td class="num">
                                <form action="simulador" method="post">
                                    <input type="hidden" name="accion" value="eliminar">
                                    <input type="hidden" name="idSimulacion" value="<%= s.getIdSimulacion() %>">
                                    <button type="submit" class="btn btn-icono" aria-label="Eliminar simulación"><i data-lucide="x"></i></button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <% } %>
        </article>
    </section>
</div>
