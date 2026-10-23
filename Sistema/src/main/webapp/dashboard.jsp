<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.util.Html"%>
<%
    String nombre = Html.esc(session.getAttribute("nombreUsuario"));
    String csrf = Html.esc(session.getAttribute("csrfToken"));
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="<%= csrf %>">
    <meta name="usuario-id" content="<%= Html.esc(session.getAttribute("idUsuario")) %>">
    <title>FinanZ · Tu espacio financiero</title>
    <link rel="icon" href="image/gatito_finanz.png">

    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com/3.4.17"></script>
    <!-- Íconos y gráficos -->
    <script src="https://cdn.jsdelivr.net/npm/lucide@0.263.0/dist/umd/lucide.min.js" defer></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js" defer></script>
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Fraunces:opsz,wght@9..144,600;9..144,700&display=swap" rel="stylesheet">

    <link href="css/estilos.css?v=3" rel="stylesheet" type="text/css"/>
</head>
<body class="app-shell">

    <div class="app-layout">

        <!-- Fondo oscuro del menú en celulares -->
        <div id="sidebar-overlay" class="sidebar-overlay"></div>

        <!-- SIDEBAR -->
        <aside id="sidebar" class="sidebar app-sidebar">

            <div class="mb-8 flex items-center gap-3 px-3">
                <div class="flex h-11 w-11 items-center justify-center overflow-hidden rounded-2xl bg-white/10">
                    <img src="image/gatito_finanz.png" alt="Logo FinanZ" class="h-full w-full object-contain">
                </div>
                <div class="min-w-0">
                    <p class="brand-serif text-lg font-bold leading-none">FinanZ</p>
                    <p class="mt-1 truncate text-xs text-[#a8d2ba]">Hola, <%= nombre %></p>
                </div>
                <button type="button" id="btn-cerrar-menu" class="ml-auto rounded-lg p-1 lg:hidden" aria-label="Cerrar menú">
                    <i data-lucide="x"></i>
                </button>
            </div>

            <nav class="space-y-1" aria-label="Secciones">
                <p class="nav-grupo">Gestión</p>
                <a href="#resumen" data-seccion="resumen" data-titulo="Resumen del mes" class="nav-item focus-ring">
                    <i data-lucide="layout-dashboard"></i><span>Resumen</span>
                </a>
                <a href="#transacciones" data-seccion="transacciones" data-titulo="Ingresos y gastos" class="nav-item focus-ring">
                    <i data-lucide="wallet-cards"></i><span>Transacciones</span>
                </a>
                <a href="#presupuestos" data-seccion="presupuestos" data-titulo="Presupuestos" class="nav-item focus-ring">
                    <i data-lucide="pie-chart"></i><span>Presupuestos</span>
                </a>
                <a href="#metas" data-seccion="metas" data-titulo="Metas de ahorro" class="nav-item focus-ring">
                    <i data-lucide="target"></i><span>Metas</span>
                </a>
                <a href="#simulador" data-seccion="simulador" data-titulo="Simulador de ahorro" class="nav-item focus-ring">
                    <i data-lucide="calculator"></i><span>Simulador</span>
                </a>

                <p class="nav-grupo">Análisis</p>
                <a href="#analisis" data-seccion="analisis" data-titulo="Análisis de gastos" class="nav-item focus-ring">
                    <i data-lucide="bar-chart-3"></i><span>Análisis</span>
                </a>
                <a href="#reportes" data-seccion="reportes" data-titulo="Reportes" class="nav-item focus-ring">
                    <i data-lucide="file-text"></i><span>Reportes</span>
                </a>

                <p class="nav-grupo">Aprender</p>
                <a href="#educacion" data-seccion="educacion" data-titulo="Educación financiera" class="nav-item focus-ring">
                    <i data-lucide="graduation-cap"></i><span>Educación</span>
                </a>
                <a href="#ajustes" data-seccion="ajustes" data-titulo="Ajustes" class="nav-item focus-ring">
                    <i data-lucide="settings-2"></i><span>Ajustes</span>
                </a>
            </nav>

            <form action="logout" method="post" class="mt-auto pt-6">
                <input type="hidden" name="_csrf" value="<%= csrf %>">
                <button type="submit" class="nav-item focus-ring w-full" style="color:#ffb3a3">
                    <i data-lucide="log-out"></i><span>Cerrar sesión</span>
                </button>
            </form>
        </aside>

        <!-- MAIN -->
        <main class="app-main">

            <header class="app-header">
                <div class="flex min-w-0 items-center gap-3">
                    <button type="button" id="btn-abrir-menu" class="icon-btn lg:hidden" aria-label="Abrir menú">
                        <i data-lucide="menu"></i>
                    </button>
                    <div class="min-w-0">
                        <p class="text-xs font-bold uppercase tracking-[.16em]" style="color:var(--forest)">Tu espacio financiero</p>
                        <h1 id="page-heading" class="brand-serif mt-1 truncate text-2xl font-bold sm:text-3xl">Resumen del mes</h1>
                    </div>
                </div>

                <div class="flex items-center gap-2">
                    <div class="relative">
                        <button type="button" id="btn-alertas" class="icon-btn relative" aria-label="Alertas" aria-expanded="false">
                            <i data-lucide="bell"></i>
                            <span id="contador-alertas" class="badge-alertas hidden">0</span>
                        </button>
                        <div id="panel-alertas" class="panel-alertas surface hidden" role="dialog" aria-label="Alertas"></div>
                    </div>
                    <button type="button" class="theme-toggle-btn icon-btn" aria-label="Cambiar tema">
                        <i data-lucide="moon"></i>
                    </button>
                </div>
            </header>

            <div class="px-4 pb-12 sm:px-8 lg:px-10">
                <div id="contenido" aria-live="polite">
                    <p class="py-10 text-center text-sm" style="color:var(--muted)">Cargando...</p>
                </div>
            </div>
        </main>
    </div>

    <!-- Notificación flotante -->
    <div id="toast" class="toast" role="status"></div>

    <script src="js/app.js?v=3"></script>
</body>
</html>
