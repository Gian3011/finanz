package com.mycompany.finanz.servicio;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sugiere la categoría de un gasto importado a partir del nombre del comercio,
 * con palabras clave de comercios frecuentes en Perú. Devuelve el nombre de una
 * de las categorías predeterminadas; si nada coincide, "Otros gastos".
 * (ImportacionServicio además prioriza la categoría que el usuario ya usó
 * antes para la misma descripción.)
 */
public final class CategorizadorGastos {

    public static final String OTROS = "Otros gastos";

    /** El orden importa: se usa la primera categoría que coincide. */
    private static final Map<String, Pattern> REGLAS = new LinkedHashMap<>();

    static {
        regla("Suscripciones", "NETFLIX", "SPOTIFY", "DISNEY", "DISNEYPLUS", "HBO", "HBOMAX", "PRIME VIDEO",
                "AMAZON PRIME", "YOUTUBE", "APPLE\\.COM", "APPLE COM", "ITUNES", "GOOGLE PLAY", "GOOGLE ONE",
                "MICROSOFT", "XBOX", "PLAYSTATION", "ADOBE", "CANVA", "OPENAI", "CHATGPT", "CLAUDE", "ANTHROPIC",
                "CRUNCHYROLL", "PARAMOUNT", "STAR PLUS", "DEEZER", "DUOLINGO", "ICLOUD", "DROPBOX");
        regla("Snacks y café", "STARBUCKS", "TAMBO", "OXXO", "MASS", "JUAN VALDEZ", "DUNKIN", "CAFE", "CAFETERIA",
                "COFFEE", "PANADERIA", "PASTELERIA", "HELADERIA", "LA LUCHA", "SAN ANTONIO", "REPSHOP", "LISTO");
        regla("Alimentación", "WONG", "PLAZA VEA", "PLAZAVEA", "TOTTUS", "METRO", "VIVANDA", "MAKRO", "MERCADO",
                "MINIMARKET", "BODEGA", "RESTAURANT", "RESTAURANTE", "RESTOBAR", "POLLERIA", "CHIFA", "CEVICHERIA",
                "PIZZA", "PIZZERIA", "KFC", "BEMBOS", "MCDONALDS", "MC DONALDS", "BURGER", "POPEYES", "NORKYS",
                "PARDOS", "RAPPI", "PEDIDOSYA", "PEDIDOS YA", "DIDI FOOD", "PAPA JOHNS", "DOMINOS", "SUBWAY", "CHILIS");
        regla("Transporte", "UBER", "CABIFY", "DIDI", "INDRIVE", "BEAT", "TAXI", "PRIMAX", "REPSOL", "PECSA",
                "PETROPERU", "GRIFO", "ESTACION DE SERVICIO", "PEAJE", "LATAM", "SKY AIRLINE", "JETSMART", "CRUZ DEL SUR",
                "OLTURSA", "METROPOLITANO", "LINEA 1", "TARJETA LIMA PASS", "ESTACIONAMIENTO", "PLAYA DE ESTACIONAMIENTO", "LOS PORTALES ESTAC");
        regla("Salud", "INKAFARMA", "MIFARMA", "BOTICA", "BOTICAS", "FARMACIA", "CLINICA", "HOSPITAL", "LABORATORIO",
                "DENTAL", "ODONTOLOG[A-Z]*", "OPTICA", "MEDIC[OA]S?", "POLICLINICO", "ESSALUD", "SANNA", "AUNA", "PACIFICO SALUD", "RIMAC SALUD");
        regla("Educación", "UTP", "UPC", "PUCP", "UNIVERSIDAD", "INSTITUTO", "COLEGIO", "ACADEMIA", "LIBRERIA",
                "CRISOL", "IBERO", "TAI LOY", "TAILOY", "UDEMY", "COURSERA", "PLATZI", "DOMESTIKA", "CERTUS", "SENATI");
        regla("Servicios", "LUZ DEL SUR", "ENEL", "SEDAPAL", "CALIDDA", "CLARO", "MOVISTAR", "ENTEL", "BITEL", "WIN",
                "TELEFONICA", "INTERNET", "RECARGA", "SERVICIO DE AGUA", "GAS NATURAL", "LIMA GAS", "SOLGAS");
        regla("Entretenimiento", "CINEPLANET", "CINEMARK", "CINEPOLIS", "UVK", "TELETICKET", "TICKETMASTER", "JOINNUS",
                "STEAM", "NINTENDO", "BOWLING", "KARAOKE", "DISCOTECA", "TEATRO", "MUSEO", "PARQUE", "JOCKEY PLAZA");
        regla("Vivienda", "ALQUILER", "SODIMAC", "PROMART", "MAESTRO", "CASAIDEAS", "MANTENIMIENTO", "CONDOMINIO",
                "FERRETERIA", "MUEBLES", "OECHSLE HOGAR", "IKEA");
    }

    private CategorizadorGastos() {
    }

    private static void regla(String categoria, String... palabras) {
        // Palabra completa: "METRO" coincide con "METRO SURCO" pero no con "METROPOLITANO"
        String alternativas = String.join("|", palabras);
        REGLAS.put(categoria, Pattern.compile("(?<![A-Z0-9])(?:" + alternativas + ")(?![A-Z0-9])"));
    }

    /** Nombre de la categoría sugerida para el texto (descripción del consumo o datos del comercio). */
    public static String sugerir(String texto) {
        String t = normalizar(texto);
        for (Map.Entry<String, Pattern> regla : REGLAS.entrySet()) {
            if (regla.getValue().matcher(t).find()) {
                return regla.getKey();
            }
        }
        return OTROS;
    }

    /** Sugerencia probando varios textos en orden (ej. primero el comercio, luego el detalle). */
    public static String sugerir(List<String> textos) {
        for (String texto : textos) {
            String categoria = sugerir(texto);
            if (!OTROS.equals(categoria)) {
                return categoria;
            }
        }
        return OTROS;
    }

    /** Mayúsculas y sin tildes, para comparar textos de bancos y comercios. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
