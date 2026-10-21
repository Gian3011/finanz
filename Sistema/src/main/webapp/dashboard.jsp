<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinanZ</title>

    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com/3.4.17"></script>

    <!-- Lucide Icons -->
    <script src="https://cdn.jsdelivr.net/npm/lucide@0.263.0/dist/umd/lucide.min.js"></script>

    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Fraunces:opsz,wght@9..144,600;9..144,700&display=swap"
          rel="stylesheet">

    <!-- CSS propio -->
    
    <link href="css/estilos.css" rel="stylesheet" type="text/css"/>
</head>
<body class="app-shell">

    <div class="app-layout">

        <!-- SIDEBAR -->
        <aside class="sidebar app-sidebar">

            <div class="mb-10 flex items-center gap-3 px-3">
                <div class="flex h-10 w-10 items-center justify-center overflow-hidden rounded-2xl bg-white/10">
                    <img src="image/gatito_finanz.png" alt="Logo FinanZ" class="h-full w-full object-contain">
                </div>
                <div>
                    <p class="brand-serif text-lg font-bold leading-none">FinanZ</p>
                    <p class="mt-1 text-xs text-[#a8d2ba]">${sessionScope.nombreUsuario}</p>
                </div>
            </div>

            <nav class="space-y-1">
                <a href="fragmentos/resumen.jsp" data-url="fragmentos/resumen.jsp" data-titulo="Resumen del mes"
                   class="nav-item active focus-ring flex items-center gap-3 rounded-xl px-4 py-3 text-left font-medium">
                    <i data-lucide="layout-dashboard"></i><span>Resumen</span>
                </a>
                <a href="MovimientoServlet" data-url="MovimientoServlet" data-titulo="Transacciones"
                   class="nav-item focus-ring flex items-center gap-3 rounded-xl px-4 py-3 text-left font-medium">
                    <i data-lucide="wallet-cards"></i><span>Transacciones</span>
                </a>
                <a href="PresupuestoServlet" data-url="PresupuestoServlet" data-titulo="Presupuestos"
                   class="nav-item focus-ring flex items-center gap-3 rounded-xl px-4 py-3 text-left font-medium">
                    <i data-lucide="pie-chart"></i><span>Presupuestos</span>
                </a>
                <a href="fragmentos/metas.jsp" data-url="fragmentos/metas.jsp" data-titulo="Metas"
                   class="nav-item focus-ring flex items-center gap-3 rounded-xl px-4 py-3 text-left font-medium">
                    <i data-lucide="target"></i><span>Metas</span>
                </a>
                <a href="fragmentos/ajustes.jsp" data-url="fragmentos/ajustes.jsp" data-titulo="Ajustes"
                   class="nav-item focus-ring flex items-center gap-3 rounded-xl px-4 py-3 text-left font-medium">
                    <i data-lucide="settings-2"></i><span>Ajustes</span>
                </a>
            </nav>

            

            <a href="LogoutServlet" class="mt-4 text-center text-xs font-semibold" style="color:#ffb3a3">Cerrar sesión</a>
        </aside>


        <!-- MAIN -->
        <main class="app-main">

            <header class="flex items-center justify-between px-5 py-5 sm:px-8 lg:px-10">
                <div>
                    <p class="text-xs font-bold uppercase tracking-[.16em]" style="color:var(--forest)">Tu espacio financiero</p>
                    <h1 id="page-heading" class="brand-serif mt-1 text-3xl font-bold sm:text-4xl">Resumen del mes</h1>
                </div>

                <button type="button" class="theme-toggle-btn surface flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold">
                    <i data-lucide="moon"></i><span>Tema</span>
                </button>
            </header>

            <div class="px-5 pb-10 sm:px-8 lg:px-10">
                <div id="contenido">
                    <jsp:include page="fragmentos/resumen.jsp" />
                </div>
            </div>

        </main>

    </div>

    <script src="js/app.js"></script>
</body>
</html>