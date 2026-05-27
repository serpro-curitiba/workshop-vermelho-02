-- SIFAP legado -> PostgreSQL
-- Artefato de arqueologia: modelo relacional inicial derivado dos DDMs Adabas.
-- Este arquivo documenta um ponto de partida para a implementacao moderna.

CREATE SCHEMA IF NOT EXISTS sifap;

SET search_path TO sifap, public;

CREATE TABLE IF NOT EXISTS programa_social (
    cod_programa VARCHAR(4) PRIMARY KEY,
    nome_programa VARCHAR(60) NOT NULL,
    sigla_programa VARCHAR(10),
    tipo_programa CHAR(1),
    orgao_responsavel VARCHAR(10),
    lei_criacao VARCHAR(20),
    dt_criacao DATE,
    dt_encerramento DATE,
    sit_programa CHAR(1) NOT NULL,
    vlr_base_individual NUMERIC(9, 2),
    vlr_base_familiar NUMERIC(9, 2),
    vlr_teto_benef NUMERIC(11, 2),
    vlr_piso_benef NUMERIC(9, 2),
    pct_reajuste_anual NUMERIC(5, 2),
    dt_ult_reajuste DATE,
    fator_k NUMERIC(9, 4),
    renda_max_percap NUMERIC(9, 2),
    idade_min SMALLINT,
    idade_max SMALLINT,
    ind_exige_filhos CHAR(1),
    qtd_min_filhos SMALLINT,
    ind_exige_escola CHAR(1),
    ind_exige_vacina CHAR(1),
    ind_exige_prenatal CHAR(1),
    ind_exige_biometria CHAR(1),
    dt_inclusao DATE,
    usr_inclusao VARCHAR(8),
    dt_ult_alteracao DATE,
    usr_ult_alteracao VARCHAR(8),
    CONSTRAINT ck_programa_tipo CHECK (tipo_programa IN ('A', 'T', 'P')),
    CONSTRAINT ck_programa_situacao CHECK (sit_programa IN ('A', 'I', 'E'))
);

COMMENT ON COLUMN programa_social.fator_k IS 'MYSTERY: campo identificado no mapeamento, mas sem regra de negocio conclusiva no legado.';

CREATE TABLE IF NOT EXISTS programa_social_faixa_calculo (
    id_faixa BIGSERIAL PRIMARY KEY,
    cod_programa VARCHAR(4) NOT NULL REFERENCES programa_social(cod_programa),
    seq_faixa SMALLINT NOT NULL,
    renda_inicio NUMERIC(9, 2),
    renda_fim NUMERIC(9, 2),
    fator_multiplicador NUMERIC(9, 4),
    vlr_adicional NUMERIC(9, 2),
    ind_acumulativo CHAR(1),
    CONSTRAINT uq_programa_faixa UNIQUE (cod_programa, seq_faixa),
    CONSTRAINT ck_faixa_acumulativa CHECK (ind_acumulativo IN ('S', 'N'))
);

CREATE TABLE IF NOT EXISTS programa_social_parametro_regional (
    id_parametro_regional BIGSERIAL PRIMARY KEY,
    cod_programa VARCHAR(4) NOT NULL REFERENCES programa_social(cod_programa),
    cod_regiao VARCHAR(2) NOT NULL,
    fator_regional NUMERIC(9, 4),
    vlr_complemento_reg NUMERIC(9, 2),
    ind_ativo_regiao CHAR(1),
    CONSTRAINT uq_programa_regiao UNIQUE (cod_programa, cod_regiao),
    CONSTRAINT ck_regiao_ativa CHECK (ind_ativo_regiao IN ('S', 'N'))
);

CREATE TABLE IF NOT EXISTS programa_social_tipo_desconto (
    id_tipo_desconto BIGSERIAL PRIMARY KEY,
    cod_programa VARCHAR(4) NOT NULL REFERENCES programa_social(cod_programa),
    seq_tipo SMALLINT NOT NULL,
    tipo_desconto VARCHAR(3) NOT NULL,
    CONSTRAINT uq_programa_tipo_desconto UNIQUE (cod_programa, seq_tipo)
);

CREATE TABLE IF NOT EXISTS beneficiario (
    num_inscricao BIGINT PRIMARY KEY,
    num_cpf VARCHAR(11) NOT NULL,
    nome_completo VARCHAR(60),
    nome_mae VARCHAR(60),
    nome_pai VARCHAR(60),
    dt_nascimento DATE,
    sexo CHAR(1),
    est_civil CHAR(1),
    rg_numero VARCHAR(15),
    rg_orgao VARCHAR(10),
    rg_uf CHAR(2),
    rg_dt_expedicao DATE,
    logradouro VARCHAR(60),
    numero VARCHAR(10),
    complemento VARCHAR(30),
    bairro VARCHAR(40),
    municipio VARCHAR(40),
    uf CHAR(2) NOT NULL,
    cep VARCHAR(8),
    cod_ibge_municipio NUMERIC(7),
    cod_regiao VARCHAR(2),
    cod_programa VARCHAR(4) REFERENCES programa_social(cod_programa),
    dt_cadastro DATE NOT NULL,
    dt_inicio_benef DATE,
    dt_fim_benef DATE,
    sit_beneficiario CHAR(1) NOT NULL,
    mot_situacao VARCHAR(3),
    dt_ult_situacao DATE,
    vlr_renda_familiar NUMERIC(11, 2),
    qtd_membros_familia SMALLINT,
    renda_percapita NUMERIC(9, 2),
    tel_fixo VARCHAR(14),
    tel_celular VARCHAR(15),
    email VARCHAR(80),
    ind_biometria CHAR(1),
    dt_coleta_bio DATE,
    cod_posto_bio VARCHAR(6),
    hash_digital VARCHAR(64),
    dt_inclusao DATE,
    hr_inclusao TIME,
    usr_inclusao VARCHAR(8),
    dt_ult_alteracao DATE,
    hr_ult_alteracao TIME,
    usr_ult_alteracao VARCHAR(8),
    num_versao SMALLINT DEFAULT 1,
    CONSTRAINT uq_beneficiario_cpf UNIQUE (num_cpf),
    CONSTRAINT ck_beneficiario_sexo CHECK (sexo IN ('M', 'F', 'I')),
    CONSTRAINT ck_beneficiario_estado_civil CHECK (est_civil IN ('S', 'C', 'D', 'V', 'U')),
    CONSTRAINT ck_beneficiario_situacao CHECK (sit_beneficiario IN ('A', 'S', 'C', 'I', 'D')),
    CONSTRAINT ck_beneficiario_biometria CHECK (ind_biometria IN ('S', 'N', 'P'))
);

CREATE TABLE IF NOT EXISTS beneficiario_dependente (
    id_dependente BIGSERIAL PRIMARY KEY,
    num_inscricao_benef BIGINT NOT NULL REFERENCES beneficiario(num_inscricao),
    seq_dependente SMALLINT NOT NULL,
    cpf_dependente VARCHAR(11),
    nome_dependente VARCHAR(60),
    dt_nasc_depend DATE,
    parentesco VARCHAR(2),
    sit_dependente CHAR(1),
    ind_deficiencia CHAR(1),
    CONSTRAINT uq_beneficiario_dependente UNIQUE (num_inscricao_benef, seq_dependente),
    CONSTRAINT ck_dependente_situacao CHECK (sit_dependente IN ('A', 'I', 'D')),
    CONSTRAINT ck_dependente_deficiencia CHECK (ind_deficiencia IN ('S', 'N'))
);

CREATE TABLE IF NOT EXISTS pagamento (
    num_pagamento BIGINT PRIMARY KEY,
    num_cpf VARCHAR(11) NOT NULL,
    num_inscricao BIGINT REFERENCES beneficiario(num_inscricao),
    cod_programa VARCHAR(4) NOT NULL REFERENCES programa_social(cod_programa),
    ano_mes_ref NUMERIC(6) NOT NULL,
    num_ciclo NUMERIC(6),
    vlr_bruto NUMERIC(11, 2),
    vlr_liquido NUMERIC(11, 2),
    vlr_desconto_total NUMERIC(11, 2),
    sit_pagamento CHAR(1) NOT NULL,
    dt_geracao DATE NOT NULL,
    hr_geracao TIME,
    dt_emissao DATE,
    dt_confirmacao DATE,
    dt_cancelamento DATE,
    mot_cancelamento VARCHAR(3),
    cod_banco VARCHAR(3),
    cod_agencia VARCHAR(6),
    num_conta VARCHAR(13),
    tipo_conta CHAR(1),
    cod_operacao VARCHAR(3),
    num_ob_siafi VARCHAR(12),
    num_ne_siafi VARCHAR(12),
    cod_ug_emitente VARCHAR(6),
    cod_gestao VARCHAR(5),
    sit_integ_siafi CHAR(1),
    dt_conciliacao DATE,
    sit_conciliacao CHAR(1),
    vlr_conciliado NUMERIC(11, 2),
    cod_retorno_banco VARCHAR(2),
    des_retorno_banco VARCHAR(40),
    hash_arq_remessa VARCHAR(64),
    hash_arq_retorno VARCHAR(64),
    dt_inclusao DATE,
    hr_inclusao TIME,
    usr_inclusao VARCHAR(8),
    dt_ult_alteracao DATE,
    hr_ult_alteracao TIME,
    usr_ult_alteracao VARCHAR(8),
    CONSTRAINT ck_pagamento_status CHECK (sit_pagamento IN ('P', 'G', 'E', 'C', 'D', 'X', 'R')),
    CONSTRAINT ck_pagamento_tipo_conta CHECK (tipo_conta IN ('C', 'P')),
    CONSTRAINT ck_pagamento_siafi CHECK (sit_integ_siafi IN ('I', 'P', 'E')),
    CONSTRAINT ck_pagamento_conciliacao CHECK (sit_conciliacao IN ('C', 'D', 'P', 'N'))
);

COMMENT ON TABLE pagamento IS 'Tabela de alto volume. Considerar particionamento por RANGE (ano_mes_ref) na implementacao definitiva.';

CREATE TABLE IF NOT EXISTS pagamento_desconto (
    id_desconto BIGSERIAL PRIMARY KEY,
    num_pagamento BIGINT NOT NULL REFERENCES pagamento(num_pagamento),
    seq_desconto SMALLINT NOT NULL,
    tipo_desconto VARCHAR(3),
    vlr_desconto NUMERIC(11, 2),
    pct_desconto NUMERIC(5, 2),
    num_processo VARCHAR(20),
    dt_inicio_desconto DATE,
    dt_fim_desconto DATE,
    CONSTRAINT uq_pagamento_desconto UNIQUE (num_pagamento, seq_desconto)
);

CREATE TABLE IF NOT EXISTS auditoria (
    num_auditoria BIGINT PRIMARY KEY,
    dt_evento DATE NOT NULL,
    hr_evento TIME,
    ts_evento TIMESTAMP NOT NULL,
    cod_acao VARCHAR(2) NOT NULL,
    cod_modulo VARCHAR(8),
    des_acao VARCHAR(80),
    tipo_entidade VARCHAR(4),
    id_entidade VARCHAR(15),
    num_cpf_afetado VARCHAR(11),
    usr_evento VARCHAR(8) NOT NULL,
    nome_usuario VARCHAR(40),
    cod_perfil VARCHAR(3),
    cod_lotacao VARCHAR(10),
    ip_origem VARCHAR(15),
    id_sessao VARCHAR(20),
    num_ciclo_batch NUMERIC(6),
    num_seq_batch NUMERIC(10),
    nom_job_batch VARCHAR(16),
    sit_batch CHAR(1),
    des_erro_batch VARCHAR(120),
    id_correlacao VARCHAR(36),
    num_seq_correlacao SMALLINT,
    CONSTRAINT ck_auditoria_acao CHECK (cod_acao IN ('IN', 'AL', 'EX', 'CO', 'LG', 'LO', 'BT', 'ER', 'AU', 'RE')),
    CONSTRAINT ck_auditoria_batch CHECK (sit_batch IN ('S', 'E', 'W'))
);

COMMENT ON TABLE auditoria IS 'Registro legalmente sensivel e imutavel. O modelo abaixo bloqueia UPDATE e DELETE.';

CREATE TABLE IF NOT EXISTS auditoria_valor_anterior (
    id_valor_anterior BIGSERIAL PRIMARY KEY,
    num_auditoria BIGINT NOT NULL REFERENCES auditoria(num_auditoria),
    seq_campo SMALLINT NOT NULL,
    campo_alterado VARCHAR(30),
    valor_anterior VARCHAR(80),
    CONSTRAINT uq_auditoria_valor_anterior UNIQUE (num_auditoria, seq_campo)
);

CREATE TABLE IF NOT EXISTS auditoria_valor_posterior (
    id_valor_posterior BIGSERIAL PRIMARY KEY,
    num_auditoria BIGINT NOT NULL REFERENCES auditoria(num_auditoria),
    seq_campo SMALLINT NOT NULL,
    campo_alterado VARCHAR(30),
    valor_posterior VARCHAR(80),
    CONSTRAINT uq_auditoria_valor_posterior UNIQUE (num_auditoria, seq_campo)
);

CREATE OR REPLACE FUNCTION bloquear_mutacao_auditoria()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Tabela %.% e imutavel no modelo de arqueologia.', TG_TABLE_SCHEMA, TG_TABLE_NAME;
END;
$$;

DROP TRIGGER IF EXISTS tr_bloquear_mutacao_auditoria ON auditoria;
CREATE TRIGGER tr_bloquear_mutacao_auditoria
BEFORE UPDATE OR DELETE ON auditoria
FOR EACH ROW
EXECUTE FUNCTION bloquear_mutacao_auditoria();

DROP TRIGGER IF EXISTS tr_bloquear_mutacao_auditoria_anterior ON auditoria_valor_anterior;
CREATE TRIGGER tr_bloquear_mutacao_auditoria_anterior
BEFORE UPDATE OR DELETE ON auditoria_valor_anterior
FOR EACH ROW
EXECUTE FUNCTION bloquear_mutacao_auditoria();

DROP TRIGGER IF EXISTS tr_bloquear_mutacao_auditoria_posterior ON auditoria_valor_posterior;
CREATE TRIGGER tr_bloquear_mutacao_auditoria_posterior
BEFORE UPDATE OR DELETE ON auditoria_valor_posterior
FOR EACH ROW
EXECUTE FUNCTION bloquear_mutacao_auditoria();

CREATE INDEX IF NOT EXISTS idx_programa_tipo_situacao
    ON programa_social (tipo_programa, sit_programa);

CREATE INDEX IF NOT EXISTS idx_programa_faixa_cod_programa
    ON programa_social_faixa_calculo (cod_programa);

CREATE INDEX IF NOT EXISTS idx_programa_regional_cod_programa
    ON programa_social_parametro_regional (cod_programa);

CREATE INDEX IF NOT EXISTS idx_programa_tipo_desconto_cod_programa
    ON programa_social_tipo_desconto (cod_programa);

CREATE INDEX IF NOT EXISTS idx_beneficiario_uf_situacao
    ON beneficiario (uf, sit_beneficiario);

CREATE INDEX IF NOT EXISTS idx_beneficiario_programa_situacao
    ON beneficiario (cod_programa, sit_beneficiario);

CREATE INDEX IF NOT EXISTS idx_beneficiario_dt_cadastro
    ON beneficiario (dt_cadastro DESC);

CREATE INDEX IF NOT EXISTS idx_beneficiario_regiao_cadastro
    ON beneficiario (cod_regiao, dt_cadastro DESC);

CREATE INDEX IF NOT EXISTS idx_beneficiario_dependente_beneficiario
    ON beneficiario_dependente (num_inscricao_benef);

CREATE INDEX IF NOT EXISTS idx_pagamento_cpf_competencia
    ON pagamento (num_cpf, ano_mes_ref DESC);

CREATE INDEX IF NOT EXISTS idx_pagamento_programa_competencia_situacao
    ON pagamento (cod_programa, ano_mes_ref DESC, sit_pagamento);

CREATE INDEX IF NOT EXISTS idx_pagamento_ciclo_situacao
    ON pagamento (num_ciclo, sit_pagamento);

CREATE INDEX IF NOT EXISTS idx_pagamento_dt_geracao
    ON pagamento (dt_geracao DESC);

CREATE INDEX IF NOT EXISTS idx_pagamento_situacao_conciliacao
    ON pagamento (sit_conciliacao, dt_conciliacao DESC)
    WHERE sit_conciliacao IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pagamento_desconto_pagamento
    ON pagamento_desconto (num_pagamento);

CREATE INDEX IF NOT EXISTS idx_auditoria_data_acao
    ON auditoria (dt_evento DESC, cod_acao);

CREATE INDEX IF NOT EXISTS idx_auditoria_entidade_data
    ON auditoria (tipo_entidade, id_entidade, dt_evento DESC);

CREATE INDEX IF NOT EXISTS idx_auditoria_usuario_data
    ON auditoria (usr_evento, dt_evento DESC);

CREATE INDEX IF NOT EXISTS idx_auditoria_cpf_data
    ON auditoria (num_cpf_afetado, dt_evento DESC)
    WHERE num_cpf_afetado IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auditoria_correlacao
    ON auditoria (id_correlacao, num_seq_correlacao)
    WHERE id_correlacao IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auditoria_valor_anterior_auditoria
    ON auditoria_valor_anterior (num_auditoria);

CREATE INDEX IF NOT EXISTS idx_auditoria_valor_posterior_auditoria
    ON auditoria_valor_posterior (num_auditoria);