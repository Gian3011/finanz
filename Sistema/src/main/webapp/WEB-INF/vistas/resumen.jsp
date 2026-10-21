
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Hola, ${sessionScope.nombreUsuario}</h2>
    <p class="mt-1" style="color:var(--muted)">
        Este es tu espacio financiero. Pronto vas a ver aquí un resumen completo de tus ingresos, gastos y metas.
    </p>
</div>

<article class="surface rounded-2xl p-6">
    <p class="text-sm" style="color:var(--muted)">
        Mientras tanto, puedes ir registrando tus movimientos en <strong>Transacciones</strong>
        y definir tu presupuesto del mes en <strong>Presupuestos</strong>.
    </p>
</article>
