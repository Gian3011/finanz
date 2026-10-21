package com.mycompany.finanz.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void elHashNoContieneLaContrasenaYSeVerifica() {
        String hash = PasswordUtil.hashear("Finanz2026");
        assertFalse(hash.contains("Finanz2026"));
        assertTrue(PasswordUtil.esHash(hash));
        assertTrue(PasswordUtil.verificar("Finanz2026", hash));
        assertFalse(PasswordUtil.verificar("finanz2026", hash));
    }

    @Test
    void cadaHashUsaUnaSalDistinta() {
        assertNotEquals(PasswordUtil.hashear("misma123"), PasswordUtil.hashear("misma123"));
    }

    @Test
    void rechazaValoresQueNoSonHash() {
        assertFalse(PasswordUtil.verificar("1234", "1234"));
        assertFalse(PasswordUtil.verificar(null, PasswordUtil.hashear("abc12345")));
    }
}
