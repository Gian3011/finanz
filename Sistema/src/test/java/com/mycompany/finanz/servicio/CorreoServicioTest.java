package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.FormatoReporte;
import com.mycompany.finanz.servicio.CorreoServicio.Configuracion;
import com.mycompany.finanz.servicio.CorreoServicio.EstadoEntrega;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Envío por correo y detección de rebotes, sin conectarse a ningún servidor. */
class CorreoServicioTest {

    private static final Session SESION = Session.getInstance(propiedades());
    private static final YearMonth MES = YearMonth.of(2026, 10);
    private static final String DESTINO = "ana.perez@ejemplo.com";

    @Test
    void armaAsuntoYNombreDelAdjunto() {
        assertEquals("FinanZ - Resumen financiero de octubre 2026", CorreoServicio.asunto(MES));
        assertEquals("FinanZ-resumen-2026-10.pdf", CorreoServicio.nombreAdjunto(MES, FormatoReporte.PDF));
        assertEquals("FinanZ-resumen-2026-10.xlsx", CorreoServicio.nombreAdjunto(MES, FormatoReporte.EXCEL));
        assertEquals("FinanZ-resumen-2026-10.docx", CorreoServicio.nombreAdjunto(MES, FormatoReporte.WORD));
    }

    @Test
    void construyeElMensajeConHtmlTextoYAdjunto() throws Exception {
        byte[] archivo = "%PDF-1.4 contenido de prueba".getBytes();
        MimeMessage armado = CorreoServicio.construirMensaje(SESION, "FinanZ <finanzutp@gmail.com>", DESTINO,
                "Ana <Pérez>", MES, FormatoReporte.PDF, archivo);

        // Se serializa y se vuelve a leer, como lo haría el servidor de correo
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        armado.writeTo(bytes);
        MimeMessage m = new MimeMessage(SESION, new ByteArrayInputStream(bytes.toByteArray()));

        assertEquals("FinanZ - Resumen financiero de octubre 2026", m.getSubject());
        InternetAddress de = (InternetAddress) m.getFrom()[0];
        assertEquals("finanzutp@gmail.com", de.getAddress());
        assertEquals("FinanZ", de.getPersonal());
        assertEquals(DESTINO, ((InternetAddress) m.getRecipients(Message.RecipientType.TO)[0]).getAddress());

        MimeMultipart mixto = (MimeMultipart) m.getContent();
        assertEquals(2, mixto.getCount());
        MimeMultipart alternativas = (MimeMultipart) mixto.getBodyPart(0).getContent();
        String texto = (String) alternativas.getBodyPart(0).getContent();
        String html = (String) alternativas.getBodyPart(1).getContent();
        assertTrue(alternativas.getBodyPart(0).isMimeType("text/plain"));
        assertTrue(alternativas.getBodyPart(1).isMimeType("text/html"));
        assertTrue(texto.contains("Hola, Ana <Pérez>:"));
        assertTrue(texto.contains("octubre 2026"));
        assertTrue(html.contains("Ana &lt;Pérez&gt;"), "el nombre del usuario va escapado en el HTML");
        assertFalse(html.contains("<Pérez>"));

        BodyPart adjunto = mixto.getBodyPart(1);
        assertEquals("FinanZ-resumen-2026-10.pdf", adjunto.getFileName());
        assertTrue(adjunto.isMimeType("application/pdf"));
        assertEquals(BodyPart.ATTACHMENT, adjunto.getDisposition());
        assertArrayEquals(archivo, ((java.io.InputStream) adjunto.getContent()).readAllBytes());
    }

    @Test
    void validaElCorreoDestino() throws ValidacionException {
        assertEquals("ana.perez@ejemplo.com", Validar.email("  Ana.Perez@Ejemplo.com "));
        assertThrows(ValidacionException.class, () -> Validar.email("ana@"));
        assertThrows(ValidacionException.class, () -> Validar.email("sin-arroba.com"));
        assertThrows(ValidacionException.class, () -> Validar.email(""));
        assertThrows(ValidacionException.class, () -> Validar.email("a".repeat(140) + "@ejemplo.com"));
    }

    @Test
    void aplicaElLimiteDeCincoEnviosPorHora() throws ValidacionException {
        ReporteServicio.validarLimiteEnvios(0);
        ReporteServicio.validarLimiteEnvios(4);
        ValidacionException e = assertThrows(ValidacionException.class, () -> ReporteServicio.validarLimiteEnvios(5));
        assertEquals("Alcanzaste el límite de envíos por hora. Inténtalo más tarde.", e.getMessage());
        assertThrows(ValidacionException.class, () -> ReporteServicio.validarLimiteEnvios(12));
    }

    @Test
    void detectaElReboteDelCorreoDestino() throws Exception {
        Date enviado = new Date();
        MimeMessage rebote = mensaje("Mail Delivery Subsystem <mailer-daemon@googlemail.com>",
                "Delivery Status Notification (Failure)",
                "No se ha encontrado la dirección " + DESTINO + " o no puede recibir correo.", enviado);

        assertTrue(CorreoServicio.esRebote(rebote, DESTINO, enviado));
        assertEquals(EstadoEntrega.REBOTADO, CorreoServicio.analizarBuzon(List.of(rebote), DESTINO, enviado));
    }

    @Test
    void detectaElReboteConAsuntoEnEspanolYCabeceraXFailedRecipients() throws Exception {
        Date enviado = new Date();
        MimeMessage rebote = mensaje("Postmaster <postmaster@outlook.com>", "No se pudo entregar: FinanZ",
                "El mensaje no se pudo entregar.", enviado);
        rebote.setHeader("X-Failed-Recipients", DESTINO);
        rebote.saveChanges();

        assertTrue(CorreoServicio.esRebote(rebote, DESTINO, enviado));
    }

    @Test
    void ignoraMensajesQueNoSonReboteDelEnvio() throws Exception {
        Date enviado = new Date();
        MimeMessage otroDestino = mensaje("mailer-daemon@googlemail.com", "Delivery Status Notification (Failure)",
                "No se encontró la dirección otra.persona@ejemplo.com", enviado);
        MimeMessage respuestaNormal = mensaje("Ana <" + DESTINO + ">", "Re: FinanZ - Resumen financiero",
                "¡Gracias! Recibí el reporte de " + DESTINO, enviado);
        MimeMessage reboteAntiguo = mensaje("mailer-daemon@googlemail.com", "Undelivered Mail Returned to Sender",
                "Falló la entrega a " + DESTINO, new Date(enviado.getTime() - 24 * 60 * 60 * 1000L));

        assertFalse(CorreoServicio.esRebote(otroDestino, DESTINO, enviado));
        assertFalse(CorreoServicio.esRebote(respuestaNormal, DESTINO, enviado));
        assertFalse(CorreoServicio.esRebote(reboteAntiguo, DESTINO, enviado), "rebote anterior al envío");
        assertEquals(EstadoEntrega.ENTREGADO_SIN_REBOTE, CorreoServicio.analizarBuzon(
                List.of(otroDestino, respuestaNormal, reboteAntiguo), DESTINO, enviado));
        assertEquals(EstadoEntrega.ENTREGADO_SIN_REBOTE, CorreoServicio.analizarBuzon(List.of(), DESTINO, enviado));
    }

    @Test
    void sinUsuarioOClaveNoEnviaNiVerifica() {
        CorreoServicio sinClave = new CorreoServicio(new Configuracion("smtp.gmail.com", 587, "pop.gmail.com", 995,
                "finanzutp@gmail.com", "", null));
        CorreoServicio conClaveDeEjemplo = new CorreoServicio(new Configuracion("smtp.gmail.com", 587, "pop.gmail.com", 995,
                "finanzutp@gmail.com", "TU_CONTRASEÑA_DE_APLICACION", null));

        assertFalse(sinClave.estaConfigurado());
        assertFalse(conClaveDeEjemplo.estaConfigurado());
        ValidacionException e = assertThrows(ValidacionException.class, () ->
                sinClave.enviarReporte(DESTINO, "Ana", MES, FormatoReporte.PDF, new byte[]{1}));
        assertEquals("El envío por correo no está configurado.", e.getMessage());
        assertEquals(EstadoEntrega.NO_VERIFICABLE, sinClave.verificarEntrega(DESTINO, LocalDateTime.now()));
    }

    @Test
    void limpiaLaClaveDeAplicacionYOcultaElCorreoEnLosRegistros() {
        assertEquals("abcdefghijklmnop", Configuracion.limpiarClave(" abcd efgh ijkl mnop "));
        assertEquals("abcdefghijklmnop", Configuracion.limpiarClave("<abcdefghijklmnop>"));
        assertEquals("", Configuracion.limpiarClave(null));
        assertEquals("an***@ejemplo.com", CorreoServicio.ocultarCorreo(DESTINO));
        assertEquals("***", CorreoServicio.ocultarCorreo("sin-arroba"));
    }

    private static Properties propiedades() {
        Properties p = new Properties();
        p.put("mail.from", "finanzutp@gmail.com"); // igual que CorreoServicio: sin consulta DNS
        return p;
    }

    private static MimeMessage mensaje(String de, String asunto, String texto, Date fecha)
            throws MessagingException, IOException {
        MimeMessage m = new MimeMessage(SESION);
        m.setFrom(new InternetAddress(de));
        m.setRecipient(Message.RecipientType.TO, new InternetAddress("finanzutp@gmail.com"));
        m.setSubject(asunto, "UTF-8");
        m.setSentDate(fecha);
        MimeBodyPart cuerpo = new MimeBodyPart();
        cuerpo.setText(texto, "UTF-8");
        MimeMultipart partes = new MimeMultipart();
        partes.addBodyPart(cuerpo);
        m.setContent(partes);
        m.saveChanges();
        // Se serializa y vuelve a leer para que se comporte como un mensaje recibido
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        m.writeTo(bytes);
        return new MimeMessage(SESION, new ByteArrayInputStream(bytes.toByteArray()));
    }
}
