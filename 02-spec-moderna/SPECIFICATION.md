<!-- markdownlint-disable MD013 MD025 MD040 MD033 -->

# SPECIFICATION — SIFAP 2.0 (inicial)

> **Par 2 · Arquitetura · Estágio 2 · Versão 0.1 (handoff para Par 3)**
> Cada requisito segue notação EARS, possui ID `REQ-<CTX>-NN` e **`source_legacy:` obrigatório** (regra do CI job `legacy-traceability`).
> Onde não há precedente legado e o requisito é greenfield, usa-se `source_legacy: GREENFIELD` com justificativa explícita.

## Convenções

- **Contextos**: `BEN` = Beneficiário · `CAT` = Catálogo de Programas · `PAY` = Pagamento · `AUD` = Auditoria · `SYS` = transversal.
- **Tipos EARS** (entre colchetes ao final): `[U]` Ubíquo · `[E]` Evento · `[S]` Estado · `[O]` Opcional · `[C]` Condicional.
- **Rastreabilidade**: cada `source_legacy:` aponta para arquivo + range de linhas verificável.

---

## Beneficiário

### REQ-BEN-01 [U]

O sistema **deverá** identificar unicamente cada beneficiário por CPF (11 dígitos, sem formatação).

- `source_legacy:` [`01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm) — campo `AB NUM-CPF` (Descriptor S1)

### REQ-BEN-02 [S]

**Enquanto** o status do beneficiário for diferente de `A` (Ativo), o sistema **deverá** rejeitar qualquer geração de pagamento para esse beneficiário.

- `source_legacy:` [`01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) (linhas 154–158: `IF BENEFICIARIO-V.STATUS NE 'A' ... ESCAPE ROUTINE`) + [`VALELEG.NSN`](../01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN) (linhas 102–121)

### REQ-BEN-03 [U]

O sistema **deverá** permitir até **10 dependentes** por beneficiário, cada um com CPF (ou `00000000000`), nome, data de nascimento, grau de parentesco (`FI`/`CJ`/`NT`/`TU`), situação (`A`/`I`/`D`) e indicador de deficiência (`S`/`N`).

- `source_legacy:` [`BENEFICIARIO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm) — grupo periódico `DA GRP-DEPENDENTE PE (max 10)`

### REQ-BEN-04 [U]

O sistema **deverá** armazenar endereço completo do beneficiário com CEP (8 dígitos sem hífen), código IBGE do município (7 dígitos) e código de região (`01`–`05` ou `99`).

- `source_legacy:` [`BENEFICIARIO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm) — grupo `BA GRP-ENDERECO`

### REQ-BEN-05 [O]

**Onde** o programa social exigir biometria, o sistema **deverá** registrar indicador de biometria (`S`/`N`/`P`), data de coleta, código do posto coletor e hash SHA-256 do template digital.

- `source_legacy:` [`BENEFICIARIO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm) — campos `FA–FD` (adicionados em 2005) + [`PROGRAMA-SOCIAL.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm) campo `CI IND-EXIGE-BIOMETRIA`

---

## Catálogo de Programas Sociais

### REQ-CAT-01 [U]

O sistema **deverá** manter cadastro paramétrico de programas sociais identificados por `COD-PROGRAMA` (4 caracteres alfanuméricos), contendo nome oficial, sigla, tipo (`A`/`T`/`P`), valores base individual e familiar, tetos/pisos e percentual de reajuste anual.

- `source_legacy:` [`PROGRAMA-SOCIAL.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm) — campos `AA`, `AB`, `AC`, `AD`, `BA`–`BE`

### REQ-CAT-02 [U]

O sistema **deverá** permitir até **5 faixas de cálculo** por programa, cada uma com renda inicial/final, fator multiplicador e valor adicional fixo.

- `source_legacy:` [`PROGRAMA-SOCIAL.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm) — `DA GRP-FAIXA-CALCULO PE (max 5)`

### REQ-CAT-03 [U]

O sistema **deverá** calcular `FATOR-K = 1 + (FATOR-REAJUSTE × 0.347215)` no momento do cadastro/alteração do programa social, e **deverá** persistir `VLR-BASE` já ajustado por `VLR-BASE × FATOR-K`. O fator e a constante mágica `0.347215` ficam isolados em `CatalogoProgramasService.computarFatorK()` e auditados.

- `source_legacy:` [`CADPROG.NSN`](../01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN) (BR-018, BR-019 em [business-rules-catalog.md](../01-arqueologia/business-rules-catalog.md)) + [`PROGRAMA-SOCIAL.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm) — campo `BG FATOR-K`
- Decisão arquitetural: [ADR-002 § Decisão item 5](ADRs/ADR-002-mapeamento-adabas-postgresql.md)
- **Atenção ao Par 3**: BR-022 (`CALCBENF.NSN`) aplica `(1 + FATOR-REAJUSTE)` sobre `VLR-BASE` — que já vem ajustado por BR-019. Há **suspeita de duplo reajuste no legado**. Validar com cálculo paralelo em shadow read antes do flip; não replicar cegamente. Possível ADR-004 para corrigir.

### REQ-CAT-04 [C]

**Se** o tipo do programa for `A` (Assistencial) **E** a renda do beneficiário for maior que `600.00` **E** o beneficiário não tiver dependentes ativos, **então** o sistema **deverá** marcar o beneficiário como inelegível com motivo `"PROG ASSISTENCIAL: RENDA > 600 SEM DEPENDENTES"`.

- `source_legacy:` [`VALELEG.NSN`](../01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN) — linhas 166–177 (bloco `DECIDE ON FIRST VALUE OF #TIPO-PROG / VALUE 'A'`)

### REQ-CAT-05 [C]

**Se** o tipo do programa for `P` (Previdenciário) **E** a idade do beneficiário for menor que **60 anos**, **então** o sistema **deverá** marcar o beneficiário como inelegível.

- `source_legacy:` [`VALELEG.NSN`](../01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN) — linhas 184–190

### REQ-CAT-06 [C]

**Se** o tipo do programa for `T` (Trabalho) **E** a idade do beneficiário estiver fora do intervalo `[16, 65]`, **então** o sistema **deverá** marcar o beneficiário como inelegível.

- `source_legacy:` [`VALELEG.NSN`](../01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN) — linhas 192–198

### REQ-CAT-07 [O]

**Onde** o código de região do beneficiário for `99` (especial/internacional), o sistema **deverá** considerá-lo automaticamente elegível, sem aplicar demais validações.

- `source_legacy:` [`VALELEG.NSN`](../01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN) — linhas 94–98

---

## Pagamento — Cálculo

### REQ-PAY-01 [U]

O sistema **deverá** calcular o valor bruto do benefício mensal pela fórmula:

$$ \text{VLR\_BENEFICIO} = \text{VLR\_BASE} \times \text{FATOR\_REG} \times \text{FATOR\_FAM} \times \text{FATOR\_RND} \times \text{FATOR\_IDADE} \times (1 + \text{FATOR\_REAJUSTE}) $$

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 215–222 (`COMPUTE #VLR-BENF = #VLR-BASE * #FATOR-REG * #FATOR-FAM * #FATOR-RND * #FATOR-IDADE` seguido de `* (1 + #FATOR-REAJ)`)
- Confirmado idêntico em [`BATCHPGT.NSN`](../01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN) linhas 268–272

### REQ-PAY-02 [U]

O sistema **deverá** aplicar o **fator regional** lendo a tabela `parametro_regional` por UF do beneficiário, com fallback `1.0000` se a UF não constar.

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 178–183 (`IF #COD-REG >= 1 AND #COD-REG <= 25 / MOVE #TAB-REG(#COD-REG) ... ELSE MOVE 1.0000`); tabela carregada hardcoded nas linhas 95–122 — na 2.0 **migra para `PROGRAMA-SOCIAL.GRP-PARAM-REGIONAL`** ([`PROGRAMA-SOCIAL.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm) `FA`)

### REQ-PAY-03 [U]

O sistema **deverá** aplicar o **fator familiar** pela faixa de dependentes ativos:

| Dependentes | Fator |
|---|---|
| 0 | 1.0000 |
| 1–2 | 1.0000 + (n × 0.0500) |
| 3–4 | 1.1000 + ((n − 2) × 0.0300) |
| ≥ 5 | 1.1600 + ((n − 4) × 0.0200) |

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 185–199

### REQ-PAY-04 [U]

O sistema **deverá** aplicar o **fator idade**:

| Idade | Fator |
|---|---|
| ≥ 65 | 1.1500 |
| 60–64 | 1.1000 |
| < 18 | 1.0500 |
| 18–59 | 1.0000 |

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 203–214

### REQ-PAY-05 [E]

**Quando** o mês de competência for **dezembro** (`MM = 12`), o sistema **deverá** calcular e somar ao valor bruto a parcela de 13º salário pela fórmula:

$$ \text{VLR\_13} = \text{VLR\_BASE} \times \text{FATOR\_REG} \times \text{FATOR\_IDADE} $$

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 230–237 (bloco `IF #MES = 12`)

### REQ-PAY-06 [E]

**Quando** o mês de competência for **dezembro** **E** o tipo do programa for `A` (Assistencial), o sistema **deverá** somar adicionalmente um **abono natalino** de 15 % do valor base calculado.

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 240–248 (`IF #TIPO-PROG = 'A' / COMPUTE #VLR-ABONO = #VLR-BENF * 0.15`)

### REQ-PAY-07 [U]

O sistema **deverá** truncar (não arredondar) todos os valores monetários para **2 casas decimais** após cada operação.

- `source_legacy:` [`CALCBENF.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) — linhas 225–227, 233–234, 244–245 (padrão `COMPUTE #VLR-TEMP = X * 100 / COMPUTE X = #VLR-TEMP / 100`)
- **Implementação 2.0**: `BigDecimal.setScale(2, RoundingMode.DOWN)`

---

## Pagamento — Descontos

### REQ-PAY-08 [U]

O sistema **deverá** aplicar contribuição social compulsória por faixa de valor bruto:

| Faixa | Alíquota |
|---|---|
| ≤ 500.00 | 3 % |
| ≤ 1000.00 | 5 % |
| ≤ 2000.00 | 7 % |
| > 2000.00 | 9 % |

- `source_legacy:` [`CALCDSCT.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN) — linhas 60–67 (tabela) + sub-rotina `CALC-CONTRIB-SOCIAL` linhas 197–205

### REQ-PAY-09 [U]

O sistema **deverá** limitar o total de descontos a **30 % do valor bruto**, **exceto** descontos do tipo `J` (Judicial) que não possuem teto.

- `source_legacy:` [`CALCDSCT.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN) — linhas 102–106 (`COMPUTE #VLR-MAX-DSCT = #VLR-BRUTO * 0.30`) + 160–164 (`IF #TIPO-DSCT NE 'J' / IF #VLR-TOTAL-DSCT > #VLR-MAX-DSCT`)

### REQ-PAY-10 [U]

O sistema **deverá** suportar pelo menos os seguintes tipos de desconto cadastrados por beneficiário: `IR` (Imposto), `JD` (Judicial), `CS` (Consignado), `PA` (Pensão Alimentícia), `EM` (Empréstimo), `TX` (Taxa), `OU` (Outros), `EX` (Extraordinário), respeitando o limite de **8 descontos simultâneos**.

- `source_legacy:` [`PAGAMENTO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm) — `CA GRP-DESCONTO PE (max 8)` + campo `CB TIPO-DESCONTO`

### REQ-PAY-11 [C]

**Se** um desconto tiver `DT-FIM-DSCT` preenchida **E** ela for anterior à data de processamento, **então** o sistema **não deverá** aplicar esse desconto no pagamento.

- `source_legacy:` [`CALCDSCT.NSN`](../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN) — linhas 113–119 (validação `IF BENEFICIARIO-V.DT-FIM-DSCT(#IDX) NE 0 AND ... < #DT-HOJE / ESCAPE TOP`)

---

## Pagamento — Ciclo mensal e integração

### REQ-PAY-12 [E]

**Quando** o batch mensal for executado para uma competência `AAAAMM`, o sistema **deverá** gerar **no máximo um pagamento** por par `(CPF, COMPETENCIA)`, ignorando beneficiários cujo pagamento dessa competência já exista.

- `source_legacy:` [`BATCHPGT.NSN`](../01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN) — linhas 198–208 (`FIND PAGAMENTO-V WITH CPF-BENEF ... IF PAGAMENTO-V.COMPETENCIA = #COMPETENCIA / MOVE TRUE TO #JA-GERADO`)

### REQ-PAY-13 [U]

O sistema **deverá** processar a geração do ciclo **em ordem ascendente de CPF** (contrato com sistemas downstream desde 1999).

- `source_legacy:` [`BATCHPGT.NSN`](../01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN) — linhas 176–179 (`READ BENEFICIARIO-V BY CPF` com nota `OTIMIZACAO 1999 / SISTEMAS DOWNSTREAM DEPENDEM DESTA ORDENACAO`)

### REQ-PAY-14 [E]

**Quando** um pagamento for emitido para o banco, o sistema **deverá** registrar a Ordem Bancária SIAFI (`NUM-OB-SIAFI`), Nota de Empenho (`NUM-NE-SIAFI`), Unidade Gestora emitente (`COD-UG-EMITENTE`) e Código de Gestão, atualizando o status de integração (`I`/`P`/`E`).

- `source_legacy:` [`PAGAMENTO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm) — campos `FA–FE` (integração SIAFI adicionada em 09/2002)

### REQ-PAY-15 [E]

**Quando** o arquivo de remessa CNAB 240 for gerado, o sistema **deverá** armazenar seu hash SHA-256 em `HASH-ARQ-REMESSA`; **quando** o arquivo de retorno for processado, **deverá** armazenar igualmente em `HASH-ARQ-RETORNO`.

- `source_legacy:` [`PAGAMENTO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm) — campos `HA HASH-ARQ-REMESSA`, `HB HASH-ARQ-RETORNO` (adicionados em 11/2015 por Carlos Mendes)

### REQ-PAY-16 [S]

**Enquanto** um pagamento estiver no status `P` (Pendente) ou `G` (Gerado), o sistema **deverá** permitir cancelamento; **enquanto** estiver `C` (Confirmado), o cancelamento **deverá** ser rejeitado e requerer fluxo de devolução.

- `source_legacy:` [`PAGAMENTO.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm) — campo `DA SIT-PAGAMENTO` (`P`/`G`/`E`/`C`/`D`/`X`/`R`)

---

## Auditoria

### REQ-AUD-01 [U]

O sistema **deverá** registrar evento de auditoria imutável para toda operação de inclusão, alteração, exclusão, login, logout, batch, erro, autorização ou rejeição, com timestamp em precisão de segundo (`AAAAMMDDHHMMSS`), usuário, módulo de origem e descrição.

- `source_legacy:` [`AUDITORIA.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm) — códigos de ação `BA COD-ACAO` (`IN/AL/EX/CO/LG/LO/BT/ER/AU/RE`)

### REQ-AUD-02 [E]

**Quando** uma alteração for registrada, o sistema **deverá** preservar o **estado anterior e posterior** dos campos modificados (até 20 campos por evento).

- `source_legacy:` [`AUDITORIA.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm) — grupos `DA GRP-ANTES` e `DD GRP-DEPOIS` (MU max 20, adicionados em 04/2005)

### REQ-AUD-03 [U]

O sistema **deverá** garantir retenção mínima de **10 anos** para todo evento de auditoria, sem permitir UPDATE ou DELETE sobre a tabela.

- `source_legacy:` [`AUDITORIA.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm) — cabeçalho linhas 9–13 (`IN-TCU 63/2010` + `RETENCAO MINIMA: 10 ANOS (ART 14 LEI 8159)` + `REGISTRO IMUTAVEL - NAO PERMITE UPDATE/DELETE`)

### REQ-AUD-04 [U]

O sistema **deverá** registrar o endereço IP de origem e o identificador de sessão de cada evento.

- `source_legacy:` [`AUDITORIA.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm) — campos `EE IP-ORIGEM` (adicionado em 02/2012) e `EF ID-SESSAO`

### REQ-AUD-05 [U]

O sistema **deverá** exibir **todas** as ações registradas, incluindo ações `EX` (exclusão), corrigindo o comportamento do `RELAUDIT.NSN` legado que as filtrava.

- `source_legacy:` [`AUDITORIA.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm) — `NOTA2` final (`CUIDADO - PROGRAMA RELAUDIT.NSN FILTRA ACOES 'EX' NA EXIBICAO`)
- Decisão pendente de validação com cliente — ver [ADR-003 § Riscos](ADRs/ADR-003-strangler-coexistencia-siafi.md)

---

## Transversais

### REQ-SYS-01 [U]

O sistema **deverá** expor APIs REST versionadas em `/api/v1/*` com contrato OpenAPI 3.1 publicado.

- `source_legacy:` GREENFIELD — não existia no legado (acesso era exclusivo via terminal 3270 / NSN online).

### REQ-SYS-02 [U]

O sistema **deverá** autenticar via OAuth2/JWT e autorizar por perfis `ADM`, `OPR`, `CON`, `AUD`, `SUP` (mapeados a partir dos perfis legados).

- `source_legacy:` [`AUDITORIA.ddm`](../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm) — campo `EC COD-PERFIL` (`ADM/OPR/CON/AUD/SUP`)

### REQ-SYS-03 [U]

O sistema **deverá** rotear cada bounded context entre legado e 2.0 por feature flag (`sifap.routing.<context> = legacy|new`) sem necessidade de redeploy.

- `source_legacy:` GREENFIELD — capacidade do plano de migração Strangler Fig
- Decisão arquitetural: [ADR-003](ADRs/ADR-003-strangler-coexistencia-siafi.md)

---

## Rastreabilidade reversa (resumo)

| Arquivo legado | Requisitos derivados |
|---|---|
| `BENEFICIARIO.ddm` | REQ-BEN-01, BEN-03, BEN-04, BEN-05 |
| `PAGAMENTO.ddm` | REQ-PAY-10, PAY-14, PAY-15, PAY-16 |
| `PROGRAMA-SOCIAL.ddm` | REQ-CAT-01, CAT-02, CAT-03, PAY-02 |
| `AUDITORIA.ddm` | REQ-AUD-01, AUD-02, AUD-03, AUD-04, AUD-05, SYS-02 |
| `CALCBENF.NSN` | REQ-BEN-02, PAY-01, PAY-02, PAY-03, PAY-04, PAY-05, PAY-06, PAY-07 |
| `CALCDSCT.NSN` | REQ-PAY-08, PAY-09, PAY-11 |
| `VALELEG.NSN` | REQ-BEN-02, CAT-04, CAT-05, CAT-06, CAT-07 |
| `BATCHPGT.NSN` | REQ-PAY-12, PAY-13 |
| GREENFIELD | REQ-SYS-01, SYS-03 |

---

## Critérios de Aceite (Given / When / Then)

> Pelo menos 2 critérios por requisito. Cada um é executável como teste automatizado (JUnit 5 + AssertJ no backend; Vitest no frontend).

### Beneficiário

**REQ-BEN-01** — identificação por CPF

- **AC-01**: Given o CPF `12345678901` ainda não existe na base, When `POST /beneficiarios` recebe payload válido com esse CPF, Then a resposta é `201 Created` e o `Location` aponta para `/beneficiarios/12345678901`.
- **AC-02**: Given o CPF `12345678901` já existe, When uma nova requisição de cadastro com o mesmo CPF é feita, Then a resposta é `409 Conflict` com `Problem.title = "CPF já cadastrado"`.

**REQ-BEN-02** — beneficiário inativo não recebe pagamento

- **AC-01**: Given beneficiário com `situacao = 'S'` (suspenso), When `POST /pagamentos:calcular` é chamado para esse CPF, Then resposta é `409` com mensagem `"BENEFICIARIO NAO ATIVO"` e nenhum registro é gravado em `pagamento.pagamento`.
- **AC-02**: Given beneficiário com `situacao ∈ {C, I, D}`, When o ciclo mensal é executado, Then esse CPF é ignorado e contabilizado em `CicloMensalResultado.ignorados`.

**REQ-BEN-03** — até 10 dependentes

- **AC-01**: Given beneficiário com 9 dependentes ativos, When `POST /beneficiarios/{cpf}/dependentes` recebe o 10º, Then resposta `201` e a lista passa a ter 10 itens.
- **AC-02**: Given beneficiário com 10 dependentes ativos, When a 11ª inclusão é tentada, Then resposta `409 Conflict` com `Problem.detail = "limite de 10 dependentes atingido"`.

**REQ-BEN-04** — endereço completo

- **AC-01**: Given payload de cadastro com `endereco.cep = "01310100"` e `endereco.uf = "SP"`, When o cadastro ocorre, Then o registro persistido tem `codIbge` resolvido (≠ null) e `codRegiao = "03"` (Sudeste).
- **AC-02**: Given payload com `cep = "00000000"` (inválido por checksum), When o cadastro é tentado, Then resposta `400` com violação de validação no campo `endereco.cep`.

**REQ-BEN-05** — biometria (opcional por programa)

- **AC-01**: Given programa cujo `exigeBiometria = true` e beneficiário sem biometria, When `POST /pagamentos:calcular`, Then resposta `409` com mensagem `"BIOMETRIA PENDENTE"`.
- **AC-02**: Given chamada `POST /beneficiarios/{cpf}/biometria` com `hashTemplate` SHA-256 válido, Then `indBiometria` passa a `'S'` e `dtColetaBio` é a data corrente.

### Catálogo de Programas

**REQ-CAT-01** — cadastro paramétrico de programas

- **AC-01**: Given catálogo vazio, When `POST /programas` recebe programa válido com `codigo = "PBF1"`, Then resposta `201`.
- **AC-02**: Given programa `PBF1` cadastrado, When `GET /programas/PBF1`, Then retorna `ProgramaDetalhe` com todos os campos persistidos.

**REQ-CAT-02** — até 5 faixas de cálculo

- **AC-01**: Given programa com 4 faixas, When uma 5ª é incluída, Then é aceita.
- **AC-02**: Given programa com 5 faixas, When uma 6ª é tentada, Then resposta `409`.

**REQ-CAT-03** — FATOR-K calculado no cadastro do programa

- **AC-01**: Given programa com `fatorReajuste = 0.10`, When `POST /programas` cadastra o programa, Then `fatorK` persistido = `1 + (0.10 × 0.347215) = 1.0347215` (precisão `numeric(7,6)`).
- **AC-02**: Given programa cadastrado com `vlrBase = 100.00` e `fatorReajuste = 0.10`, Then `vlrBase` persistido = `100.00 × 1.0347215 = 103.47` (truncado a 2 casas conforme BR-028).

**REQ-CAT-04** — programa Assistencial: renda > 600 sem dependentes

- **AC-01**: Given beneficiário com `rendaFamiliar = 700.00` e 0 dependentes ativos e programa tipo `A`, When `POST /programas/{cod}/elegibilidade/{cpf}`, Then `elegivel = false` e motivos contém `"PROG ASSISTENCIAL: RENDA > 600 SEM DEPENDENTES"`.
- **AC-02**: Given mesmo beneficiário com 2 dependentes ativos, Then `elegivel = true` (assumindo demais validações OK).

**REQ-CAT-05** — Previdenciário: idade ≥ 60

- **AC-01**: Given beneficiário com 59 anos, programa tipo `P`, When elegibilidade é avaliada, Then `elegivel = false` com motivo `"PROG PREVIDENCIARIO: IDADE < 60"`.
- **AC-02**: Given beneficiário com 60 anos, Then `elegivel = true` (demais OK).

**REQ-CAT-06** — Trabalho: idade ∈ [16, 65]

- **AC-01**: Given idade 15, programa tipo `T`, Then `elegivel = false`.
- **AC-02**: Given idade 66, programa tipo `T`, Then `elegivel = false`. Given idade 30, Then `elegivel = true`.

**REQ-CAT-07** — Região 99 é automaticamente elegível

- **AC-01**: Given beneficiário com `codRegiao = "99"`, When elegibilidade é avaliada, Then `elegivel = true` sem aplicar demais validações.
- **AC-02**: Auditoria registra essa decisão com `acao = "AU"` e descrição `"REGIAO ESPECIAL"`.

### Pagamento — Cálculo

**REQ-PAY-01** — fórmula do benefício mensal

- **AC-01**: Given beneficiário e programa cujos fatores levam a `VLR_BASE × FATOR_REG × FATOR_FAM × FATOR_RND × FATOR_IDADE × (1 + reajuste) = 712.345`, When `POST /pagamentos:calcular`, Then `vlrBruto = 712.34` (truncado, REQ-PAY-07).
- **AC-02**: Property-based test (jqwik) com 1 000 combinações aleatórias de fatores compara a saída do serviço com a fórmula expressa em BigDecimal — divergência tolerada: `0.00`.

**REQ-PAY-02** — fator regional vem do catálogo

- **AC-01**: Given UF `SP` com fator `1.1000` cadastrado, When o cálculo ocorre, Then `FATOR_REG` aplicado é `1.1000`.
- **AC-02**: Given UF inexistente no catálogo, Then `FATOR_REG = 1.0000` (fallback) e um warning é logado.

**REQ-PAY-03** — fator familiar por faixa de dependentes

- **AC-01**: Tabela de testes parametrizados:

  | Dependentes | FATOR_FAM esperado |
  |---|---|
  | 0 | 1.0000 |
  | 1 | 1.0500 |
  | 2 | 1.1000 |
  | 3 | 1.1300 |
  | 4 | 1.1600 |
  | 5 | 1.1800 |
  | 8 | 1.2400 |
- **AC-02**: Apenas dependentes com `situacao = 'A'` são contados.

**REQ-PAY-04** — fator idade

- **AC-01**: Tabela parametrizada para idades `{17, 25, 60, 64, 65, 70}` confere `{1.05, 1.00, 1.10, 1.10, 1.15, 1.15}`.
- **AC-02**: Idade é calculada como `ano(competencia) − ano(dataNascimento)` (mesma semântica de `CALCBENF.NSN` linha 200).

**REQ-PAY-05** — 13º salário em dezembro

- **AC-01**: Given competência `202612`, When o cálculo ocorre, Then `vlrBruto = vlrMensal + vlrDecimoTerceiro` e o campo `tipoPagamento = 'D'`.
- **AC-02**: Given competência `202611`, Then `vlrDecimoTerceiro = 0` e `tipoPagamento = 'N'`.

**REQ-PAY-06** — abono natalino 15% (apenas programa tipo A)

- **AC-01**: Given dezembro + programa tipo `A` com `vlrMensal = 1000.00`, Then `vlrAbono = 150.00` e somado ao bruto.
- **AC-02**: Given dezembro + programa tipo `P` ou `T`, Then `vlrAbono = 0`.

**REQ-PAY-07** — truncamento (não arredondamento) a 2 casas

- **AC-01**: Given cálculo intermediário `100.999`, Then valor persistido é `100.99` (não `101.00`).
- **AC-02**: Auditoria de equivalência: 100 amostras aleatórias do legado vs. 2.0 → divergência máxima `0.00`.

### Pagamento — Descontos

**REQ-PAY-08** — contribuição social por faixa

- **AC-01**: Tabela parametrizada `{vlrBruto, aliquotaEsperada}`: `{300 → 3%, 750 → 5%, 1500 → 7%, 5000 → 9%}`.
- **AC-02**: Contribuição é sempre aplicada (compulsória), independente de outros descontos.

**REQ-PAY-09** — teto de 30% (judicial sem teto)

- **AC-01**: Given `vlrBruto = 1000` e descontos não-judiciais somando 400, Then `vlrDescontoTotal` é capado em 300.
- **AC-02**: Given mesmo cenário + desconto judicial de 500, Then `vlrDescontoTotal = 300 + 500 = 800` (judicial fura o teto).

**REQ-PAY-10** — tipos de desconto suportados

- **AC-01**: Cada um dos 8 tipos (`IR, JD, CS, PA, EM, TX, OU, EX`) é aceito em `POST /beneficiarios/{cpf}/descontos`.
- **AC-02**: Tentativa de cadastrar um 9º desconto retorna `409`.

**REQ-PAY-11** — vigência de desconto

- **AC-01**: Given desconto com `dtFim` ontem, When o cálculo de hoje ocorre, Then esse desconto **não** é aplicado.
- **AC-02**: Given desconto com `dtInicio` amanhã, Then também não é aplicado hoje.

### Pagamento — Ciclo

**REQ-PAY-12** — idempotência (CPF, competência)

- **AC-01**: Given ciclo `202605` já gerou pagamento para CPF X, When o ciclo é re-executado, Then nenhum novo pagamento é criado para X.
- **AC-02**: Re-execução total resulta em `CicloMensalResultado.gerados = 0` e `ignorados = N` (todos).

**REQ-PAY-13** — processamento em ordem ascendente de CPF

- **AC-01**: Teste de integração captura a ordem dos `INSERT` em `pagamento.pagamento` durante um ciclo e verifica monotônica crescente por CPF.
- **AC-02**: Documentação OpenAPI marca a query `GET /pagamentos?competencia=...` como retornando ordenado por CPF por padrão.

**REQ-PAY-14** — integração SIAFI

- **AC-01**: Given pagamento emitido com sucesso, Then `siafi.numOb`, `siafi.numNe`, `siafi.codUg` estão preenchidos e `siafi.situacao = 'I'`.
- **AC-02**: Given erro do SIAFI, Then `siafi.situacao = 'E'` e o pagamento permanece em `situacao = 'G'` (não emitido).

**REQ-PAY-15** — hashes SHA-256 dos arquivos CNAB

- **AC-01**: Given arquivo de remessa gerado, Then `hashArqRemessa` é exatamente `SHA-256(conteúdo_binário)`.
- **AC-02**: Given arquivo de retorno processado, Then `hashArqRetorno` está preenchido e auditoria registra evento com correlação.

**REQ-PAY-16** — cancelamento depende do status

- **AC-01**: Given pagamento em `situacao = 'P'` ou `'G'`, When `POST /pagamentos/{n}:cancelar`, Then `204` e `situacao = 'X'`.
- **AC-02**: Given pagamento em `situacao = 'C'`, When cancelamento é tentado, Then `409` com mensagem `"USAR FLUXO DE DEVOLUCAO"`.

### Auditoria

**REQ-AUD-01** — registro imutável de toda operação

- **AC-01**: Para cada um dos 10 códigos (`IN, AL, EX, CO, LG, LO, BT, ER, AU, RE`), existe um teste end-to-end que dispara a operação e verifica a presença do evento em `auditoria.evento_auditoria`.
- **AC-02**: O timestamp gravado tem precisão de segundo (`tsEvento` é `Instant`, não `LocalDate`).

**REQ-AUD-02** — estado antes/depois

- **AC-01**: Given uma alteração de `nomeCompleto` de `"Maria"` para `"Maria Silva"`, Then o evento contém `camposAlterados = [{ campo: "nomeCompleto", valorAnterior: "Maria", valorPosterior: "Maria Silva" }]`.
- **AC-02**: Alteração em mais de 20 campos simultâneos resulta em truncamento explícito com flag `truncado = true` (limite herdado do MU max 20).

**REQ-AUD-03** — retenção ≥ 10 anos, imutabilidade

- **AC-01**: Migração Flyway aplica `REVOKE UPDATE, DELETE ON auditoria.evento_auditoria FROM PUBLIC` — teste de integração tenta `DELETE` e espera SQLException.
- **AC-02**: Job de purga **não existe** para auditoria — teste de arquitetura falha se alguma classe agendar `DELETE FROM auditoria.*`.

**REQ-AUD-04** — IP de origem e sessão

- **AC-01**: Toda requisição autenticada propaga `X-Forwarded-For` para o evento via interceptor; ausente → grava `"unknown"` + warn.
- **AC-02**: `idSessao` corresponde ao `jti` do JWT.

**REQ-AUD-05** — exibir todas as ações (inclui EX)

- **AC-01**: Given eventos de ações `EX` no período, When `GET /auditoria?acao=EX`, Then retorna todos.
- **AC-02**: Given `GET /auditoria` sem filtro, Then o histograma de ações inclui contagem de `EX > 0` (corrigindo o bug histórico do `RELAUDIT.NSN`).

### Transversais

**REQ-SYS-01** — APIs REST versionadas + OpenAPI

- **AC-01**: `GET /api/v1/openapi.yaml` retorna o documento — diff contra [openapi.yaml](openapi.yaml) é vazio (CI valida).
- **AC-02**: Springdoc gera Swagger UI em `/swagger-ui.html`.

**REQ-SYS-02** — OAuth2/JWT + perfis

- **AC-01**: Endpoint `GET /auditoria` exige claim `roles` ∈ {`AUD`, `ADM`, `SUP`} — outros perfis recebem `403`.
- **AC-02**: Endpoint `POST /pagamentos:ciclo-mensal` exige `ADM` ou `SUP`.

**REQ-SYS-03** — feature flag de roteamento por contexto

- **AC-01**: Given `sifap.routing.beneficiario = legacy`, When `POST /beneficiarios`, Then a requisição é proxificada para o legado (validado por mock).
- **AC-02**: Mudança do flag em runtime (`refresh scope`) altera o roteamento sem restart.

---

## Open Questions (Não são requisitos — bloqueiam estágios futuros)

| ID | Pergunta | Bloqueia | Quem precisa responder |
|---|---|---|---|
| ~~OQ-01~~ | ~~Qual é a fórmula real do `FATOR-K` em PROGRAMA-SOCIAL e quando deve ser aplicado?~~ **RESPONDIDA em 27/05/2026 pela arqueologia de `CADPROG.NSN`**: `FATOR-K = 1 + (FATOR-REAJUSTE × 0.347215)`, aplicado no cadastro do programa para ajustar `VLR-BASE`. Ver BR-018/BR-019. **Nova questão derivada (OQ-01b)**: o legado pode estar aplicando reajuste duas vezes (uma em CADPROG via FATOR-K, outra em CALCBENF via `(1 + FATOR-REAJUSTE)`). Validar com SENARC se isso é intencional. | Estágio 3 (flip de Pagamento) | ~~SENARC~~ → OQ-01b ainda pendente em SENARC |
| OQ-02 | A não exibição de ações `EX` por `RELAUDIT.NSN` é bug ou requisito? Se requisito, abrir ADR-004 com flag de paridade. | Estágio 3 (auditoria) | Cliente + TCU |
| OQ-03 | Qual a política de purga / arquivamento aceitável para a tabela `pagamento` (180 M+ registros, sem purga desde 1998)? | Estágio 4 (operação) | Compliance + Par 7 (DBA) |
| OQ-04 | Ações `CO` (consulta) devem voltar a ser gravadas na 2.0 ou manter exclusão desde Port. CGTI 213/2010? | Estágio 3 (auditoria) | CGTI |
| OQ-05 | Adabas Event Replicator está disponível no ambiente para feed contínuo legado → 2.0? Se não, ETL diário é aceitável? | Estágio 4 (strangler) | Par 9 (DevOps) + Operações Mainframe |

> Mistérios são preservados em `01-arqueologia/mysteries-found.md` (quando o Par 1 finalizar). Cada OQ acima referencia um mistério catalogado.

---

## Matriz de Rastreabilidade

| REQ-ID | EARS | Contexto | Arquivo legado fonte | Linhas / Campos |
|---|---|---|---|---|
| REQ-BEN-01 | U | Beneficiário | `BENEFICIARIO.ddm` | `AB NUM-CPF` (DE/S1) |
| REQ-BEN-02 | S | Beneficiário | `CALCBENF.NSN`, `VALELEG.NSN` | L154–158 / L102–121 |
| REQ-BEN-03 | U | Beneficiário | `BENEFICIARIO.ddm` | `DA GRP-DEPENDENTE PE (10)` |
| REQ-BEN-04 | U | Beneficiário | `BENEFICIARIO.ddm` | `BA GRP-ENDERECO` |
| REQ-BEN-05 | O | Beneficiário | `BENEFICIARIO.ddm`, `PROGRAMA-SOCIAL.ddm` | `FA–FD` / `CI` |
| REQ-CAT-01 | U | Catálogo | `PROGRAMA-SOCIAL.ddm` | `AA–AD`, `BA–BE` |
| REQ-CAT-02 | U | Catálogo | `PROGRAMA-SOCIAL.ddm` | `DA GRP-FAIXA-CALCULO PE (5)` |
| REQ-CAT-03 | U | Catálogo | `CADPROG.NSN`, `PROGRAMA-SOCIAL.ddm` | `FATOR-REAJUSTE`, `FATOR-K`, `VLR-BASE` (BR-018/019) |
| REQ-CAT-04 | C | Catálogo | `VALELEG.NSN` | L166–177 |
| REQ-CAT-05 | C | Catálogo | `VALELEG.NSN` | L184–190 |
| REQ-CAT-06 | C | Catálogo | `VALELEG.NSN` | L192–198 |
| REQ-CAT-07 | O | Catálogo | `VALELEG.NSN` | L94–98 |
| REQ-PAY-01 | U | Pagamento | `CALCBENF.NSN`, `BATCHPGT.NSN` | L215–222 / L268–272 |
| REQ-PAY-02 | U | Pagamento | `CALCBENF.NSN`, `PROGRAMA-SOCIAL.ddm` | L178–183 / `FA` |
| REQ-PAY-03 | U | Pagamento | `CALCBENF.NSN` | L185–199 |
| REQ-PAY-04 | U | Pagamento | `CALCBENF.NSN` | L203–214 |
| REQ-PAY-05 | E | Pagamento | `CALCBENF.NSN` | L230–237 |
| REQ-PAY-06 | E | Pagamento | `CALCBENF.NSN` | L240–248 |
| REQ-PAY-07 | U | Pagamento | `CALCBENF.NSN` | L225–227, 233–234, 244–245 |
| REQ-PAY-08 | U | Pagamento | `CALCDSCT.NSN` | L60–67, 197–205 |
| REQ-PAY-09 | U | Pagamento | `CALCDSCT.NSN` | L102–106, 160–164 |
| REQ-PAY-10 | U | Pagamento | `PAGAMENTO.ddm` | `CA GRP-DESCONTO PE (8)` |
| REQ-PAY-11 | C | Pagamento | `CALCDSCT.NSN` | L113–119 |
| REQ-PAY-12 | E | Pagamento | `BATCHPGT.NSN` | L198–208 |
| REQ-PAY-13 | U | Pagamento | `BATCHPGT.NSN` | L176–179 |
| REQ-PAY-14 | E | Pagamento | `PAGAMENTO.ddm` | `FA–FE` (2002) |
| REQ-PAY-15 | E | Pagamento | `PAGAMENTO.ddm` | `HA/HB` (2015) |
| REQ-PAY-16 | S | Pagamento | `PAGAMENTO.ddm` | `DA SIT-PAGAMENTO` |
| REQ-AUD-01 | U | Auditoria | `AUDITORIA.ddm` | `BA COD-ACAO` |
| REQ-AUD-02 | E | Auditoria | `AUDITORIA.ddm` | `DA/DD GRP-ANTES/DEPOIS` (2005) |
| REQ-AUD-03 | U | Auditoria | `AUDITORIA.ddm` | Cabeçalho L9–13 (IN-TCU 63/2010) |
| REQ-AUD-04 | U | Auditoria | `AUDITORIA.ddm` | `EE IP-ORIGEM` (2012), `EF ID-SESSAO` |
| REQ-AUD-05 | U | Auditoria | `AUDITORIA.ddm` | Nota final (RELAUDIT filtra EX) |
| REQ-SYS-01 | U | Transversal | GREENFIELD | n/a — capacidade nova |
| REQ-SYS-02 | U | Transversal | `AUDITORIA.ddm` | `EC COD-PERFIL` (perfis legados) |
| REQ-SYS-03 | U | Transversal | GREENFIELD | n/a — strangler |

**Cobertura**: 36 requisitos · 34 com rastreabilidade direta ao legado · 2 greenfield (REQ-SYS-01, REQ-SYS-03) declarados explicitamente.

---

## Handoff para Par 3 (Implementação)

Esta especificação está pronta para ser refinada e estimada. Próximos passos para o Par 3:

1. Refinar cada `REQ-*` em critérios de aceite executáveis (Given/When/Then).
2. Mapear cada requisito para `@Test` (JUnit 5 / Vitest) — meta: cobertura ≥ 80 % nas regras de cálculo.
3. Implementar na ordem da migração definida em [ADR-003 § Decisão item 2](ADRs/ADR-003-strangler-coexistencia-siafi.md): Catálogo → Beneficiário → Pagamento → Auditoria.

**Versão**: 0.1 · **Data**: 19/05/2026 · **Estado**: Pronta para Par 3.
