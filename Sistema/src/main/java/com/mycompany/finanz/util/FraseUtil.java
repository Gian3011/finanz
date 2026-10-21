package com.mycompany.finanz.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utilidad para obtener frases motivacionales de finanzas personales.
 * Se usa en la pantalla de login para mostrar una frase distinta
 * cada vez que el usuario ingresa a la página.
 */
public final class FraseUtil {

    private static final String[] FRASES = {
        "Cada sol que ahorras hoy es un paso más hacia la libertad financiera de mañana.",
        "Un pequeño ahorro hoy es un gran respiro mañana.",
        "No ahorres lo que sobra después de gastar, gasta lo que sobra después de ahorrar.",
        "La libertad financiera empieza con un solo hábito: gastar menos de lo que ganas.",
        "Cada sol bien administrado hoy es un problema menos mañana.",
        "El control de tus finanzas es el primer paso hacia tu tranquilidad.",
        "No se trata de cuánto ganas, sino de cuánto conservas.",
        "Un presupuesto no te limita, te libera.",
        "Ahorrar poco pero constante vale más que ahorrar mucho pero nunca.",
        "Tus finanzas de hoy son el reflejo de tus decisiones de ayer.",
        "Invertir en tus finanzas es invertir en tu futuro yo."
    };

    private FraseUtil() {
        // Clase de utilidad, no se instancia
    }

    public static String obtenerAleatoria() {
        return FRASES[ThreadLocalRandom.current().nextInt(FRASES.length)];
    }
}
