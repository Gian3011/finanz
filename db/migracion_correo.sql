-- =====================================================================
--  FinanZ - Migración: envío del reporte por correo (RF21)
--
--  Para bases de datos db_finanz ya creadas con una versión anterior de
--  db_finanz.sql. No borra datos y se puede ejecutar varias veces.
--
--      mysql -u root -p db_finanz < db/migracion_correo.sql
--  En GitHub Codespaces lo aplica .devcontainer/iniciar.sh al arrancar.
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS envio_reporte (
    id_envio        INT           NOT NULL AUTO_INCREMENT,
    id_usuario      INT           NOT NULL,
    periodo         DATE          NOT NULL,
    formato         VARCHAR(10)   NOT NULL,
    correo_destino  VARCHAR(150)  NOT NULL,
    fecha_envio     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'ENVIADO',
    PRIMARY KEY (id_envio),
    KEY idx_envio_usuario_fecha (id_usuario, fecha_envio),
    CONSTRAINT fk_envio_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_envio_estado CHECK (estado IN ('ENVIADO', 'REBOTADO', 'ERROR'))
) ENGINE=InnoDB;
