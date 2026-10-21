package com.mycompany.finanz.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hasheo de contraseñas con PBKDF2 + HMAC-SHA-256 y una sal aleatoria por usuario.
 * Nunca se guarda la contraseña original: solo el texto
 * "pbkdf2$iteraciones$sal$hash" (sal y hash en Base64).
 */
public final class PasswordUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2";
    private static final int ITERACIONES = 65_536;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashear(String contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        RANDOM.nextBytes(sal);
        byte[] hash = pbkdf2(contrasena.toCharArray(), sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    /**
     * Compara la contraseña ingresada con el hash guardado en tiempo constante
     * (para no dar pistas por el tiempo de respuesta).
     */
    public static boolean verificar(String contrasena, String hashGuardado) {
        if (contrasena == null || !esHash(hashGuardado)) {
            return false;
        }
        String[] partes = hashGuardado.split("\\$");
        int iteraciones = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        byte[] calculado = pbkdf2(contrasena.toCharArray(), sal, iteraciones);
        return MessageDigest.isEqual(esperado, calculado);
    }

    /** true si el valor tiene el formato de un hash generado por esta clase. */
    public static boolean esHash(String valor) {
        return valor != null && valor.startsWith(PREFIJO + "$") && valor.split("\\$").length == 4;
    }

    private static byte[] pbkdf2(char[] contrasena, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(contrasena, sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }

    /** Permite generar un hash desde consola: java PasswordUtil miClave */
    public static void main(String[] args) {
        System.out.println(hashear(args.length > 0 ? args[0] : "Finanz2026"));
    }
}
