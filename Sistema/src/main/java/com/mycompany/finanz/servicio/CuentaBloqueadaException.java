package com.mycompany.finanz.servicio;

/**
 * Se lanza cuando el usuario existe, la contraseña puede incluso ser
 * correcta, pero la cuenta está bloqueada por exceso de intentos fallidos.
 */
public class CuentaBloqueadaException extends Exception {
    public CuentaBloqueadaException(String mensaje) {
        super(mensaje);
    }
}
