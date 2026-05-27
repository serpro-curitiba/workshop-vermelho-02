<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Catálogo de Regras de Negócio — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **business-rules-catalog**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Registre aqui todas as regras de negócio extraídas do código Natural/Adabas.
> Cada regra precisa ter rastreabilidade até o código-fonte.
>
> **REGRA DURA:** linhas com `Programa Fonte` vazio são **inválidas** e não contam para o gate do Estágio 2. Use o formato `01-arqueologia/legado-sifap/natural-programs/ARQUIVO.NSN#L<inicio>-L<fim>` sempre que possível. Mínimo aceito: nome do arquivo .NSN.

## Como pensar em "regra de negócio"

O que conta:

- Um `IF` que decide algo no domínio (ex.: _"se a UF é do Nordeste e o programa é Seca, valor base × 1.2"_)
- Uma constante numérica sem explicação (ex.: `0.075` num cálculo de imposto)
- Uma transição de status com regra (ex.: _"só de A para S, nunca de I para A"_)
- Um tratamento especial para um caso (ex.: _"se o CPF começa com 999, é teste"_)

O que NÃO conta: paginação de relatório, formatação de saída, manipulação de cursor Adabas, abertura de arquivo. Ignore esses detalhes de implementação.

## Níveis de Risco

| Nível       | Descrição                                                     |
| ----------- | ------------------------------------------------------------- |
| **CRÍTICO** | Regra financeira ou de segurança — erro causa prejuízo direto |
| **ALTO**    | Regra de negócio central — afeta fluxo principal              |
| **MÉDIO**   | Regra de validação ou formatação — afeta qualidade dos dados  |
| **BAIXO**   | Regra de apresentação ou conveniência — impacto limitado      |

## Regras Encontradas

> **Conflito de merge resolvido em 27/05/2026:** duas frentes de arqueologia independentes (`CAD*` × `CALC*/VAL*/BATCH*/AUDITORIA`) consolidadas. BR-001..BR-020 cobrem **cadastros** (programas online `CADBENEF`, `CADDEPEND`, `CADPROG`); BR-021..BR-039 cobrem **cálculo, pagamento e auditoria** (programas batch e sub-rotinas). Antiga BR-020 ("FATOR-K não documentado") **descartada** porque BR-018/BR-019 demonstram que o campo **é** usado em `CADPROG.NSN` para derivar `VLR-BASE`. OQ-01 fica respondida (ver §Resumo).

### Cadastros (programas online: CADBENEF / CADDEPEND / CADPROG)

| ID | Regra de Negócio | Programa Fonte | Campos DDM | Nível de Risco | Notas |
|---|---|---|---|---|---|
| BR-001 | O cadastro de beneficiário só aceita operações `I` (inclusão) ou `A` (alteração). | `CADBENEF.NSN` | `BENEFICIARIO.OPERACAO` | ALTO | Regra de entrada do fluxo. |
| BR-002 | CPF do beneficiário é obrigatório para cadastrar/alterar. | `CADBENEF.NSN` | `BENEFICIARIO.CPF` | ALTO | Chave funcional primária no processo. |
| BR-003 | CPF do beneficiário deve ser válido pelo algoritmo módulo 11. | `CADBENEF.NSN` | `BENEFICIARIO.CPF` | CRÍTICO | Falha pode gerar cadastro inválido e pagamento indevido. |
| BR-004 | Nome do beneficiário é obrigatório. | `CADBENEF.NSN` | `BENEFICIARIO.NOME` | MÉDIO | Regra de qualidade de cadastro. |
| BR-005 | Sexo do beneficiário só aceita `M` ou `F`. | `CADBENEF.NSN` | `BENEFICIARIO.SEXO` | MÉDIO | Domínio fechado legado. |
| BR-006 | Inclusão de beneficiário é bloqueada quando CPF já existe. | `CADBENEF.NSN` | `BENEFICIARIO.CPF` | CRÍTICO | Evita duplicidade de benefício. |
| BR-007 | Alteração de beneficiário só ocorre se o CPF já existir. | `CADBENEF.NSN` | `BENEFICIARIO.CPF` | ALTO | Evita atualização fantasma. |
| BR-008 | Na inclusão, beneficiário inicia com status `A`. | `CADBENEF.NSN` | `BENEFICIARIO.STATUS` | ALTO | Regra de estado inicial. |
| BR-009 | Beneficiário com idade superior a 75 anos recebe status `S`. | `CADBENEF.NSN` | `BENEFICIARIO.DT-NASCIMENTO`, `BENEFICIARIO.STATUS` | ALTO | Regra especial etária prevalece sobre status inicial. |
| BR-010 | Não permite incluir dependente para titular com status `C` ou `D`. | `CADDEPEND.NSN` | `BENEFICIARIO.STATUS`, `BENEFICIARIO.CPF` | ALTO | Regra de elegibilidade do titular. |
| BR-011 | Há limite máximo de dependentes por titular. | `CADDEPEND.NSN` | `BENEFICIARIO.NUM-DEPENDENTES`, `BENEFICIARIO.DEPENDENTES.*` | ALTO | Condição atual indica possível off-by-one (validar contra `PE max 10`). |
| BR-012 | Parentesco de dependente só aceita `FI`, `CO`, `IR` ou `OU`. | `CADDEPEND.NSN` | `BENEFICIARIO.DEPENDENTES.PARENTESCO` | MÉDIO | Domínio fechado de parentesco. |
| BR-013 | Não permite CPF de dependente duplicado para o mesmo titular. | `CADDEPEND.NSN` | `BENEFICIARIO.CPF`, `BENEFICIARIO.DEPENDENTES.CPF-DEP` | ALTO | Deduplicação é local por titular. |
| BR-014 | Inclusão de dependente incrementa `NUM-DEPENDENTES` do titular. | `CADDEPEND.NSN` | `BENEFICIARIO.NUM-DEPENDENTES`, `BENEFICIARIO.DEPENDENTES.*` | ALTO | Impacta regras de elegibilidade e cálculo (BR-027). |
| BR-015 | Cadastro de programa social só aceita operações `I` (inclusão) ou `C` (consulta). | `CADPROG.NSN` | `PROGRAMA-SOCIAL.OPERACAO` | MÉDIO | Regra de fluxo operacional. |
| BR-016 | Operação `C` executa consulta somente leitura por código do programa. | `CADPROG.NSN` | `PROGRAMA-SOCIAL.COD-PROGRAMA` | BAIXO | Não altera dados. |
| BR-017 | Inclusão de programa é bloqueada quando código já existe. | `CADPROG.NSN` | `PROGRAMA-SOCIAL.COD-PROGRAMA` | ALTO | Garante unicidade de programa. |
| BR-018 | `FATOR-K` é calculado por `1 + (fator_reajuste × 0.347215)`. | `CADPROG.NSN` | `PROGRAMA-SOCIAL.FATOR-REAJUSTE`, `PROGRAMA-SOCIAL.FATOR-K` | CRÍTICO | **Constante mágica `0.347215` sensível**. Responde a OQ-01: FATOR-K **é** usado, derivado no momento do cadastro do programa. |
| BR-019 | Valor armazenado do programa é `VLR-BASE × FATOR-K`. | `CADPROG.NSN` | `PROGRAMA-SOCIAL.VLR-BASE`, `PROGRAMA-SOCIAL.FATOR-K` | CRÍTICO | Campo `VLR-BASE` passa a carregar valor já ajustado — atenção para não reaplicar reajuste em `CALCBENF`. |
| BR-020 | Programa novo é criado com status ativo `A`. | `CADPROG.NSN` | `PROGRAMA-SOCIAL.STATUS-PROG` | ALTO | Estado inicial afeta uso imediato em elegibilidade. |

### Cálculo, Pagamento e Auditoria (batch e sub-rotinas)

| ID | Regra de Negócio | Programa Fonte | Campos DDM | Nível de Risco | Notas |
|---|---|---|---|---|---|
| BR-021 | Beneficiário com status diferente de `A` (Ativo) **não pode** gerar pagamento. | `CALCBENF.NSN#L154-L158` + `VALELEG.NSN#L102-L121` | `BENEFICIARIO.STATUS` | CRÍTICO | Gate de elegibilidade. Status válidos: A/S/C/I/D. Casa com BR-008/BR-009. |
| BR-022 | Cálculo do valor bruto = `VLR_BASE × FATOR_REG × FATOR_FAM × FATOR_RND × FATOR_IDADE × (1 + FATOR_REAJUSTE)`. | `CALCBENF.NSN#L215-L222` (confirmado em `BATCHPGT.NSN#L268-L272`) | `PROGRAMA-SOCIAL.VLR-BASE`, `BENEFICIARIO.COD-REG`, dependentes, idade | CRÍTICO | Fórmula central duplicada. **Atenção**: `VLR-BASE` aqui já vem ajustado por BR-019 — não reaplicar `FATOR-K`. |
| BR-023 | Fator regional via tabela `parametro_regional[UF]` com fallback `1.0000` se UF ausente. | `CALCBENF.NSN#L178-L183` (tabela hardcoded `L95-L122`) | `BENEFICIARIO.UF` | ALTO | Hardcoded no legado; migra para `PROGRAMA-SOCIAL.GRP-PARAM-REGIONAL`. |
| BR-024 | Fator familiar por faixa de dependentes ativos (0 / 1–2 / 3–4 / ≥5). | `CALCBENF.NSN#L185-L199` | `BENEFICIARIO.GRP-DEPENDENTE.SIT-DEPEND` | ALTO | Só conta dependentes com `SIT='A'`. |
| BR-025 | Fator idade: ≥65 → 1.1500; 60–64 → 1.1000; <18 → 1.0500; 18–59 → 1.0000. | `CALCBENF.NSN#L203-L214` | `BENEFICIARIO.DT-NASC` | ALTO | Calcular idade na **data de competência**, não data corrente. |
| BR-026 | Em dezembro (`MM=12`) somar 13º = `VLR_BASE × FATOR_REG × FATOR_IDADE`. | `CALCBENF.NSN#L230-L237` | competência | ALTO | Aplica a todos os tipos de programa. |
| BR-027 | Em dezembro **e** tipo `A` (Assistencial) somar **abono natalino** de 15% sobre `VLR_BENF`. | `CALCBENF.NSN#L240-L248` | `PROGRAMA-SOCIAL.TIPO-PROGRAMA` | ALTO | Empilha com o 13º (somam-se). |
| BR-028 | Truncar (não arredondar) valores a 2 casas decimais após cada operação. | `CALCBENF.NSN#L225-L227`, `L233-L234`, `L244-L245` | todos os valores | CRÍTICO | Padrão `BigDecimal.setScale(2, RoundingMode.DOWN)`. Divergência aqui gera centavos. |
| BR-029 | Contribuição social compulsória por faixa de bruto: ≤500→3%, ≤1000→5%, ≤2000→7%, >2000→9%. | `CALCDSCT.NSN#L60-L67` + `L197-L205` | `PAGAMENTO.VLR-BRUTO` | CRÍTICO | Cobre todos os pagamentos sem exceção. |
| BR-030 | Total de descontos não pode exceder 30% do bruto, **exceto** tipo `J` (Judicial) que ignora teto. | `CALCDSCT.NSN#L102-L106` + `L160-L164` | `PAGAMENTO.VLR-BRUTO`, `PAGAMENTO.GRP-DESCONTO.TIPO-DESCONTO` | CRÍTICO | Regra financeira federal. Tipo `J` = exceção legal. |
| BR-031 | Descontos com `DT-FIM-DSCT` no passado **não** devem ser aplicados. | `CALCDSCT.NSN#L113-L119` | `PAGAMENTO.GRP-DESCONTO.DT-FIM-DSCT` | ALTO | Validar contra data de processamento, não data corrente. |
| BR-032 | Idempotência: no máximo um pagamento por `(CPF, COMPETENCIA)`. | `BATCHPGT.NSN#L198-L208` | `PAGAMENTO.CPF-BENEF`, `PAGAMENTO.COMPETENCIA` | CRÍTICO | Constraint UNIQUE obrigatória em PostgreSQL. |
| BR-033 | Batch processa beneficiários **em ordem ascendente de CPF** (contrato downstream desde 1999). | `BATCHPGT.NSN#L176-L179` | `BENEFICIARIO.NUM-CPF` (superdescriptor S1) | ALTO | Sistemas downstream dependem dessa ordem. Quebrar = incidente. |
| BR-034 | Programa `A` (Assistencial) com renda >600 sem dependentes → **inelegível**. | `VALELEG.NSN#L166-L177` | `BENEFICIARIO.VLR-RENDA-FAM`, dependentes | ALTO | Motivo: "PROG ASSISTENCIAL: RENDA > 600 SEM DEPENDENTES". |
| BR-035 | Programa `P` (Previdenciário) com idade <60 → **inelegível**. | `VALELEG.NSN#L184-L190` | idade calculada | ALTO | — |
| BR-036 | Programa `T` (Trabalho) com idade fora de `[16,65]` → **inelegível**. | `VALELEG.NSN#L192-L198` | idade calculada | ALTO | — |
| BR-037 | Região `99` (especial/internacional) → **automaticamente elegível**, ignora demais validações. | `VALELEG.NSN#L94-L98` | `BENEFICIARIO.COD-REG` | MÉDIO | Bypass intencional para casos diplomáticos/missões. |
| BR-038 | Auditoria é **imutável**: nenhum UPDATE ou DELETE permitido. Retenção mínima 10 anos. | `AUDITORIA.ddm` cabeçalho `L9-L13` | toda a tabela | CRÍTICO | IN-TCU 63/2010 + Art. 14 Lei 8159. Implementar como append-only + REVOKE. |
| BR-039 | Auditoria de alteração preserva estado **antes e depois** (até 20 campos). | `AUDITORIA.ddm` grupos `DA GRP-ANTES`, `DD GRP-DEPOIS` | PE/MU max 20 | ALTO | Em PostgreSQL: JSONB `antes` e `depois`. |

### Regras escondidas confirmadas (10 de 10)

Cobertas por: BR-018 (fórmula FATOR-K com constante mágica `0.347215`), BR-026 (13º), BR-027 (abono natalino), BR-028 (truncamento), BR-030 (exceção tipo J), BR-033 (ordem CPF), BR-034/35/36 (regras de elegibilidade por tipo de programa), BR-037 (bypass região 99) e BR-038 (imutabilidade auditoria).

> Adicione mais linhas conforme novas descobertas.

## Exemplo de linha bem preenchida

| ID     | Regra de Negócio                                                                        | Programa Fonte                                   | Campos DDM                                                               | Nível de Risco | Notas                                      |
| ------ | --------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------ | -------------- | ------------------------------------------ |
| BR-013 | Desconto total não pode exceder 30% do valor bruto, exceto descontos judiciais (tipo J) | `01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L142-L148` | `PAGAMENTO.VLR-BRUTO`, `PAGAMENTO.VLR-TOTAL-DSCT`, `PAGAMENTO.TIPO-DSCT` | CRÍTICO        | Regra financeira. Tipo 'J' = exceção legal |

## Regras por Categoria

### Cálculos Financeiros

- **BR-018**: Fator K é calculado por `1 + (fator reajuste * 0.347215)`.
  Impacto: define a fórmula de reajuste financeiro do programa social.
- **BR-019**: Valor armazenado do programa é `VLR-BASE * FATOR-K`.
  Impacto: altera diretamente o valor-base utilizado no sistema.
- **BR-014**: Inclusão de dependente incrementa `NUM-DEPENDENTES` do titular.
  Impacto: pode influenciar regras futuras de elegibilidade ou cálculo por composição familiar.

### Validações de Status

- **BR-008**: Na inclusão, beneficiário inicia com status `A`.
  Impacto: define o estado inicial do cadastro.
- **BR-009**: Beneficiário com idade superior a 75 anos recebe status `S`.
  Impacto: sobrescreve o status inicial por regra etária especial.
- **BR-010**: Não permite incluir dependente para titular com status `C` ou `D`.
  Impacto: restringe manutenção cadastral para titulares inelegíveis.
- **BR-020**: Programa novo é criado com status ativo `A`.
  Impacto: torna o programa imediatamente utilizável no processo.

### Regras de Autorização

- **BR-001**: O cadastro de beneficiário só aceita operações `I` (inclusão) ou `A` (alteração).
  Impacto: controla quais ações são permitidas na rotina.
- **BR-007**: Alteração de beneficiário só ocorre se o CPF já existir.
  Impacto: impede alteração de registro inexistente.
- **BR-015**: Cadastro de programa social só aceita operações `I` (inclusão) ou `C` (consulta).
  Impacto: restringe as ações válidas na manutenção de programas.
- **BR-016**: Operação `C` executa consulta somente leitura por código do programa.
  Impacto: separa explicitamente leitura de escrita.
- **BR-017**: Inclusão de programa é bloqueada quando código já existe.
  Impacto: impede criação duplicada de programa social.

### Regras de Negócio Temporais

- **BR-009**: Beneficiário com idade superior a 75 anos recebe status `S`.
  Impacto: regra dependente da idade apurada a partir da data de nascimento.
- **BR-018**: Fator K é calculado a partir do fator de reajuste vigente no cadastro do programa.
  Impacto: embute regra de atualização temporal/econômica no valor do programa.
- **BR-020**: Programa novo é criado com status ativo `A` na data de inclusão.
  Impacto: caracteriza vigência imediata do programa salvo regra posterior.

## Resumo Estatístico

- Total de regras encontradas: **39**
- Regras críticas: **11** — BR-003, BR-006, BR-018, BR-019, BR-021, BR-022, BR-028, BR-029, BR-030, BR-032, BR-038
- Regras com duplicação: **1** — BR-022 (fórmula central duplicada em `CALCBENF.NSN` e `BATCHPGT.NSN`)
- Regras sem documentação (escondidas): **10** — todas catalogadas (ver §Regras escondidas confirmadas)
- **OQ-01 (FATOR-K) respondida**: BR-018/BR-019 demonstram uso real do campo. Atualizar `SPECIFICATION.md` REQ-CAT-03 e `mysteries-found.md` MYS-001 em iteração futura.

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="GUIDE.md"><strong>GUIDE do Estágio 1</strong></a><br/>
<sub>Passo a passo do estágio.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="dependency-map.md"><strong>dependency-map.md</strong></a><br/>
<sub>Mapa de quem chama quem.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

