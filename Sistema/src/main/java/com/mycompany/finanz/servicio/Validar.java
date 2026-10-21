package com.mycompany.finanz.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Conversión y validación de los datos que llegan desde los formularios.
 * Cada método lanza ValidacionException con un mensaje listo para el usuario.
 */
public final class Validar {

    public static final BigDecimal MONTO_MAXIMO = new BigDecimal("99999999.99");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");

    private Validar() {
    }

    public static String texto(String valor, String campo, int maximo) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new ValidacionException("El campo \"" + campo + "\" admite como máximo " + maximo + " caracteres.");
        }
        return limpio;
    }

    public static String textoOpcional(String valor, String campo, int maximo) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return texto(valor, campo, maximo);
    }

    public static BigDecimal monto(String valor, String campo) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("Indica el " + campo + ".");
        }
        BigDecimal monto;
        try {
            monto = new BigDecimal(valor.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new ValidacionException("El " + campo + " ingresado no es un número válido.");
        }
        if (monto.signum() <= 0) {
            throw new ValidacionException("El " + campo + " debe ser mayor a cero.");
        }
        if (monto.compareTo(MONTO_MAXIMO) > 0 || monto.scale() > 2) {
            throw new ValidacionException("El " + campo + " debe tener como máximo 2 decimales y ser menor a 100 millones.");
        }
        return monto;
    }

    public static BigDecimal montoOpcional(String valor, String campo) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        BigDecimal monto;
        try {
            monto = new BigDecimal(valor.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new ValidacionException("El " + campo + " ingresado no es un número válido.");
        }
        if (monto.signum() < 0 || monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new ValidacionException("El " + campo + " no es válido.");
        }
        return monto;
    }

    public static LocalDate fecha(String valor, String campo) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("Indica la " + campo + ".");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new ValidacionException("La " + campo + " ingresada no es válida.");
        }
    }

    /** Convierte "yyyy-MM" (input type="month"); si viene vacío devuelve el mes actual. */
    public static YearMonth mes(String valor) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new ValidacionException("El mes indicado no es válido.");
        }
    }

    public static int entero(String valor, String campo) throws ValidacionException {
        try {
            return Integer.parseInt(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("Selecciona un valor válido para \"" + campo + "\".");
        }
    }

    public static String email(String valor) throws ValidacionException {
        String email = texto(valor, "correo electrónico", 150).toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidacionException("El correo electrónico no tiene un formato válido.");
        }
        return email;
    }

    /** Política de contraseñas: mínimo 8 caracteres, con al menos una letra y un número. */
    public static void contrasenaSegura(String contrasena) throws ValidacionException {
        if (contrasena == null || contrasena.length() < 8 || contrasena.length() > 100
                || !contrasena.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*") || !contrasena.matches(".*\\d.*")) {
            throw new ValidacionException("La contraseña debe tener al menos 8 caracteres e incluir letras y números.");
        }
    }
}
