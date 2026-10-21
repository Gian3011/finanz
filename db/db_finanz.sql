-- =====================================================================
--  FinanZ - Plataforma Web de Educación y Gestión Financiera Personal
--  Script de creación de la base de datos (MySQL 8.0)
--
--  Ejecutar completo en MySQL Workbench (botón del rayo) o con:
--      mysql -u root -p < db/db_finanz.sql
--
--  ATENCIÓN: elimina y vuelve a crear la base de datos db_finanz.
-- =====================================================================

DROP DATABASE IF EXISTS db_finanz;
CREATE DATABASE db_finanz CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE db_finanz;

-- ---------------------------------------------------------------------
-- USUARIO: datos personales y credenciales de acceso
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario         INT           NOT NULL AUTO_INCREMENT,
    nombres            VARCHAR(100)  NOT NULL,
    apellidos          VARCHAR(100)  NOT NULL,
    email              VARCHAR(150)  NOT NULL,
    password_hash      VARCHAR(300)  NOT NULL,
    fecha_registro     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    intentos_fallidos  INT           NOT NULL DEFAULT 0,
    bloqueado_hasta    DATETIME      NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- CATEGORIA: id_usuario NULL = categoría predeterminada (visible para todos);
-- con id_usuario = categoría personalizada de ese usuario.
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id_categoria      INT          NOT NULL AUTO_INCREMENT,
    id_usuario        INT          NULL,
    nombre_categoria  VARCHAR(50)  NOT NULL,
    tipo              VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_categoria),
    KEY fk_categoria_usuario_idx (id_usuario),
    CONSTRAINT fk_categoria_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_categoria_tipo CHECK (tipo IN ('INGRESO', 'GASTO'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- MOVIMIENTO_FINANCIERO: datos comunes de ingresos y gastos
-- ---------------------------------------------------------------------
CREATE TABLE movimiento_financiero (
    id_movimiento    INT            NOT NULL AUTO_INCREMENT,
    id_usuario       INT            NOT NULL,
    id_categoria     INT            NOT NULL,
    monto            DECIMAL(10,2)  NOT NULL,
    fecha            DATE           NOT NULL,
    descripcion      VARCHAR(150)   NULL,
    tipo_movimiento  VARCHAR(20)    NOT NULL,
    PRIMARY KEY (id_movimiento),
    KEY idx_mov_usuario_fecha (id_usuario, fecha),
    KEY fk_mov_categoria_idx (id_categoria),
    CONSTRAINT fk_mov_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_mov_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria (id_categoria),
    CONSTRAINT ck_mov_tipo CHECK (tipo_movimiento IN ('INGRESO', 'GASTO')),
    CONSTRAINT ck_mov_monto CHECK (monto > 0)
) ENGINE=InnoDB;

-- INGRESO: especialización de movimiento_financiero (entradas de dinero)
CREATE TABLE ingreso (
    id_movimiento  INT           NOT NULL,
    fuente         VARCHAR(100)  NOT NULL,
    PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_ingreso_mov FOREIGN KEY (id_movimiento)
        REFERENCES movimiento_financiero (id_movimiento) ON DELETE CASCADE
) ENGINE=InnoDB;

-- GASTO: especialización de movimiento_financiero (salidas de dinero)
CREATE TABLE gasto (
    id_movimiento     INT         NOT NULL,
    es_gasto_hormiga  TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_gasto_mov FOREIGN KEY (id_movimiento)
        REFERENCES movimiento_financiero (id_movimiento) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- PRESUPUESTO: monto mensual (periodo = primer día del mes)
-- ---------------------------------------------------------------------
CREATE TABLE presupuesto (
    id_presupuesto     INT            NOT NULL AUTO_INCREMENT,
    id_usuario         INT            NOT NULL,
    periodo            DATE           NOT NULL,
    monto_presupuesto  DECIMAL(10,2)  NOT NULL,
    PRIMARY KEY (id_presupuesto),
    UNIQUE KEY uk_presupuesto_usuario_periodo (id_usuario, periodo),
    CONSTRAINT fk_presupuesto_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_presupuesto_monto CHECK (monto_presupuesto > 0)
) ENGINE=InnoDB;

-- LIMITE_GASTO: tope por categoría dentro de un presupuesto
CREATE TABLE limite_gasto (
    id_limite       INT            NOT NULL AUTO_INCREMENT,
    id_presupuesto  INT            NOT NULL,
    id_categoria    INT            NOT NULL,
    monto_limite    DECIMAL(10,2)  NOT NULL,
    PRIMARY KEY (id_limite),
    UNIQUE KEY uk_limite_presupuesto_categoria (id_presupuesto, id_categoria),
    KEY fk_limite_categoria_idx (id_categoria),
    CONSTRAINT fk_limite_presupuesto FOREIGN KEY (id_presupuesto)
        REFERENCES presupuesto (id_presupuesto) ON DELETE CASCADE,
    CONSTRAINT fk_limite_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria (id_categoria) ON DELETE CASCADE,
    CONSTRAINT ck_limite_monto CHECK (monto_limite > 0)
) ENGINE=InnoDB;

-- ALERTA: avisos al acercarse (80%) o superar el presupuesto / un límite
CREATE TABLE alerta (
    id_alerta       INT           NOT NULL AUTO_INCREMENT,
    id_usuario      INT           NOT NULL,
    id_presupuesto  INT           NULL,
    id_limite       INT           NULL,
    nivel           VARCHAR(20)   NOT NULL,
    mensaje         VARCHAR(250)  NOT NULL,
    fecha           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    PRIMARY KEY (id_alerta),
    KEY idx_alerta_usuario_estado (id_usuario, estado),
    CONSTRAINT fk_alerta_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_alerta_presupuesto FOREIGN KEY (id_presupuesto)
        REFERENCES presupuesto (id_presupuesto) ON DELETE CASCADE,
    CONSTRAINT fk_alerta_limite FOREIGN KEY (id_limite)
        REFERENCES limite_gasto (id_limite) ON DELETE CASCADE,
    CONSTRAINT ck_alerta_nivel CHECK (nivel IN ('CERCA', 'SUPERADO')),
    CONSTRAINT ck_alerta_estado CHECK (estado IN ('PENDIENTE', 'LEIDO'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- META_AHORRO y APORTE_AHORRO
-- ---------------------------------------------------------------------
CREATE TABLE meta_ahorro (
    id_meta          INT            NOT NULL AUTO_INCREMENT,
    id_usuario       INT            NOT NULL,
    nombre_meta      VARCHAR(100)   NOT NULL,
    monto_objetivo   DECIMAL(10,2)  NOT NULL,
    fecha_objetivo   DATE           NOT NULL,
    estado           VARCHAR(20)    NOT NULL DEFAULT 'EN_PROGRESO',
    fecha_creacion   DATE           NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (id_meta),
    KEY fk_meta_usuario_idx (id_usuario),
    CONSTRAINT fk_meta_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_meta_estado CHECK (estado IN ('EN_PROGRESO', 'CUMPLIDA')),
    CONSTRAINT ck_meta_monto CHECK (monto_objetivo > 0)
) ENGINE=InnoDB;

CREATE TABLE aporte_ahorro (
    id_aporte     INT            NOT NULL AUTO_INCREMENT,
    id_meta       INT            NOT NULL,
    monto_aporte  DECIMAL(10,2)  NOT NULL,
    fecha_aporte  DATE           NOT NULL,
    PRIMARY KEY (id_aporte),
    KEY fk_aporte_meta_idx (id_meta),
    CONSTRAINT fk_aporte_meta FOREIGN KEY (id_meta)
        REFERENCES meta_ahorro (id_meta) ON DELETE CASCADE,
    CONSTRAINT ck_aporte_monto CHECK (monto_aporte > 0)
) ENGINE=InnoDB;

-- SIMULACION_AHORRO: escenarios calculados por el simulador
CREATE TABLE simulacion_ahorro (
    id_simulacion     INT            NOT NULL AUTO_INCREMENT,
    id_usuario        INT            NOT NULL,
    monto_objetivo    DECIMAL(10,2)  NOT NULL,
    monto_inicial     DECIMAL(10,2)  NOT NULL DEFAULT 0,
    aporte_periodico  DECIMAL(10,2)  NOT NULL,
    frecuencia        VARCHAR(20)    NOT NULL,
    numero_periodos   INT            NOT NULL,
    fecha_objetivo    DATE           NOT NULL,
    fecha_creacion    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_simulacion),
    KEY fk_simulacion_usuario_idx (id_usuario),
    CONSTRAINT fk_simulacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_simulacion_frecuencia CHECK (frecuencia IN ('SEMANAL', 'QUINCENAL', 'MENSUAL'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- EDUCACIÓN FINANCIERA
-- ---------------------------------------------------------------------
CREATE TABLE contenido_educativo (
    id_contenido  INT           NOT NULL AUTO_INCREMENT,
    titulo        VARCHAR(150)  NOT NULL,
    descripcion   VARCHAR(250)  NOT NULL,
    contenido     TEXT          NOT NULL,
    tema          VARCHAR(50)   NOT NULL,
    PRIMARY KEY (id_contenido)
) ENGINE=InnoDB;

CREATE TABLE cuestionario (
    id_cuestionario  INT           NOT NULL AUTO_INCREMENT,
    id_contenido     INT           NOT NULL,
    titulo           VARCHAR(150)  NOT NULL,
    descripcion      VARCHAR(250)  NULL,
    PRIMARY KEY (id_cuestionario),
    CONSTRAINT fk_cuestionario_contenido FOREIGN KEY (id_contenido)
        REFERENCES contenido_educativo (id_contenido) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE pregunta (
    id_pregunta      INT           NOT NULL AUTO_INCREMENT,
    id_cuestionario  INT           NOT NULL,
    enunciado        VARCHAR(300)  NOT NULL,
    PRIMARY KEY (id_pregunta),
    CONSTRAINT fk_pregunta_cuestionario FOREIGN KEY (id_cuestionario)
        REFERENCES cuestionario (id_cuestionario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE opcion (
    id_opcion    INT           NOT NULL AUTO_INCREMENT,
    id_pregunta  INT           NOT NULL,
    texto        VARCHAR(250)  NOT NULL,
    es_correcta  TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id_opcion),
    CONSTRAINT fk_opcion_pregunta FOREIGN KEY (id_pregunta)
        REFERENCES pregunta (id_pregunta) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE resultado_cuestionario (
    id_resultado     INT       NOT NULL AUTO_INCREMENT,
    id_usuario       INT       NOT NULL,
    id_cuestionario  INT       NOT NULL,
    puntaje          INT       NOT NULL,
    total_preguntas  INT       NOT NULL,
    fecha            DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_resultado),
    KEY idx_resultado_usuario (id_usuario),
    CONSTRAINT fk_resultado_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_resultado_cuestionario FOREIGN KEY (id_cuestionario)
        REFERENCES cuestionario (id_cuestionario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- REPORTES Y RECOMENDACIONES
-- ---------------------------------------------------------------------
CREATE TABLE reporte_financiero (
    id_reporte        INT          NOT NULL AUTO_INCREMENT,
    id_usuario        INT          NOT NULL,
    fecha_generacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    periodo           VARCHAR(30)  NOT NULL,
    formato           VARCHAR(10)  NOT NULL DEFAULT 'PDF',
    PRIMARY KEY (id_reporte),
    CONSTRAINT fk_reporte_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE recomendacion (
    id_recomendacion  INT           NOT NULL AUTO_INCREMENT,
    id_usuario        INT           NOT NULL,
    titulo            VARCHAR(100)  NOT NULL,
    descripcion       TEXT          NOT NULL,
    tipo              VARCHAR(30)   NOT NULL,
    fecha             DATE          NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (id_recomendacion),
    KEY idx_recomendacion_usuario_fecha (id_usuario, fecha),
    CONSTRAINT fk_recomendacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =====================================================================
--  DATOS INICIALES
-- =====================================================================

-- Categorías predeterminadas (id_usuario NULL = disponibles para todos)
INSERT INTO categoria (id_categoria, id_usuario, nombre_categoria, tipo) VALUES
 (1,  NULL, 'Alimentación',      'GASTO'),
 (2,  NULL, 'Transporte',        'GASTO'),
 (3,  NULL, 'Entretenimiento',   'GASTO'),
 (4,  NULL, 'Educación',         'GASTO'),
 (5,  NULL, 'Salud',             'GASTO'),
 (6,  NULL, 'Vivienda',          'GASTO'),
 (7,  NULL, 'Servicios',         'GASTO'),
 (8,  NULL, 'Snacks y café',     'GASTO'),
 (9,  NULL, 'Suscripciones',     'GASTO'),
 (10, NULL, 'Otros gastos',      'GASTO'),
 (11, NULL, 'Sueldo',            'INGRESO'),
 (12, NULL, 'Trabajos extra',    'INGRESO'),
 (13, NULL, 'Otros ingresos',    'INGRESO');

-- Usuario de demostración: juan@correo.com / Finanz2026
-- (la contraseña se guarda con PBKDF2-HMAC-SHA256 y sal aleatoria)
INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash) VALUES
 (1, 'Juan', 'Pérez', 'juan@correo.com',
  'pbkdf2$65536$2Q0M10dx/DZ1745PRsHdhQ==$CtTDlfzCD0TNDoK4pq3AM7wqNSUWrEDCZb4L33XJ9EE=');

-- Movimientos de los últimos meses (fechas relativas a hoy, para que la demo siempre esté al día)
INSERT INTO movimiento_financiero (id_movimiento, id_usuario, id_categoria, monto, fecha, descripcion, tipo_movimiento) VALUES
 -- Mes actual
 (1,  1, 11, 2500.00, DATE_FORMAT(CURDATE(), '%Y-%m-01'),                          'Sueldo del mes',           'INGRESO'),
 (2,  1, 6,   800.00, DATE_FORMAT(CURDATE(), '%Y-%m-01'),                          'Alquiler de habitación',   'GASTO'),
 (3,  1, 1,   180.50, DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 DAY), 'Compras del mercado',      'GASTO'),
 (4,  1, 2,    95.00, DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 3 DAY), 'Recarga tarjeta de bus',   'GASTO'),
 (5,  1, 7,   120.00, DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 4 DAY), 'Luz, agua e internet',     'GASTO'),
 (6,  1, 9,    44.90, DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 5 DAY), 'Streaming de películas',   'GASTO'),
 (7,  1, 3,    78.00, DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 6 DAY), 'Salida al cine',           'GASTO'),
 (8,  1, 12,  350.00, DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 7 DAY), 'Diseño de logo (freelance)', 'INGRESO'),
 (9,  1, 8,     8.50, DATE_SUB(CURDATE(), INTERVAL 12 DAY), 'Café de la mañana',  'GASTO'),
 (10, 1, 8,     6.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'Galletas y gaseosa', 'GASTO'),
 (11, 1, 8,     9.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY),  'Café de la mañana',  'GASTO'),
 (12, 1, 8,     7.50, DATE_SUB(CURDATE(), INTERVAL 6 DAY),  'Snack en la universidad', 'GASTO'),
 (13, 1, 8,     8.50, DATE_SUB(CURDATE(), INTERVAL 4 DAY),  'Café de la mañana',  'GASTO'),
 (14, 1, 8,    12.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY),  'Frappé',             'GASTO'),
 (15, 1, 1,    25.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY),  'Menú del día',       'GASTO'),
 -- Mes anterior
 (16, 1, 11, 2500.00, DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), 'Sueldo del mes', 'INGRESO'),
 (17, 1, 6,   800.00, DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), 'Alquiler de habitación', 'GASTO'),
 (18, 1, 1,   420.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), INTERVAL 5 DAY),  'Compras del mes', 'GASTO'),
 (19, 1, 2,   140.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), INTERVAL 6 DAY),  'Taxis y bus', 'GASTO'),
 (20, 1, 3,   210.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), INTERVAL 12 DAY), 'Concierto', 'GASTO'),
 (21, 1, 7,   118.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), INTERVAL 4 DAY),  'Luz, agua e internet', 'GASTO'),
 (22, 1, 4,   250.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), INTERVAL 15 DAY), 'Curso de Excel', 'GASTO'),
 (23, 1, 9,    44.90, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH), INTERVAL 5 DAY),  'Streaming de películas', 'GASTO'),
 -- Hace dos meses
 (24, 1, 11, 2500.00, DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH), 'Sueldo del mes', 'INGRESO'),
 (25, 1, 6,   800.00, DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH), 'Alquiler de habitación', 'GASTO'),
 (26, 1, 1,   390.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH), INTERVAL 5 DAY), 'Compras del mes', 'GASTO'),
 (27, 1, 2,   130.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH), INTERVAL 6 DAY), 'Taxis y bus', 'GASTO'),
 (28, 1, 5,   160.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH), INTERVAL 9 DAY), 'Consulta médica', 'GASTO'),
 (29, 1, 7,   121.00, DATE_ADD(DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH), INTERVAL 4 DAY), 'Luz, agua e internet', 'GASTO');

INSERT INTO ingreso (id_movimiento, fuente) VALUES
 (1, 'Empleador'), (8, 'Cliente freelance'), (16, 'Empleador'), (24, 'Empleador');

INSERT INTO gasto (id_movimiento, es_gasto_hormiga)
SELECT id_movimiento, CASE WHEN id_categoria = 8 THEN 1 ELSE 0 END
FROM movimiento_financiero WHERE tipo_movimiento = 'GASTO';

-- Presupuesto del mes actual con límites por categoría
INSERT INTO presupuesto (id_presupuesto, id_usuario, periodo, monto_presupuesto) VALUES
 (1, 1, DATE_FORMAT(CURDATE(), '%Y-%m-01'), 1800.00);

INSERT INTO limite_gasto (id_presupuesto, id_categoria, monto_limite) VALUES
 (1, 1, 450.00), (1, 2, 150.00), (1, 3, 120.00), (1, 8, 40.00);

-- Metas de ahorro
INSERT INTO meta_ahorro (id_meta, id_usuario, nombre_meta, monto_objetivo, fecha_objetivo, fecha_creacion) VALUES
 (1, 1, 'Fondo de emergencia', 3000.00, DATE_ADD(CURDATE(), INTERVAL 8 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH)),
 (2, 1, 'Laptop nueva',        2800.00, DATE_ADD(CURDATE(), INTERVAL 5 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH));

INSERT INTO aporte_ahorro (id_meta, monto_aporte, fecha_aporte) VALUES
 (1, 300.00, DATE_SUB(CURDATE(), INTERVAL 55 DAY)),
 (1, 300.00, DATE_SUB(CURDATE(), INTERVAL 25 DAY)),
 (1, 250.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
 (2, 400.00, DATE_SUB(CURDATE(), INTERVAL 20 DAY));

-- ---------------------------------------------------------------------
-- Contenido educativo
-- ---------------------------------------------------------------------
INSERT INTO contenido_educativo (id_contenido, titulo, descripcion, contenido, tema) VALUES
(1, 'Cómo armar tu primer presupuesto',
 'Aprende a organizar tus ingresos y gastos del mes con la regla 50/30/20.',
 'Un presupuesto es un plan que indica en qué vas a usar tu dinero antes de gastarlo. No sirve para limitarte, sino para que tu dinero vaya primero a lo que más te importa.\n\nPaso 1. Calcula tu ingreso neto mensual: lo que realmente recibes después de descuentos.\n\nPaso 2. Lista tus gastos fijos (alquiler, transporte, servicios) y tus gastos variables (comida fuera, ocio, compras).\n\nPaso 3. Aplica la regla 50/30/20 como punto de partida:\n• 50 % para necesidades: vivienda, alimentación básica, transporte, servicios.\n• 30 % para deseos: salidas, suscripciones, ropa que no es indispensable.\n• 20 % para ahorro y pago de deudas.\n\nPaso 4. Registra tus movimientos durante el mes y compáralos con lo planificado. En FinanZ puedes definir un presupuesto mensual y límites por categoría para que el sistema te avise cuando llegues al 80 % o te pases.\n\nPaso 5. Ajusta. El primer presupuesto casi nunca sale perfecto; lo importante es revisarlo cada mes.',
 'Presupuesto'),
(2, 'Gastos hormiga: el enemigo silencioso',
 'Descubre qué son los gastos hormiga y cómo afectan tu capacidad de ahorro.',
 'Los gastos hormiga son pequeños desembolsos que hacemos con frecuencia y casi sin pensar: el café de la mañana, un snack, una app, el delivery por comodidad.\n\nPor separado parecen insignificantes, pero sumados pesan. Un café de S/ 8 cinco días a la semana son S/ 160 al mes y casi S/ 1 920 al año.\n\nCómo identificarlos:\n• Revisa tus movimientos de las últimas semanas y busca montos pequeños que se repiten.\n• FinanZ marca como posible gasto hormiga un gasto menor a S/ 20 cuando ya registraste 5 o más gastos en la misma categoría durante los últimos 30 días.\n\nCómo reducirlos:\n• Ponte un tope semanal para esos gustos.\n• Prepara en casa lo que compras con más frecuencia.\n• Cancela suscripciones que no usas.\n• Antes de comprar, pregúntate si lo necesitas o solo es costumbre.\n\nNo se trata de eliminar todo gusto, sino de decidir de forma consciente.',
 'Hábitos'),
(3, 'Metas de ahorro SMART',
 'Convierte tus deseos en metas de ahorro concretas y alcanzables.',
 'Ahorrar es más fácil cuando sabes para qué lo haces. Una meta SMART es:\n\n• Específica: "Ahorrar para una laptop", no solo "ahorrar".\n• Medible: tiene un monto objetivo, por ejemplo S/ 2 800.\n• Alcanzable: acorde a tus ingresos.\n• Relevante: importante para ti.\n• Con tiempo definido: una fecha límite.\n\nPara calcular cuánto aportar, divide lo que te falta entre los meses que quedan. Si te faltan S/ 2 400 y tienes 6 meses, necesitas S/ 400 al mes.\n\nConsejos:\n• Ahorra apenas recibes tu ingreso ("págate primero").\n• Empieza por un fondo de emergencia equivalente a 3 a 6 meses de gastos básicos.\n• Usa el simulador de FinanZ para probar distintos montos y frecuencias de aporte.',
 'Ahorro'),
(4, 'Fondo de emergencia',
 'Por qué necesitas un colchón financiero y cómo construirlo paso a paso.',
 'Un fondo de emergencia es dinero reservado solo para imprevistos: una enfermedad, la pérdida del empleo, una reparación urgente.\n\n¿Cuánto debe tener? Lo recomendable es cubrir entre 3 y 6 meses de tus gastos esenciales. Si gastas S/ 1 500 al mes en lo básico, tu meta estaría entre S/ 4 500 y S/ 9 000.\n\n¿Dónde guardarlo? En un lugar seguro y de fácil acceso, separado de tu cuenta de gastos diarios, por ejemplo una cuenta de ahorros.\n\n¿Cómo empezar?\n1. Define una meta inicial pequeña (por ejemplo, un mes de gastos).\n2. Aporta un monto fijo cada vez que cobras.\n3. Si lo usas, vuelve a reponerlo.\n\nTener este fondo evita endeudarse con tarjetas o préstamos caros cuando aparece un imprevisto.',
 'Ahorro'),
(5, 'Uso responsable de la tarjeta de crédito',
 'Entiende cómo funcionan los intereses y evita caer en deudas costosas.',
 'La tarjeta de crédito es un préstamo: el banco paga por ti y luego tú le devuelves el dinero. Si no pagas el total a tiempo, se cobran intereses que pueden ser muy altos.\n\nConceptos clave:\n• Fecha de corte: día en que se cierra el periodo de consumos.\n• Fecha de pago: último día para pagar sin intereses.\n• Pago mínimo: el monto más bajo que puedes pagar; pagar solo eso genera intereses sobre el saldo restante.\n• TCEA: costo total anual del crédito, útil para comparar tarjetas.\n\nBuenas prácticas:\n• Paga siempre el total del mes.\n• No uses más del 30 % de tu línea de crédito.\n• Evita disposiciones de efectivo; suelen tener la tasa más alta.\n• Registra tus compras con tarjeta como gastos en FinanZ para no perder el control.',
 'Crédito');

INSERT INTO cuestionario (id_cuestionario, id_contenido, titulo, descripcion) VALUES
 (1, 1, 'Quiz: presupuesto personal', 'Responde 4 preguntas sobre cómo armar un presupuesto.'),
 (2, 2, 'Quiz: gastos hormiga',       'Comprueba si sabes identificar y reducir gastos hormiga.'),
 (3, 3, 'Quiz: metas de ahorro',      'Pon a prueba lo aprendido sobre metas SMART.'),
 (4, 5, 'Quiz: tarjeta de crédito',   'Verifica tus conocimientos sobre el uso de la tarjeta.');

INSERT INTO pregunta (id_pregunta, id_cuestionario, enunciado) VALUES
 (1, 1, 'Según la regla 50/30/20, ¿qué porcentaje del ingreso se destina al ahorro y pago de deudas?'),
 (2, 1, '¿Cuál de estos es un gasto fijo?'),
 (3, 1, '¿Qué ingreso debes usar para armar tu presupuesto?'),
 (4, 1, '¿Cada cuánto conviene revisar el presupuesto?'),
 (5, 2, '¿Qué caracteriza a un gasto hormiga?'),
 (6, 2, 'Si gastas S/ 8 en café cinco días a la semana, ¿cuánto gastas aproximadamente al mes?'),
 (7, 2, '¿Cuál es una buena estrategia para reducir gastos hormiga?'),
 (8, 3, '¿Qué significa la "M" de una meta SMART?'),
 (9, 3, 'Te faltan S/ 1 200 para tu meta y quedan 4 meses. ¿Cuánto debes ahorrar al mes?'),
 (10, 3, '¿Qué significa "págate primero"?'),
 (11, 4, '¿Qué ocurre si solo realizas el pago mínimo de la tarjeta?'),
 (12, 4, '¿Qué porcentaje máximo de la línea de crédito se recomienda usar?'),
 (13, 4, '¿Para qué sirve la TCEA?');

INSERT INTO opcion (id_pregunta, texto, es_correcta) VALUES
 (1, '50 %', 0), (1, '30 %', 0), (1, '20 %', 1), (1, '10 %', 0),
 (2, 'El alquiler de la vivienda', 1), (2, 'Una salida al cine', 0), (2, 'Un antojo en la universidad', 0),
 (3, 'El ingreso bruto antes de descuentos', 0), (3, 'El ingreso neto que realmente recibes', 1), (3, 'El ingreso que esperas tener el próximo año', 0),
 (4, 'Una vez al año', 0), (4, 'Solo cuando sobra dinero', 0), (4, 'Cada mes', 1),
 (5, 'Es un gasto grande y planificado', 0), (5, 'Es un gasto pequeño, frecuente y poco planificado', 1), (5, 'Es el pago de una deuda', 0),
 (6, 'S/ 40', 0), (6, 'S/ 80', 0), (6, 'S/ 160', 1), (6, 'S/ 400', 0),
 (7, 'Ignorarlos porque son montos pequeños', 0), (7, 'Ponerles un tope semanal y registrarlos', 1), (7, 'Pagarlos siempre con tarjeta de crédito', 0),
 (8, 'Mensual', 0), (8, 'Medible', 1), (8, 'Mínima', 0),
 (9, 'S/ 200', 0), (9, 'S/ 300', 1), (9, 'S/ 400', 0), (9, 'S/ 1 200', 0),
 (10, 'Ahorrar apenas recibes tu ingreso, antes de gastar', 1), (10, 'Comprarte un gusto antes de pagar las cuentas', 0), (10, 'Pagar primero tus deudas más pequeñas', 0),
 (11, 'No pasa nada, es lo recomendado', 0), (11, 'Se cobran intereses sobre el saldo pendiente', 1), (11, 'El banco cancela la deuda', 0),
 (12, '30 %', 1), (12, '70 %', 0), (12, '100 %', 0),
 (13, 'Para comparar el costo total de distintos créditos', 1), (13, 'Para saber la fecha de corte', 0), (13, 'Para calcular el pago mínimo', 0);
