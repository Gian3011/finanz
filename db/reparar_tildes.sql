-- =====================================================================
--  FinanZ - Reparación de tildes mal guardadas ("AlimentaciÃ³n" -> "Alimentación")
--
--  Ocurre cuando db_finanz.sql se cargó leyendo el archivo como Latin-1 en vez
--  de UTF-8 (versiones anteriores al "SET NAMES utf8mb4"). Este script corrige
--  solo los textos dañados y NO borra datos (usuarios, movimientos, etc.).
--
--  Uso:  mysql -h db -u root -pfinanz --default-character-set=utf8mb4 db_finanz < db/reparar_tildes.sql
-- =====================================================================

SET NAMES utf8mb4;

-- Un texto dañado contiene secuencias como "Ã", "Â" o "â€"; se reinterpreta
-- su contenido como bytes Latin-1 y se vuelve a leer como UTF-8.
UPDATE categoria SET nombre_categoria = CONVERT(CAST(CONVERT(nombre_categoria USING latin1) AS BINARY) USING utf8mb4)
 WHERE nombre_categoria REGEXP 'Ã|Â|â€';

UPDATE usuario SET nombres = CONVERT(CAST(CONVERT(nombres USING latin1) AS BINARY) USING utf8mb4)
 WHERE nombres REGEXP 'Ã|Â|â€';
UPDATE usuario SET apellidos = CONVERT(CAST(CONVERT(apellidos USING latin1) AS BINARY) USING utf8mb4)
 WHERE apellidos REGEXP 'Ã|Â|â€';

UPDATE movimiento_financiero SET descripcion = CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4)
 WHERE descripcion REGEXP 'Ã|Â|â€';
UPDATE ingreso SET fuente = CONVERT(CAST(CONVERT(fuente USING latin1) AS BINARY) USING utf8mb4)
 WHERE fuente REGEXP 'Ã|Â|â€';

UPDATE meta_ahorro SET nombre_meta = CONVERT(CAST(CONVERT(nombre_meta USING latin1) AS BINARY) USING utf8mb4)
 WHERE nombre_meta REGEXP 'Ã|Â|â€';

UPDATE alerta SET mensaje = CONVERT(CAST(CONVERT(mensaje USING latin1) AS BINARY) USING utf8mb4)
 WHERE mensaje REGEXP 'Ã|Â|â€';

UPDATE contenido_educativo SET titulo = CONVERT(CAST(CONVERT(titulo USING latin1) AS BINARY) USING utf8mb4)
 WHERE titulo REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo SET descripcion = CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4)
 WHERE descripcion REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo SET contenido = CONVERT(CAST(CONVERT(contenido USING latin1) AS BINARY) USING utf8mb4)
 WHERE contenido REGEXP 'Ã|Â|â€';
UPDATE contenido_educativo SET tema = CONVERT(CAST(CONVERT(tema USING latin1) AS BINARY) USING utf8mb4)
 WHERE tema REGEXP 'Ã|Â|â€';

UPDATE cuestionario SET titulo = CONVERT(CAST(CONVERT(titulo USING latin1) AS BINARY) USING utf8mb4)
 WHERE titulo REGEXP 'Ã|Â|â€';
UPDATE cuestionario SET descripcion = CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4)
 WHERE descripcion REGEXP 'Ã|Â|â€';

UPDATE pregunta SET enunciado = CONVERT(CAST(CONVERT(enunciado USING latin1) AS BINARY) USING utf8mb4)
 WHERE enunciado REGEXP 'Ã|Â|â€';
UPDATE opcion SET texto = CONVERT(CAST(CONVERT(texto USING latin1) AS BINARY) USING utf8mb4)
 WHERE texto REGEXP 'Ã|Â|â€';

UPDATE recomendacion SET titulo = CONVERT(CAST(CONVERT(titulo USING latin1) AS BINARY) USING utf8mb4)
 WHERE titulo REGEXP 'Ã|Â|â€';
UPDATE recomendacion SET descripcion = CONVERT(CAST(CONVERT(descripcion USING latin1) AS BINARY) USING utf8mb4)
 WHERE descripcion REGEXP 'Ã|Â|â€';

SELECT 'Reparación terminada' AS resultado;
