package com.mycompany.finanz.modelo;

/**
 * Formatos en los que se puede exportar el resumen financiero con JasperReports.
 * El código se guarda en la columna reporte_financiero.formato.
 */
public enum FormatoReporte {

    PDF("PDF", "pdf", "application/pdf", "PDF"),
    EXCEL("XLSX", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel"),
    WORD("DOCX", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word");

    private final String codigo;
    private final String extension;
    private final String tipoContenido;
    private final String nombre;

    FormatoReporte(String codigo, String extension, String tipoContenido, String nombre) {
        this.codigo = codigo;
        this.extension = extension;
        this.tipoContenido = tipoContenido;
        this.nombre = nombre;
    }

    /** Nombre para mostrar al usuario: "PDF", "Excel" o "Word". */
    public String getNombre() {
        return nombre;
    }

    /** Acepta la extensión o el código ("pdf", "xlsx", "DOCX"...); si no se reconoce, devuelve null. */
    public static FormatoReporte desde(String valor) {
        if (valor == null) {
            return null;
        }
        for (FormatoReporte f : values()) {
            if (f.extension.equalsIgnoreCase(valor.trim()) || f.codigo.equalsIgnoreCase(valor.trim())) {
                return f;
            }
        }
        return null;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getExtension() {
        return extension;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }
}
