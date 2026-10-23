-- =====================================================================
--  FinanZ - Reparación de tildes mal guardadas ("AlimentaciÃ³n" -> "Alimentación")
--
--  Ocurre cuando db_finanz.sql se cargó leyendo el archivo como Latin-1 en vez
--  de UTF-8 (versiones anteriores al "SET NAMES utf8mb4"). Este script corrige
--  solo las secuencias dañadas y NO borra datos (usuarios, movimientos, etc.).
--  Funciona también con textos mixtos (parte bien escrita y parte dañada), como
--  las alertas generadas a partir del nombre dañado de una categoría.
--
--  Uso:  mysql -h db -u root -pfinanz --default-character-set=utf8mb4 db_finanz < db/reparar_tildes.sql
-- =====================================================================

SET NAMES utf8mb4;

DROP FUNCTION IF EXISTS finanz_arreglar_texto;

DELIMITER //
-- Reemplaza cada secuencia dañada por el carácter correcto. Las secuencias se
-- escriben en hexadecimal (UTF-8) porque algunas incluyen caracteres invisibles.
CREATE FUNCTION finanz_arreglar_texto(t LONGTEXT) RETURNS LONGTEXT CHARSET utf8mb4
    DETERMINISTIC NO SQL
BEGIN
    IF t IS NULL THEN
        RETURN NULL;
    END IF;
    -- Secuencias de 3 bytes (comillas, guiones, viñetas, euro)
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACE284A2') USING utf8mb4), CONVERT(UNHEX('E28099') USING utf8mb4)); -- ’
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACCB9C') USING utf8mb4), CONVERT(UNHEX('E28098') USING utf8mb4));   -- ‘
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACC593') USING utf8mb4), CONVERT(UNHEX('E2809C') USING utf8mb4));   -- “
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACC29D') USING utf8mb4), CONVERT(UNHEX('E2809D') USING utf8mb4));   -- ”
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACE2809C') USING utf8mb4), CONVERT(UNHEX('E28093') USING utf8mb4)); -- –
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACE2809D') USING utf8mb4), CONVERT(UNHEX('E28094') USING utf8mb4)); -- —
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACC2A2') USING utf8mb4), CONVERT(UNHEX('E280A2') USING utf8mb4));   -- •
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E282ACC2A6') USING utf8mb4), CONVERT(UNHEX('E280A6') USING utf8mb4));   -- …
    SET t = REPLACE(t, CONVERT(UNHEX('C3A2E2809AC2AC') USING utf8mb4), CONVERT(UNHEX('E282AC') USING utf8mb4));   -- €
    -- Vocales con tilde, ñ, ü y signos españoles
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2A1') USING utf8mb4), CONVERT(UNHEX('C3A1') USING utf8mb4)); -- á
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2A9') USING utf8mb4), CONVERT(UNHEX('C3A9') USING utf8mb4)); -- é
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2AD') USING utf8mb4), CONVERT(UNHEX('C3AD') USING utf8mb4)); -- í
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2B3') USING utf8mb4), CONVERT(UNHEX('C3B3') USING utf8mb4)); -- ó
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2BA') USING utf8mb4), CONVERT(UNHEX('C3BA') USING utf8mb4)); -- ú
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2B1') USING utf8mb4), CONVERT(UNHEX('C3B1') USING utf8mb4)); -- ñ
    SET t = REPLACE(t, CONVERT(UNHEX('C383C2BC') USING utf8mb4), CONVERT(UNHEX('C3BC') USING utf8mb4)); -- ü
    SET t = REPLACE(t, CONVERT(UNHEX('C383C281') USING utf8mb4), CONVERT(UNHEX('C381') USING utf8mb4)); -- Á
    SET t = REPLACE(t, CONVERT(UNHEX('C383E280B0') USING utf8mb4), CONVERT(UNHEX('C389') USING utf8mb4)); -- É
    SET t = REPLACE(t, CONVERT(UNHEX('C383C28D') USING utf8mb4), CONVERT(UNHEX('C38D') USING utf8mb4)); -- Í
    SET t = REPLACE(t, CONVERT(UNHEX('C383E2809C') USING utf8mb4), CONVERT(UNHEX('C393') USING utf8mb4)); -- Ó
    SET t = REPLACE(t, CONVERT(UNHEX('C383C5A1') USING utf8mb4), CONVERT(UNHEX('C39A') USING utf8mb4)); -- Ú
    SET t = REPLACE(t, CONVERT(UNHEX('C383E28098') USING utf8mb4), CONVERT(UNHEX('C391') USING utf8mb4)); -- Ñ
    SET t = REPLACE(t, CONVERT(UNHEX('C383C593') USING utf8mb4), CONVERT(UNHEX('C39C') USING utf8mb4)); -- Ü
    SET t = REPLACE(t, CONVERT(UNHEX('C382C2BF') USING utf8mb4), CONVERT(UNHEX('C2BF') USING utf8mb4)); -- ¿
    SET t = REPLACE(t, CONVERT(UNHEX('C382C2A1') USING utf8mb4), CONVERT(UNHEX('C2A1') USING utf8mb4)); -- ¡
    SET t = REPLACE(t, CONVERT(UNHEX('C382C2B0') USING utf8mb4), CONVERT(UNHEX('C2B0') USING utf8mb4)); -- °
    SET t = REPLACE(t, CONVERT(UNHEX('C382C2B7') USING utf8mb4), CONVERT(UNHEX('C2B7') USING utf8mb4)); -- ·
    RETURN t;
END //
DELIMITER ;

-- Solo se tocan las filas que contienen secuencias dañadas ("Ã", "Â", "â€")
UPDATE categoria             SET nombre_categoria = finanz_arreglar_texto(nombre_categoria) WHERE nombre_categoria REGEXP 'Ã|Â|â€';
UPDATE usuario               SET nombres = finanz_arreglar_texto(nombres)         WHERE nombres REGEXP 'Ã|Â|â€';
UPDATE usuario               SET apellidos = finanz_arreglar_texto(apellidos)     WHERE apellidos REGEXP 'Ã|Â|â€';
UPDATE movimiento_financiero SET descripcion = finanz_arreglar_texto(descripcion) WHERE descripcion REGEXP 'Ã|Â|â€';
UPDATE ingreso               SET fuente = finanz_arreglar_texto(fuente)           WHERE fuente REGEXP 'Ã|Â|â€';
UPDATE meta_ahorro           SET nombre_meta = finanz_arreglar_texto(nombre_meta) WHERE nombre_meta REGEXP 'Ã|Â|â€';
UPDATE alerta                SET mensaje = finanz_arreglar_texto(mensaje)         WHERE mensaje REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo   SET titulo = finanz_arreglar_texto(titulo)           WHERE titulo REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo   SET descripcion = finanz_arreglar_texto(descripcion) WHERE descripcion REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo   SET contenido = finanz_arreglar_texto(contenido)     WHERE contenido REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo   SET tema = finanz_arreglar_texto(tema)               WHERE tema REGEXP 'Ã|Â|â€';
UPDATE cuestionario          SET titulo = finanz_arreglar_texto(titulo)           WHERE titulo REGEXP 'Ã|Â|â€';
UPDATE cuestionario          SET descripcion = finanz_arreglar_texto(descripcion) WHERE descripcion REGEXP 'Ã|Â|â€';
UPDATE pregunta              SET enunciado = finanz_arreglar_texto(enunciado)     WHERE enunciado REGEXP 'Ã|Â|â€';
UPDATE opcion                SET texto = finanz_arreglar_texto(texto)             WHERE texto REGEXP 'Ã|Â|â€';
UPDATE recomendacion         SET titulo = finanz_arreglar_texto(titulo)           WHERE titulo REGEXP 'Ã|Â|â€';
UPDATE recomendacion         SET descripcion = finanz_arreglar_texto(descripcion) WHERE descripcion REGEXP 'Ã|Â|â€';

DROP FUNCTION finanz_arreglar_texto;

SELECT 'Reparación terminada' AS resultado;
