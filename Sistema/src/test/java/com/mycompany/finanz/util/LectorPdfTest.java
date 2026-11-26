package com.mycompany.finanz.util;

import com.mycompany.finanz.servicio.ValidacionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** Lectura real de PDF con PDFBox: texto, archivos protegidos y archivos que no son PDF. */
class LectorPdfTest {

    @Test
    void extraeElTextoDeUnPdf() throws Exception {
        String texto = LectorPdf.extraerTexto(pdf(null, "ESTADO DE CUENTA", "21/10 22/10 WONG BENAVIDES 245.90"), null);
        assertTrue(texto.contains("ESTADO DE CUENTA"));
        assertTrue(texto.contains("21/10 22/10 WONG BENAVIDES 245.90"));
    }

    @Test
    void pideLaClaveSiElPdfEstaProtegido() throws Exception {
        byte[] protegido = pdf("45678912", "Consumo protegido 10.00");

        ValidacionException sinClave = assertThrows(ValidacionException.class, () -> LectorPdf.extraerTexto(protegido, ""));
        assertTrue(sinClave.getMessage().contains("protegido con contraseña"));
        ValidacionException claveMala = assertThrows(ValidacionException.class, () -> LectorPdf.extraerTexto(protegido, "000"));
        assertEquals("La clave del PDF no es correcta.", claveMala.getMessage());
        assertTrue(LectorPdf.extraerTexto(protegido, "45678912").contains("Consumo protegido 10.00"));
    }

    @Test
    void rechazaArchivosQueNoSonPdfOSinTexto() throws Exception {
        assertThrows(ValidacionException.class, () -> LectorPdf.extraerTexto(new byte[0], null));
        ValidacionException noPdf = assertThrows(ValidacionException.class,
                () -> LectorPdf.extraerTexto("hola, no soy un PDF".getBytes(), null));
        assertEquals("El archivo no es un PDF válido.", noPdf.getMessage());
        ValidacionException escaneado = assertThrows(ValidacionException.class, () -> LectorPdf.extraerTexto(pdf(null), null));
        assertTrue(escaneado.getMessage().contains("escaneada"));
    }

    /** PDF de una página con las líneas dadas; con clave, protegido como lo hacen los bancos. */
    static byte[] pdf(String clave, String... lineas) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                if (lineas.length > 0) {
                    c.beginText();
                    c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    c.setLeading(14);
                    c.newLineAtOffset(40, 740);
                    for (String linea : lineas) {
                        c.showText(linea);
                        c.newLine();
                    }
                    c.endText();
                }
            }
            if (clave != null) {
                StandardProtectionPolicy politica = new StandardProtectionPolicy("propietario-" + clave, clave, new AccessPermission());
                politica.setEncryptionKeyLength(128);
                doc.protect(politica);
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            doc.save(salida);
            return salida.toByteArray();
        }
    }
}
