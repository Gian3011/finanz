# FinanZ – Plataforma Web de Educación y Gestión Financiera Personal

[![Abrir en GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Gian3011/finanz?quickstart=1)
[![CI](https://github.com/Gian3011/finanz/actions/workflows/ci.yml/badge.svg)](https://github.com/Gian3011/finanz/actions/workflows/ci.yml)

Proyecto del curso **Integrador I: Sistemas Software** – Universidad Tecnológica del Perú (2026).

FinanZ permite registrar ingresos y gastos, planificar presupuestos con alertas, crear metas de ahorro,
detectar gastos hormiga, simular escenarios de ahorro, comparar periodos, exportar reportes en PDF y
aprender finanzas personales con lecciones y cuestionarios.

🌐 **Página del proyecto:** https://gian3011.github.io/finanz/

---

## ▶️ Ver el sistema funcionando (sin instalar nada)

1. Haz clic en el botón **“Open in GitHub Codespaces”** de arriba (necesitas una cuenta de GitHub).
2. Espera 2–3 minutos mientras se crea el entorno (Java 17 + MySQL 8 con datos de demostración).
3. Se abrirá la pestaña **FinanZ** (puerto 8080). Si no se abre, ve a la pestaña **Puertos** y abre el 8080.
4. Inicia sesión con la cuenta

> Para detener el servidor usa `Ctrl + C` en la terminal; para volver a iniciarlo: `bash .devcontainer/iniciar.sh`.

---

## ✅ Requerimientos implementados

### Funcionales

| # | Requerimiento | Dónde |
|---|---|---|
| RF01 | Iniciar sesión (y registrarse) | `Login.jsp`, `/registro` |
| RF02 | Registrar ingresos | Transacciones |
| RF03 | Registrar gastos | Transacciones |
| RF04 | Clasificar gastos por categorías (predeterminadas y personales) | Transacciones |
| RF05 | Consultar y filtrar el historial (texto, tipo, categoría, rango de fechas, hormiga) | Transacciones |
| RF06 | Establecer presupuesto mensual | Presupuestos |
| RF07 | Límites de gasto por categoría | Presupuestos |
| RF08 | Mostrar monto disponible | Presupuestos / Resumen |
| RF09 | Alertas al acercarse (80 %) o superar el presupuesto | Campana de alertas, avisos al registrar |
| RF10 | Crear metas con monto y fecha objetivo | Metas |
| RF11 | Registrar aportes a las metas | Metas |
| RF12 | Mostrar el progreso de las metas | Metas / Resumen |
| RF13 | Identificar y clasificar gastos hormiga (automático + manual) | Transacciones / Análisis |
| RF14 | Estadísticas y gráficos de hábitos de gasto | Resumen / Análisis |
| RF15 | Comparar ingresos, gastos y ahorro entre periodos | Análisis |
| RF16 | Exportar el resumen financiero en PDF | Reportes |
| RF17 | Recomendaciones personalizadas | Resumen / Análisis |
| RF18 | Contenido educativo | Educación |
| RF19 | Cuestionarios con resultados (nota 0–20) | Educación |
| RF20 | Simular escenarios de ahorro | Simulador |

### No funcionales

| Requerimiento | Cómo se cumple |
|---|---|
| **Rendimiento** (100–200 usuarios simultáneos) | Pool de conexiones HikariCP (`ConexionDB`), servlets sin estado, índices en la BD, caché de sentencias preparadas. |
| **Velocidad** (< 2 s) | Secciones cargadas por AJAX (solo el fragmento HTML), consultas agregadas en SQL, caché de recursos estáticos. |
| **Disponibilidad** (99 %) | Aplicación sin estado compartido en memoria, lista para desplegarse en GlassFish/Jetty con reinicio automático; el pool se reconecta si la BD se reinicia. |
| **Seguridad** | Contraseñas con PBKDF2-HMAC-SHA256 + sal, bloqueo temporal tras 3 intentos, filtro de sesión, token CSRF, escape de HTML (XSS), `PreparedStatement` (inyección SQL), verificación de que cada registro pertenece al usuario, cookie `HttpOnly`. |
| **Diseño responsivo** | Tailwind CSS + CSS propio: menú lateral desplegable en celulares, grillas adaptables, modo oscuro. |

---

## 🧱 Arquitectura

```
Navegador (HTML5 + CSS3 + JavaScript, Chart.js)
        │  AJAX
        ▼
Controladores (Servlets)  ── SeguridadFiltro (sesión, CSRF, UTF-8)
        │
Servicios (reglas de negocio: presupuesto, gasto hormiga, metas, simulador, recomendaciones, PDF)
        │
DAO (JDBC + PreparedStatement)  ── ConexionDB (Singleton + pool HikariCP)
        │
MySQL 8 (db_finanz)
```

```
Sistema/src/main/java/com/mycompany/finanz/
├── controlador/   Servlets (Login, Registro, Resumen, Movimiento, Presupuesto, Meta, Simulador, Análisis, Reporte, Educación, Ajustes, Alerta)
├── servicio/      Lógica de negocio y validaciones
├── dao/           Acceso a datos
├── modelo/        Entidades (Movimiento → Ingreso / Gasto, Presupuesto, MetaAhorro, ...)
├── filtro/        Seguridad y ciclo de vida de la aplicación
└── util/          Hash de contraseñas, formato, escape HTML, JSON
Sistema/src/main/webapp/
├── Login.jsp, dashboard.jsp
├── WEB-INF/vistas/   Fragmentos JSP de cada sección (no accesibles directamente)
├── css/, js/, image/
db/db_finanz.sql      Script de la base de datos con datos de demostración
```

---

## 💻 Instalación local (NetBeans + GlassFish)

**Requisitos:** JDK 17, Apache NetBeans 19, MySQL Server 8.0, GlassFish 6.2.5.

1. **Base de datos:** abre `db/db_finanz.sql` en MySQL Workbench y ejecútalo completo (⚡).
   Crea la base `db_finanz` con 18 tablas y datos de demostración.
2. **Credenciales:** copia `Sistema/src/main/resources/db.properties.example` como `db.properties`
   (misma carpeta) y escribe tu usuario y contraseña de MySQL. *Este archivo no se sube a GitHub.*
3. **NetBeans:** `File → Open Project` → carpeta `Sistema`. Clic derecho → **Run** (GlassFish).
4. Abre `http://localhost:8080/Finanz/` e inicia sesión con la cuenta de demostración.

**Alternativa sin GlassFish:** dentro de `Sistema` ejecuta `mvn jetty:run` y abre `http://localhost:8080`.

**Pruebas unitarias:** `mvn test` (dentro de `Sistema`).

---

## 👥 Equipo

- Ashley Canares Ramos
- Gianmarco Morales Amao
- Jesus Flores Herrera
- Juan Miguel Choque
- Maria Jose Condori Mendiguri
