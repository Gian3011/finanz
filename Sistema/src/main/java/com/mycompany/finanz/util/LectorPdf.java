package com.mycompany.finanz.util;

import com.mycompany.finanz.servicio.ValidacionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Extrae el texto de un PDF con Apache PDFBox. El archivo se procesa en memoria
 * y no se guarda en el servidor. Acepta PDF protegidos con contraseña (muchos
 * bancos protegen el estado de cuenta) si el usuario escribe la clave.
 */
public final class LectorPdf {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_PAGINAS = 40;

    private LectorPdf() {
    }

    public static String extraerTexto(byte[] pdf, String clave) throws ValidacionException {
        if (pdf == null || pdf.length == 0) {
            throw new ValidacionException("Selecciona un archivo PDF.");
        }
        if (pdf.length > MAX_BYTES) {
            throw new ValidacionException("El PDF supera el tamaño máximo de 5 MB.");
        }
        if (!esPdf(pdf)) {
            throw new ValidacionException("El archivo no es un PDF válido.");
        }
        boolean sinClave = clave == null || clave.isEmpty();
        try (PDDocument documento = Loader.loadPDF(pdf, sinClave ? "" : clave)) {
            if (documento.getNumberOfPages() > MAX_PAGINAS) {
                throw new ValidacionException("El PDF tiene más de " + MAX_PAGINAS + " páginas. Sube solo el estado de cuenta o el comprobante.");
            }
            PDFTextStripper lector = new PDFTextStripper();
            lector.setSortByPosition(true); // respeta el orden de las columnas de las tablas
            String texto = lector.getText(documento);
            if (texto == null || texto.isBlank()) {
                throw new ValidacionException("El PDF no tiene texto (parece una imagen escaneada). "
                        + "Sube el PDF original que te envió el banco o la tienda.");
            }
            return texto;
        } catch (InvalidPasswordException e) {
            throw new ValidacionException(sinClave
                    ? "El PDF está protegido con contraseña. Escribe la clave del documento (la que te indicó tu banco) y vuelve a subirlo."
                    : "La clave del PDF no es correcta.");
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer el PDF. Verifica que el archivo no esté dañado.");
        }
    }

    /** Todo PDF empieza con "%PDF-" (se tolera basura antes, como permite el estándar). */
    static boolean esPdf(byte[] datos) {
        int limite = Math.min(datos.length, 1024);
        return new String(datos, 0, limite, StandardCharsets.ISO_8859_1).contains("%PDF-");
    }
}
