package com.mycompany.finanz.util;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Construcción mínima de JSON para pasar datos de los gráficos al navegador
 * (atributos data-* que lee app.js). Evita agregar una librería solo para esto.
 */
public final class Json {

    private Json() {
    }

    public static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    public static String textos(Collection<String> valores) {
        StringBuilder sb = new StringBuilder("[");
        for (String v : valores) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append(texto(v));
        }
        return sb.append(']').toString();
    }

    public static String numeros(Collection<BigDecimal> valores) {
        StringBuilder sb = new StringBuilder("[");
        for (BigDecimal v : valores) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append(v == null ? "0" : v.toPlainString());
        }
        return sb.append(']').toString();
    }
}
