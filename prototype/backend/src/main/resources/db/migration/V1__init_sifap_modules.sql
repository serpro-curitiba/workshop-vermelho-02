-- ============================================================================
-- V1__init_sifap_modules.sql
-- ============================================================================
-- Base moderna do SIFAP 2.0.
-- Padrão seguido: schemas por módulo + PE/MU do legado viram tabelas filhas.
--
-- Implements REQ-BEN-01, REQ-BEN-03, REQ-BEN-05
-- Implements REQ-CAT-01, REQ-CAT-02, REQ-CAT-03
-- Implements REQ-PAY-01, REQ-PAY-05, REQ-PAY-06, REQ-PAY-08, REQ-PAY-11, REQ-PAY-12
-- Implements REQ-AUD-01, REQ-AUD-03
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS beneficiary;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS catalog.social_program (
    code                    VARCHAR(4) PRIMARY KEY,
    name                    VARCHAR(60) NOT NULL,
    program_type            VARCHAR(1) NOT NULL,
    base_value_original     NUMERIC(11, 2) NOT NULL,
    base_value_adjusted     NUMERIC(11, 2) NOT NULL,
    factor_reajuste         NUMERIC(9, 4) NOT NULL,
    factor_k                NUMERIC(10, 7) NOT NULL,
    biometrics_required     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT social_program_type_ck CHECK (program_type IN ('A', 'T', 'P'))
);

CREATE TABLE IF NOT EXISTS catalog.regional_parameter (
    id                      BIGSERIAL PRIMARY KEY,
    program_code            VARCHAR(4) NOT NULL REFERENCES catalog.social_program(code) ON DELETE CASCADE,
    uf                      VARCHAR(2) NOT NULL,
    factor_regional         NUMERIC(9, 4) NOT NULL,
    CONSTRAINT regional_parameter_program_uf_uk UNIQUE (program_code, uf)
);

CREATE TABLE IF NOT EXISTS beneficiary.beneficiary (
    id                      BIGSERIAL PRIMARY KEY,
    cpf                     VARCHAR(11) NOT NULL UNIQUE,
    name                    VARCHAR(80) NOT NULL,
    birth_date              DATE NOT NULL,
    status                  VARCHAR(1) NOT NULL,
    uf                      VARCHAR(2) NOT NULL,
    region_code             VARCHAR(2) NOT NULL,
    family_income           NUMERIC(11, 2) NOT NULL,
    program_code            VARCHAR(4) NOT NULL REFERENCES catalog.social_program(code),
    biometrics_status       VARCHAR(1) NOT NULL DEFAULT 'N',
    biometrics_collected_at DATE,
    biometrics_post_code    VARCHAR(6),
    biometrics_hash         VARCHAR(64),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT beneficiary_status_ck CHECK (status IN ('A', 'S', 'C', 'I', 'D')),
    CONSTRAINT beneficiary_biometrics_ck CHECK (biometrics_status IN ('S', 'N', 'P'))
);

CREATE TABLE IF NOT EXISTS beneficiary.dependent (
    id                      BIGSERIAL PRIMARY KEY,
    beneficiary_id          BIGINT NOT NULL REFERENCES beneficiary.beneficiary(id) ON DELETE CASCADE,
    cpf                     VARCHAR(11),
    name                    VARCHAR(80) NOT NULL,
    birth_date              DATE NOT NULL,
    relationship_code       VARCHAR(2) NOT NULL,
    status                  VARCHAR(1) NOT NULL,
    disability_indicator    VARCHAR(1) NOT NULL DEFAULT 'N',
    CONSTRAINT dependent_status_ck CHECK (status IN ('A', 'I', 'D')),
    CONSTRAINT dependent_disability_ck CHECK (disability_indicator IN ('S', 'N'))
);

CREATE TABLE IF NOT EXISTS payment.payment_record (
    id                      BIGSERIAL PRIMARY KEY,
    beneficiary_cpf         VARCHAR(11) NOT NULL,
    competence              VARCHAR(6) NOT NULL,
    gross_amount            NUMERIC(11, 2) NOT NULL,
    deduction_total         NUMERIC(11, 2) NOT NULL,
    net_amount              NUMERIC(11, 2) NOT NULL,
    thirteenth_amount       NUMERIC(11, 2) NOT NULL DEFAULT 0,
    christmas_bonus         NUMERIC(11, 2) NOT NULL DEFAULT 0,
    payment_type            VARCHAR(1) NOT NULL,
    status                  VARCHAR(1) NOT NULL DEFAULT 'G',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT payment_record_status_ck CHECK (status IN ('P', 'G', 'E', 'C', 'D', 'X', 'R')),
    CONSTRAINT payment_record_type_ck CHECK (payment_type IN ('N', 'D')),
    CONSTRAINT payment_record_cpf_competence_uk UNIQUE (beneficiary_cpf, competence)
);

CREATE TABLE IF NOT EXISTS payment.payment_discount (
    id                      BIGSERIAL PRIMARY KEY,
    payment_id              BIGINT NOT NULL REFERENCES payment.payment_record(id) ON DELETE CASCADE,
    discount_type           VARCHAR(3) NOT NULL,
    amount                  NUMERIC(11, 2) NOT NULL,
    start_date              DATE,
    end_date                DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT payment_discount_type_ck CHECK (discount_type IN ('SOC', 'IR', 'JD', 'CS', 'PA', 'EM', 'TX', 'OU', 'EX'))
);

CREATE TABLE IF NOT EXISTS audit.audit_event (
    id                      BIGSERIAL PRIMARY KEY,
    action_code             VARCHAR(2) NOT NULL,
    entity_type             VARCHAR(20) NOT NULL,
    entity_id               VARCHAR(40) NOT NULL,
    description             VARCHAR(255) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT audit_event_action_ck CHECK (action_code IN ('IN', 'AL', 'EX', 'CO', 'LG', 'LO', 'BT', 'ER', 'AU', 'RE'))
);

CREATE INDEX IF NOT EXISTS idx_beneficiary_program ON beneficiary.beneficiary(program_code);
CREATE INDEX IF NOT EXISTS idx_payment_competence ON payment.payment_record(competence);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment.payment_record(status);
CREATE INDEX IF NOT EXISTS idx_payment_discount_payment ON payment.payment_discount(payment_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_action ON audit.audit_event(action_code, created_at DESC);

REVOKE UPDATE, DELETE ON audit.audit_event FROM PUBLIC;