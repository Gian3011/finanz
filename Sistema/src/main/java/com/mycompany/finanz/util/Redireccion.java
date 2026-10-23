package com.mycompany.finanz.util;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirecciones con dirección relativa.
 *
 * response.sendRedirect() convierte la dirección en absoluta usando el esquema
 * con el que llegó la petición al servidor. Detrás de un túnel o proxy HTTPS
 * (Cloudflare, Codespaces) el servidor recibe "http", y el navegador terminaría
 * saltando a la versión sin cifrar del sitio. Con una dirección relativa el
 * navegador conserva el esquema y dominio que está usando (https).
 */
public final class Redireccion {

    private Redireccion() {
    }

    public static void a(HttpServletResponse response, String destino) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", destino);
    }
}
