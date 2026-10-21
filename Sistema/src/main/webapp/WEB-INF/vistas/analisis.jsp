<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.servicio.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.util.*"%>
<%!
    private String celdaVariacion(BigDecimal antes, BigDecimal ahora, boolean subirEsBueno) {
        Integer v = EstadisticaServicio.variacion(antes, ahora);
        if (v == null) {
            return "<td class=\"num muted\">—</td>";
        }
        String color = v == 0 ? "var(--muted)" : ((v > 0) == subirEsBueno ? "var(--forest)" : "var(--coral)");
        return "<td class=\"num font-bold\" style=\"color:" + color + "\">" + (v > 0 ? "+" : "") + v + " %</td>";
    }

    private String icono(String tipo) {
        switch (tipo) {
            case Recomendacion.ALERTA: return "⚠️";
            case Recomendacion.AHORRO: return "🐷";
            case Recomendacion.HABITO: return "🐜";
            case Recomendacion.EDUCACION: return "📚";
            default: return "🏆";
        }
    }
%>
<%
    ResumenPeriodo a = (ResumenPeriodo) request.getAttribute("resumenA");
    ResumenPeriodo b = (ResumenPeriodo) request.getAttribute("resumenB");
    List<Movimiento> hormiga = (List<Movimiento>) request.getAttribute("gastosHormiga");
    List<ResumenPeriodo> meses = (List<ResumenPeriodo>) request.getAttribute("ultimosMeses");
    List<Recomendacion> recomendaciones = (List<Recomendacion>) request.getAttribute("recomendaciones");

    // Categorías presentes en cualquiera de los dos meses
    Set<String> categorias = new LinkedHashSet<>(b.getGastosPorCategoria().keySet());
    categorias.addAll(a.getGastosPorCategoria().keySet());
    List<BigDecimal> valoresA = new ArrayList<>(), valoresB = new ArrayList<>();
    for (String c : categorias) {
        valoresA.add(a.getGastosPorCategoria().getOrDefault(c, BigDecimal.ZERO));
        valoresB.add(b.getGastosPorCategoria().getOrDefault(c, BigDecimal.ZERO));
    }
    String seriesComparacion = "[{\"nombre\":" + Json.texto(Formato.mes(a.getMes())) + ",\"valores\":" + Json.numeros(valoresA)
            + "},{\"nombre\":" + Json.texto(Formato.mes(b.getMes())) + ",\"valores\":" + Json.numeros(valoresB) + "}]";

    List<String> etiquetasMeses = new ArrayList<>();
    List<BigDecimal> balances = new ArrayList<>();
    for (ResumenPeriodo m : meses) {
        etiquetasMeses.add(Formato.mesCorto(m.getMes()));
        balances.add(m.getBalance());
    }
    String seriesBalance = "[{\"nombre\":\"Balance\",\"valores\":" + Json.numeros(balances) + "}]";

    BigDecimal totalHormiga = BigDecimal.ZERO;
    for (Movimiento m : hormiga) totalHormiga = totalHormiga.add(m.getMonto());
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Análisis de tus hábitos</h2>
    <p class="mt-1 muted">Compara periodos, detecta gastos hormiga y revisa tus recomendaciones personalizadas.</p>
</div>

<!-- Recomendaciones -->
<section class="surface mb-6 rounded-2xl p-5" aria-labelledby="titulo-recomendaciones">
    <h3 id="titulo-recomendaciones" class="mb-4 text-lg font-bold">Recomendaciones para ti</h3>
    <div class="grid gap-3 md:grid-cols-2">
        <% for (Recomendacion r : recomendaciones) { %>
        <div class="flex gap-3 rounded-xl p-4" style="background:var(--paper)">
            <span class="text-2xl" aria-hidden="true"><%= icono(r.getTipo()) %></span>
            <div>
                <p class="font-bold"><%= Html.esc(r.getTitulo()) %></p>
                <p class="mt-1 text-sm muted"><%= Html.esc(r.getDescripcion()) %></p>
            </div>
        </div>
        <% } %>
    </div>
</section>

<!-- Comparación de periodos -->
<section class="surface mb-6 rounded-2xl p-5" aria-labelledby="titulo-comparacion">
    <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
        <h3 id="titulo-comparacion" class="text-lg font-bold">Comparar periodos</h3>
        <form action="analisis" method="get" class="flex flex-wrap items-end gap-2">
            <label class="text-xs font-semibold muted">Mes A <input type="month" name="mesA" value="<%= a.getMes() %>" class="campo mt-1"></label>
            <label class="text-xs font-semibold muted">Mes B <input type="month" name="mesB" value="<%= b.getMes() %>" class="campo mt-1"></label>
            <button type="submit" class="btn btn-primario">Comparar</button>
        </form>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
        <div class="overflow-x-auto">
            <table class="tabla">
                <thead><tr><th>Concepto</th><th class="num"><%= Formato.mes(a.getMes()) %></th><th class="num"><%= Formato.mes(b.getMes()) %></th><th class="num">Variación</th></tr></thead>
                <tbody>
                    <tr><td>Ingresos</td><td class="num"><%= Formato.soles(a.getIngresos()) %></td><td class="num"><%= Formato.soles(b.getIngresos()) %></td><%= celdaVariacion(a.getIngresos(), b.getIngresos(), true) %></tr>
                    <tr><td>Gastos</td><td class="num"><%= Formato.soles(a.getGastos()) %></td><td class="num"><%= Formato.soles(b.getGastos()) %></td><%= celdaVariacion(a.getGastos(), b.getGastos(), false) %></tr>
                    <tr><td>Balance</td><td class="num"><%= Formato.soles(a.getBalance()) %></td><td class="num"><%= Formato.soles(b.getBalance()) %></td><%= celdaVariacion(a.getBalance(), b.getBalance(), true) %></tr>
                    <tr><td>Ahorro en metas</td><td class="num"><%= Formato.soles(a.getAhorroMetas()) %></td><td class="num"><%= Formato.soles(b.getAhorroMetas()) %></td><%= celdaVariacion(a.getAhorroMetas(), b.getAhorroMetas(), true) %></tr>
                    <tr><td>Gastos hormiga</td><td class="num"><%= Formato.soles(a.getGastosHormiga()) %></td><td class="num"><%= Formato.soles(b.getGastosHormiga()) %></td><%= celdaVariacion(a.getGastosHormiga(), b.getGastosHormiga(), false) %></tr>
                    <tr><td>% de ingresos conservado</td><td class="num"><%= a.getTasaAhorro() %> %</td><td class="num"><%= b.getTasaAhorro() %> %</td><td class="num muted">—</td></tr>
                </tbody>
            </table>
        </div>
        <div>
            <p class="mb-2 text-sm font-semibold">Gasto por categoría</p>
            <% if (categorias.isEmpty()) { %>
                <p class="py-12 text-center text-sm muted">No hay gastos en los meses elegidos.</p>
            <% } else { %>
            <div class="grafico">
                <canvas data-grafico="barras" role="img" aria-label="Comparación de gastos por categoría entre los dos meses"
                        data-colores="#9db4cf,#2f8fd8"
                        data-etiquetas="<%= Html.esc(Json.textos(categorias)) %>"
                        data-series="<%= Html.esc(seriesComparacion) %>"></canvas>
            </div>
            <% } %>
        </div>
    </div>
</section>

<div class="grid gap-6 xl:grid-cols-2">
    <!-- Gastos hormiga -->
    <section class="surface rounded-2xl p-5" aria-labelledby="titulo-hormiga">
        <h3 id="titulo-hormiga" class="text-lg font-bold">🐜 Gastos hormiga (últimos 3 meses)</h3>
        <p class="mt-1 text-sm muted">Gastos menores a S/ <%= GastoHormigaServicio.UMBRAL_MONTO.intValue() %> que se repiten
            <%= GastoHormigaServicio.UMBRAL_FRECUENCIA %>+ veces en 30 días en la misma categoría, o que marcaste manualmente.</p>
        <div class="mt-4 grid grid-cols-2 gap-3">
            <div class="kpi" style="background:var(--paper)">
                <p class="text-xs font-semibold muted">Total detectado</p>
                <p class="valor texto-gasto"><%= Formato.soles(totalHormiga) %></p>
            </div>
            <div class="kpi" style="background:var(--paper)">
                <p class="text-xs font-semibold muted">Si sigues así, en un año</p>
                <p class="valor texto-gasto"><%= Formato.soles(b.getGastosHormiga().multiply(BigDecimal.valueOf(12))) %></p>
            </div>
        </div>
        <% if (hormiga.isEmpty()) { %>
            <p class="mt-4 text-sm muted">¡Bien! No se detectaron gastos hormiga.</p>
        <% } else { %>
        <ul class="mt-4 max-h-72 space-y-2 overflow-y-auto">
            <% for (Movimiento m : hormiga) { %>
            <li class="flex items-center justify-between rounded-xl px-3 py-2" style="background:var(--paper)">
                <span class="text-sm"><strong><%= Html.esc(m.getDescripcion()) %></strong>
                    <span class="muted"> · <%= Html.esc(m.getNombreCategoria()) %> · <%= Formato.fecha(m.getFecha()) %></span></span>
                <span class="text-sm font-bold texto-gasto"><%= Formato.soles(m.getMonto()) %></span>
            </li>
            <% } %>
        </ul>
        <% } %>
    </section>

    <!-- Evolución del balance -->
    <section class="surface rounded-2xl p-5" aria-labelledby="titulo-balance">
        <h3 id="titulo-balance" class="text-lg font-bold">Evolución del balance mensual</h3>
        <p class="mt-1 text-sm muted">Ingresos menos gastos de los últimos 6 meses.</p>
        <div class="grafico mt-4">
            <canvas data-grafico="linea" role="img" aria-label="Balance mensual de los últimos 6 meses"
                    data-colores="#176b4a"
                    data-etiquetas="<%= Html.esc(Json.textos(etiquetasMeses)) %>"
                    data-series="<%= Html.esc(seriesBalance) %>"></canvas>
        </div>
    </section>
</div>
