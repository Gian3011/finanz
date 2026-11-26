package com.mycompany.finanz.servicio;

import com.mycompany.finanz.modelo.FormatoReporte;
import com.mycompany.finanz.util.Formato;
import com.mycompany.finanz.util.Html;

import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envía el resumen financiero por correo (SMTP con STARTTLS) y verifica si el envío
 * rebotó leyendo el buzón de la cuenta remitente (POP3 con SSL), con Jakarta Mail.
 *
 * La configuración se lee, en este orden (mismo patrón que ConexionDB):
 *   1. Variables de entorno FINANZ_MAIL_USUARIO, FINANZ_MAIL_CLAVE, FINANZ_MAIL_REMITENTE,
 *      FINANZ_MAIL_SMTP_HOST, FINANZ_MAIL_SMTP_PORT, FINANZ_MAIL_POP3_HOST, FINANZ_MAIL_POP3_PORT
 *   2. El archivo src/main/resources/mail.properties (no se sube a GitHub)
 *   3. Valores por defecto: smtp.gmail.com:587 y pop.gmail.com:995, sin usuario ni clave
 *
 * La clave nunca se escribe en los registros ni en los mensajes de error.
 */
public class CorreoServicio {

    /** Resultado de revisar el buzón de la cuenta remitente después de un envío. */
    public enum EstadoEntrega {
        ENTREGADO_SIN_REBOTE, REBOTADO, NO_VERIFICABLE
    }

    public static final String NO_CONFIGURADO = "El envío por correo no está configurado.";
    public static final String ERROR_ENVIO = "No se pudo enviar el correo. Intenta nuevamente.";

    /** Tiempo máximo para conectar y para cada lectura con los servidores de correo. */
    static final int TIEMPO_ESPERA_MS = 10_000;
    /** Cantidad de mensajes más recientes del buzón que se revisan buscando rebotes. */
    static final int MENSAJES_A_REVISAR = 50;
    /** Tolerancia por diferencias de reloj entre FinanZ y el servidor de correo. */
    private static final long TOLERANCIA_MS = 2 * 60 * 1000L;

    private static final List<String> REMITENTES_REBOTE = Arrays.asList("mailer-daemon", "postmaster");
    private static final List<String> ASUNTOS_REBOTE = Arrays.asList(
            "delivery status notification", "undelivered", "undeliverable", "no se pudo entregar");

    private static final Logger LOG = Logger.getLogger(CorreoServicio.class.getName());

    private final Configuracion config;

    public CorreoServicio() {
        this(Configuracion.cargar());
    }

    public CorreoServicio(Configuracion config) {
        this.config = config;
    }

    public boolean estaConfigurado() {
        return config.estaCompleta();
    }

    /**
     * Envía el reporte adjunto al correo destino.
     *
     * @throws ValidacionException con un mensaje para el usuario si falta la configuración o el envío falla
     */
    public void enviarReporte(String destino, String nombreUsuario, YearMonth mes, FormatoReporte formato, byte[] archivo)
            throws ValidacionException {
        if (!estaConfigurado()) {
            throw new ValidacionException(NO_CONFIGURADO);
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", config.smtpHost);
        props.put("mail.smtp.port", String.valueOf(config.smtpPuerto));
        props.put("mail.smtp.connectiontimeout", String.valueOf(TIEMPO_ESPERA_MS));
        props.put("mail.smtp.timeout", String.valueOf(TIEMPO_ESPERA_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(TIEMPO_ESPERA_MS));
        // Con mail.from, el Message-ID se arma con el correo de la cuenta y no con el nombre del servidor
        // (evita una consulta DNS lenta en algunos equipos)
        props.put("mail.from", config.usuario);
        Session session = Session.getInstance(props);

        try {
            MimeMessage mensaje = construirMensaje(session, config.remitente, destino, nombreUsuario, mes, formato, archivo);
            try (Transport transporte = session.getTransport("smtp")) {
                transporte.connect(config.smtpHost, config.smtpPuerto, config.usuario, config.clave);
                transporte.sendMessage(mensaje, mensaje.getAllRecipients());
            }
            LOG.info("Reporte " + mes + " (" + formato.getCodigo() + ") enviado a " + ocultarCorreo(destino));
        } catch (MessagingException e) {
            LOG.log(Level.WARNING, "No se pudo enviar el reporte a " + ocultarCorreo(destino)
                    + " por " + config.smtpHost + ":" + config.smtpPuerto + ": " + e.getMessage(), e);
            throw new ValidacionException(ERROR_ENVIO);
        }
    }

    /**
     * Revisa los mensajes recientes del buzón de la cuenta remitente (POP3, solo lectura,
     * sin borrar nada) buscando un aviso de rebote dirigido al correo destino posterior al envío.
     */
    public EstadoEntrega verificarEntrega(String destino, LocalDateTime enviadoEn) {
        if (!estaConfigurado()) {
            return EstadoEntrega.NO_VERIFICABLE;
        }
        Properties props = new Properties();
        props.put("mail.pop3s.host", config.pop3Host);
        props.put("mail.pop3s.port", String.valueOf(config.pop3Puerto));
        props.put("mail.pop3s.connectiontimeout", String.valueOf(TIEMPO_ESPERA_MS));
        props.put("mail.pop3s.timeout", String.valueOf(TIEMPO_ESPERA_MS));
        Session session = Session.getInstance(props);
        Date desde = Date.from(enviadoEn.atZone(ZoneId.systemDefault()).toInstant());

        Store store = null;
        Folder bandeja = null;
        try {
            store = session.getStore("pop3s");
            store.connect(config.pop3Host, config.pop3Puerto, usuarioPop3(), config.clave);
            bandeja = store.getFolder("INBOX");
            bandeja.open(Folder.READ_ONLY);
            int total = bandeja.getMessageCount();
            List<Message> recientes = total == 0
                    ? new ArrayList<>()
                    : Arrays.asList(bandeja.getMessages(Math.max(1, total - MENSAJES_A_REVISAR + 1), total));
            return analizarBuzon(recientes, destino, desde);
        } catch (MessagingException e) {
            LOG.log(Level.WARNING, "No se pudo verificar la entrega a " + ocultarCorreo(destino)
                    + " por POP3 (" + config.pop3Host + ":" + config.pop3Puerto + "): " + e.getMessage(), e);
            return EstadoEntrega.NO_VERIFICABLE;
        } finally {
            cerrar(bandeja, store);
        }
    }

    /**
     * En Gmail, el prefijo "recent:" hace que POP3 devuelva los mensajes de los últimos
     * 30 días aunque ya se hayan descargado antes, así se puede verificar varias veces.
     */
    private String usuarioPop3() {
        return config.pop3Host.toLowerCase(Locale.ROOT).endsWith("gmail.com") && !config.usuario.startsWith("recent:")
                ? "recent:" + config.usuario
                : config.usuario;
    }

    private static void cerrar(Folder bandeja, Store store) {
        try {
            if (bandeja != null && bandeja.isOpen()) {
                bandeja.close(false); // false: no aplicar borrados
            }
        } catch (MessagingException e) {
            LOG.fine("No se pudo cerrar INBOX: " + e.getMessage());
        }
        try {
            if (store != null) {
                store.close();
            }
        } catch (MessagingException e) {
            LOG.fine("No se pudo cerrar la conexión POP3: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Armado del mensaje (sin conexión: se prueba en memoria)
    // ------------------------------------------------------------------

    static String asunto(YearMonth mes) {
        return "FinanZ - Resumen financiero de " + Formato.mes(mes);
    }

    static String nombreAdjunto(YearMonth mes, FormatoReporte formato) {
        return "FinanZ-resumen-" + mes + "." + formato.getExtension();
    }

    static MimeMessage construirMensaje(Session session, String remitente, String destino, String nombreUsuario,
                                        YearMonth mes, FormatoReporte formato, byte[] archivo) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(remitente, true));
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destino, true));
        mensaje.setSubject(asunto(mes), StandardCharsets.UTF_8.name());
        mensaje.setSentDate(new Date());

        String nombre = nombreUsuario == null || nombreUsuario.isBlank() ? "usuario de FinanZ" : nombreUsuario.trim();
        String textoPlano = "Hola, " + nombre + ":\n\n"
                + "Te enviamos adjunto tu resumen financiero de " + Formato.mes(mes)
                + " en formato " + formato.getNombre() + ".\n\n"
                + "Este correo fue generado automáticamente por FinanZ - Plataforma Web de Educación "
                + "y Gestión Financiera Personal. Si no lo solicitaste, puedes ignorarlo.";
        String html = "<div style=\"font-family:Arial,Helvetica,sans-serif;color:#17221D;font-size:14px;line-height:1.5\">"
                + "<p>Hola, " + Html.esc(nombre) + ":</p>"
                + "<p>Te enviamos adjunto tu <strong>resumen financiero de " + Html.esc(Formato.mes(mes))
                + "</strong> en formato " + Html.esc(formato.getNombre()) + ".</p>"
                + "<p style=\"color:#66736C;font-size:12px\">Este correo fue generado automáticamente por FinanZ – "
                + "Plataforma Web de Educación y Gestión Financiera Personal. Si no lo solicitaste, puedes ignorarlo.</p>"
                + "</div>";

        MimeBodyPart parteTexto = new MimeBodyPart();
        parteTexto.setText(textoPlano, StandardCharsets.UTF_8.name());
        MimeBodyPart parteHtml = new MimeBodyPart();
        parteHtml.setContent(html, "text/html; charset=UTF-8");
        MimeMultipart alternativas = new MimeMultipart("alternative");
        alternativas.addBodyPart(parteTexto);
        alternativas.addBodyPart(parteHtml);
        MimeBodyPart cuerpo = new MimeBodyPart();
        cuerpo.setContent(alternativas);

        MimeBodyPart adjunto = new MimeBodyPart();
        adjunto.setDataHandler(new DataHandler(new ByteArrayDataSource(archivo, formato.getTipoContenido())));
        adjunto.setFileName(nombreAdjunto(mes, formato));
        adjunto.setDisposition(Part.ATTACHMENT);

        MimeMultipart contenido = new MimeMultipart("mixed");
        contenido.addBodyPart(cuerpo);
        contenido.addBodyPart(adjunto);
        mensaje.setContent(contenido);
        mensaje.saveChanges();
        return mensaje;
    }

    // ------------------------------------------------------------------
    // Detección de rebotes (sin conexión: se prueba con mensajes simulados)
    // ------------------------------------------------------------------

    static EstadoEntrega analizarBuzon(List<? extends Message> mensajes, String destino, Date enviadoEn) {
        for (Message m : mensajes) {
            if (esRebote(m, destino, enviadoEn)) {
                return EstadoEntrega.REBOTADO;
            }
        }
        return EstadoEntrega.ENTREGADO_SIN_REBOTE;
    }

    /**
     * true si el mensaje es un aviso de no entrega (remitente mailer-daemon/postmaster o asunto
     * de rebote), es posterior al envío y menciona el correo destino.
     */
    static boolean esRebote(Message m, String destino, Date enviadoEn) {
        try {
            Date fecha = m.getSentDate() != null ? m.getSentDate() : m.getReceivedDate();
            if (fecha != null && enviadoEn != null && fecha.getTime() < enviadoEn.getTime() - TOLERANCIA_MS) {
                return false;
            }
            if (!remitenteDeRebote(m) && !asuntoDeRebote(m.getSubject())) {
                return false;
            }
            String buscado = destino.toLowerCase(Locale.ROOT);
            String[] fallidos = m.getHeader("X-Failed-Recipients");
            if (fallidos != null && String.join(",", fallidos).toLowerCase(Locale.ROOT).contains(buscado)) {
                return true;
            }
            StringBuilder texto = new StringBuilder();
            extraerTexto(m, texto, 0);
            return texto.toString().toLowerCase(Locale.ROOT).contains(buscado);
        } catch (MessagingException | IOException e) {
            LOG.fine("No se pudo leer un mensaje del buzón: " + e.getMessage());
            return false;
        }
    }

    private static boolean remitenteDeRebote(Message m) throws MessagingException {
        Address[] remitentes = m.getFrom();
        if (remitentes == null) {
            return false;
        }
        for (Address a : remitentes) {
            String direccion = a.toString().toLowerCase(Locale.ROOT);
            for (String r : REMITENTES_REBOTE) {
                if (direccion.contains(r)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean asuntoDeRebote(String asunto) {
        if (asunto == null) {
            return false;
        }
        String a = asunto.toLowerCase(Locale.ROOT);
        return ASUNTOS_REBOTE.stream().anyMatch(a::contains);
    }

    /** Junta el texto de las partes del mensaje (incluidos los reportes de entrega y el mensaje original). */
    private static void extraerTexto(Part parte, StringBuilder destino, int nivel) throws MessagingException, IOException {
        if (nivel > 5 || destino.length() > 200_000) {
            return;
        }
        Object contenido = parte.getContent();
        if (contenido instanceof String) {
            destino.append((String) contenido).append('\n');
        } else if (contenido instanceof Multipart) {
            Multipart mp = (Multipart) contenido;
            for (int i = 0; i < mp.getCount(); i++) {
                extraerTexto(mp.getBodyPart(i), destino, nivel + 1);
            }
        } else if (contenido instanceof Part) {
            extraerTexto((Part) contenido, destino, nivel + 1);
        } else if (contenido instanceof InputStream) {
            try (InputStream in = (InputStream) contenido) {
                destino.append(new String(in.readNBytes(100_000), StandardCharsets.UTF_8)).append('\n');
            }
        }
    }

    /** Oculta el correo en los registros: "juan.perez@gmail.com" -> "ju***@gmail.com". */
    static String ocultarCorreo(String correo) {
        if (correo == null) {
            return "(sin correo)";
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.substring(0, Math.min(2, arroba)) + "***" + correo.substring(arroba);
    }

    // ------------------------------------------------------------------
    // Configuración
    // ------------------------------------------------------------------

    /** Datos de la cuenta de correo que envía los reportes. */
    public static final class Configuracion {

        private static final String CLAVE_DE_EJEMPLO = "TU_CONTRASEÑA_DE_APLICACION";

        final String smtpHost;
        final int smtpPuerto;
        final String pop3Host;
        final int pop3Puerto;
        final String usuario;
        final String clave;
        final String remitente;

        public Configuracion(String smtpHost, int smtpPuerto, String pop3Host, int pop3Puerto,
                             String usuario, String clave, String remitente) {
            this.smtpHost = smtpHost;
            this.smtpPuerto = smtpPuerto;
            this.pop3Host = pop3Host;
            this.pop3Puerto = pop3Puerto;
            this.usuario = usuario == null ? "" : usuario.trim();
            this.clave = limpiarClave(clave);
            this.remitente = remitente == null || remitente.isBlank() ? this.usuario : remitente.trim();
        }

        boolean estaCompleta() {
            return !usuario.isEmpty() && !clave.isEmpty() && !CLAVE_DE_EJEMPLO.equals(clave);
        }

        /** Google muestra la contraseña de aplicación en grupos de 4 letras: se quitan espacios y los signos < >. */
        static String limpiarClave(String clave) {
            if (clave == null) {
                return "";
            }
            String c = clave.replace(" ", "").trim();
            if (c.startsWith("<") && c.endsWith(">")) {
                c = c.substring(1, c.length() - 1);
            }
            return c;
        }

        public static Configuracion cargar() {
            Properties archivo = new Properties();
            try (InputStream in = CorreoServicio.class.getClassLoader().getResourceAsStream("mail.properties")) {
                if (in != null) {
                    archivo.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                LOG.warning("No se pudo leer mail.properties: " + e.getMessage());
            }
            return new Configuracion(
                    valor("FINANZ_MAIL_SMTP_HOST", archivo, "mail.smtp.host", "smtp.gmail.com"),
                    entero(valor("FINANZ_MAIL_SMTP_PORT", archivo, "mail.smtp.port", "587"), 587),
                    valor("FINANZ_MAIL_POP3_HOST", archivo, "mail.pop3.host", "pop.gmail.com"),
                    entero(valor("FINANZ_MAIL_POP3_PORT", archivo, "mail.pop3.port", "995"), 995),
                    valor("FINANZ_MAIL_USUARIO", archivo, "mail.usuario", ""),
                    valor("FINANZ_MAIL_CLAVE", archivo, "mail.clave", ""),
                    valor("FINANZ_MAIL_REMITENTE", archivo, "mail.remitente", ""));
        }

        private static String valor(String variableEntorno, Properties archivo, String clave, String porDefecto) {
            String env = System.getenv(variableEntorno);
            if (env != null && !env.isBlank()) {
                return env.trim();
            }
            return archivo.getProperty(clave, porDefecto).trim();
        }

        private static int entero(String valor, int porDefecto) {
            try {
                return Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                return porDefecto;
            }
        }
    }
}
