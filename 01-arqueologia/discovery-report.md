<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Relatório de Descoberta — Estágio 1: Arqueologia Digital

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **discovery-report**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Este documento consolida todas as descobertas do Estágio 1.
> Preencha cada seção com as conclusões do time. **Este é o input principal do Estágio 2** — sem ele, a especificação vira chute.

**Time**: Vermelho-02 (workshop SIFAP 2.0)
**Data**: 27/05/2026
**Edição**: v1.0 — base para Estágio 2
**Participantes**: Par 1 (Product Owner + Requirements Engineer); Par 2 (Enterprise Architect + Software Architect); Par 3 (Tech Lead + Developer); Par 4 (DBA + QA + DevOps + Tech Writer).

---

## 1. Sumário Executivo

O SIFAP é um sistema **Natural/Adabas** em produção há ~25 anos para gerir cadastro de beneficiários, parametrização de programas sociais, ciclo mensal de pagamentos e auditoria, com integração crítica ao SIAFI via CNAB 240. São **15 programas Natural** (4 online, 3 batch, 2 relatórios, 6 sub-rotinas) e **4 DDMs Adabas**. O código está **estruturado e disciplinado** (toda escrita passa por auditoria imutável; ciclos idempotentes por `(CPF, COMPETENCIA)`), mas concentra **regras críticas em poucos arquivos** (`CALCBENF`, `CALCDSCT`, `VALELEG`, `BATCHPGT`) e carrega **mistérios não documentados** — em especial o `FATOR-K` (cadastrado mas não usado em fórmulas desde 2008) e o filtro silencioso de ações `EX` no `RELAUDIT.NSN`. Criticidade: **alta** (fluxo financeiro federal regulado por IN-TCU 63/2010 e Lei 8159).

---

## 2. Visão Geral do Sistema

### 2.1 Propósito do SIFAP

SIFAP (Sistema de Identificação e Folha de Apoio) executa quatro funções centrais:

1. **Cadastro de beneficiários** e até 10 dependentes por beneficiário, com endereço completo (CEP+IBGE+UF+região) e biometria opcional.
2. **Catálogo paramétrico** de programas sociais (tipos A/T/P, valores base, faixas de cálculo, fatores regionais, percentual de reajuste).
3. **Ciclo mensal de pagamentos**: validação de elegibilidade → cálculo do benefício → aplicação de descontos → idempotência por competência → geração de remessa CNAB 240 → integração com SIAFI (OB, NE, UG).
4. **Auditoria imutável** de toda operação (inclusão, alteração, exclusão, login, batch, erro), com retenção mínima de 10 anos.

### 2.2 Arquitetura Legada

- **15 programas Natural** divididos em 4 categorias (online / batch / relatórios / sub-rotinas via `CALLNAT`).
- **4 DDMs Adabas**: `BENEFICIARIO`, `PAGAMENTO`, `PROGRAMA-SOCIAL`, `AUDITORIA`.
- **Padrões Adabas críticos para migração**:
  - Descriptors (`AB NUM-CPF`, etc.) → viram índices em PostgreSQL.
  - Periodic Groups (`PE`): dependentes (max 10), faixas de cálculo (max 5), descontos (max 8) → JSONB ou tabela filha.
  - Multiple-Value Fields (`MU`): grupos `ANTES`/`DEPOIS` da auditoria (max 20) → JSONB.
  - Superdescriptors compostos → índices compostos.
- **Fluxo principal**: Terminal 3270 → `CAD*`/`CONS*` → DDM. Scheduler noturno → `BATCHPGT` → `VALELEG` + `CALCBENF` + `CALCDSCT` → `PAGAMENTO` → SIAFI/CNAB.
- **Cross-cutting**: todos os programas escrevem em `AUDITORIA` (append-only).
- Veja [dependency-map.md](dependency-map.md) para o grafo completo.

### 2.3 Usuários e Perfis

Perfis legados (campo `EC COD-PERFIL` em `AUDITORIA.ddm`):

| Perfil | Significado | Pode |
|---|---|---|
| `ADM` | Administrador | Tudo (incluindo cadastro de programas e usuários). |
| `OPR` | Operador | Cadastro e alteração de beneficiários/dependentes; lançamento de descontos. |
| `CON` | Consulta | Read-only sobre beneficiários e pagamentos. |
| `AUD` | Auditor | Read-only sobre `AUDITORIA` e relatórios regulatórios. |
| `SUP` | Suporte | Read-only ampliado + reprocessamento de batch (com aprovação). |

---

## 3. Principais Descobertas

### 3.1 Regras de Negócio Críticas

1. **BR-001** — Beneficiário com status ≠ `A` não gera pagamento. Gate de elegibilidade. Ver [business-rules-catalog.md#regras-encontradas](business-rules-catalog.md).
2. **BR-002** — Fórmula central de cálculo do benefício (`VLR_BASE × FATOR_REG × FATOR_FAM × FATOR_RND × FATOR_IDADE × (1 + reajuste)`), duplicada em `CALCBENF` e `BATCHPGT`.
3. **BR-010** — Teto de 30% sobre descontos, com exceção legal para tipo `J` (Judicial).
4. **BR-012** — Idempotência: no máximo um pagamento por `(CPF, COMPETENCIA)`. UNIQUE obrigatório.
5. **BR-018** — Auditoria imutável com retenção mínima de 10 anos (IN-TCU 63/2010 + Lei 8159). Append-only, sem UPDATE/DELETE.

### 3.2 Dependências Complexas

- **`BATCHPGT.NSN` é o ponto mais acoplado**: depende de `BENEFICIARIO`, `PROGRAMA-SOCIAL` e `PAGAMENTO` simultaneamente, e chama `VALELEG` + `CALCBENF` + `CALCDSCT`. Falha aqui paralisa o ciclo mensal inteiro.
- **Fórmula central duplicada** entre `CALCBENF` (uso online) e `BATCHPGT` (uso batch) — risco de divergência se uma cópia for alterada e a outra não. Migrar como serviço único `PagamentoService.calcular(...)`.
- **Cross-cutting de auditoria**: todos os 15 programas escrevem em `AUDITORIA`. Em modular monolith vira `@DomainEventListener` único.
- **Contrato de ordenação por CPF (BR-013)**: `BATCHPGT` ordena por CPF asc e sistemas downstream dependem disso desde 1999. Não detectável por testes unitários — só por contrato integrado.

### 3.3 Dívida Técnica Identificada

- [ ] **Tabela `parametro_regional` hardcoded** em `CALCBENF.NSN` linhas 95–122 — qualquer mudança de fator regional exige recompilação. Migrar para `PROGRAMA-SOCIAL.GRP-PARAM-REGIONAL` paramétrico.
- [ ] **`FATOR-K` órfão** em `PROGRAMA-SOCIAL.ddm` (campo `BG`, inserido 08/2008 com comentário "NAO DOCUMENTADO"). Decisão arquitetural: preservar valor, não usar em fórmulas (REQ-CAT-03; ADR-002 item 5). Bloqueio formal em SENARC (OQ-01).
- [ ] **Filtro silencioso de ações `EX`** em `RELAUDIT.NSN` — nota em `AUDITORIA.ddm` final alerta para o comportamento, mas não diz se é bug ou requisito. Decisão temporária: REQ-AUD-05 exibe tudo. Bloqueio em auditoria interna (OQ-02).
- [ ] **Truncamento manual a 2 casas** com gambiarra `× 100 / 100` no `CALCBENF` — migrar para `BigDecimal.setScale(2, RoundingMode.DOWN)`.
- [ ] **Sem versão de API**: acesso exclusivo por terminal 3270. Toda integração externa hoje é via batch/arquivo.
- [ ] **Sem testes automatizados** detectáveis no legado — única validação histórica é o reprocessamento manual em ambiente espelho.

### 3.4 Gaps de Documentação

- Semântica do `FATOR-K` (BR-020).
- Origem do filtro `EX` no `RELAUDIT`.
- Política de purga / retenção de `PAGAMENTO` (auditoria tem 10 anos definidos; pagamento não — OQ-03).
- Se ações `CO` (consulta) deveriam continuar sendo auditadas em 2.0 (LGPD impactou o trade-off — OQ-04).
- Disponibilidade de `Adabas Event Replicator` no ambiente para shadow read (OQ-05).

---

## 4. Mistérios e Riscos

### 4.1 Mistérios Não Resolvidos

| ID | Descrição | Risco para Migração |
|---|---|---|
| M-01 | `FATOR-K` no `PROGRAMA-SOCIAL` (campo `BG`, 08/2008) — semântica desconhecida. | **ALTO** se for usado por algum órgão upstream; **BAIXO** se realmente nunca foi ativado. Mitigação: preservar valor, não usar em fórmula (REQ-CAT-03). |
| M-02 | `RELAUDIT.NSN` filtra ações `EX` (exclusão) sem explicação. | **MÉDIO** — pode ser requisito regulatório camuflado. Mitigação: REQ-AUD-05 exibe tudo; validar com auditoria. |
| M-03 | Política de purga de `PAGAMENTO` não documentada (auditoria tem 10 anos; pagamento não). | **MÉDIO** — afeta dimensionamento de storage e particionamento. |
| M-04 | Auditoria de `CO` (consulta) — alto custo de I/O; pode haver decisão de desligar em 2.0 sob LGPD. | **BAIXO** — decisão arquitetural reversível. |
| M-05 | Disponibilidade do `Adabas Event Replicator` para coexistência (shadow read). | **ALTO** se ausente — força CDC custom ou batch sync. Bloqueia ADR-003 plenamente. |

### 4.2 Riscos para o Estágio 2

1. **Idempotência do batch** (BR-012) é não-negociável — quebrar gera pagamento duplicado.
2. **Ordenação por CPF** (BR-013) é contrato silencioso com downstream — não detectável por unit tests.
3. **Truncamento, não arredondamento** (BR-008) — usar `BigDecimal.setScale(2, RoundingMode.DOWN)` em **todas** as operações financeiras.
4. **Imutabilidade da auditoria** (BR-018) exige REVOKE de UPDATE/DELETE em PostgreSQL — não confiar só em código.
5. **`FATOR-K`** (BR-020) precisa estar no schema mas **ausente das fórmulas** até decisão SENARC.

---

## 5. Recomendações

### 5.1 O que migrar primeiro

Ordem casada com o plano Strangler ([ADR-003](../02-spec-moderna/ADRs/ADR-003-strangler-coexistencia-siafi.md)):

| Prioridade | Funcionalidade | Justificativa |
|---|---|---|
| 1 | **Catálogo de Programas Sociais** (`CADPROG`, `PROGRAMA-SOCIAL.ddm`) | Menor risco, dados paramétricos. Habilita os demais contextos. |
| 2 | **Beneficiário + Dependentes** (`CADBENEF`, `CADDEPEND`, `CONSBENF`) | Base para qualquer operação. Volume controlável. |
| 3 | **Pagamento — ciclo mensal** (`BATCHPGT` + `CALCBENF` + `CALCDSCT` + `VALELEG`) | Núcleo crítico. Migrar depois que 1 e 2 estiverem estáveis. |
| 4 | **Auditoria** (`AUDITORIA.ddm` + `RELAUDIT`) | Cross-cutting; vira `@DomainEventListener` consumindo eventos dos contextos anteriores. |

### 5.2 O que descartar

- **Relatórios em terminal 3270 (`RELPGT`, `RELAUDIT` em formato impresso)**: substituir por API + dashboard moderno (Next.js). Lógica preserva-se; saída muda completamente.
- **`BATCHCON.NSN` (consultas em lote para TCU/CGU)**: substituir por API REST pesquisável + export sob demanda.
- **Tabela `parametro_regional` hardcoded** em `CALCBENF` (linhas 95–122): descartar e mover para `PROGRAMA-SOCIAL` paramétrico.
- **Uso de `FATOR-K`**: descartar até decisão SENARC. Campo preservado por compatibilidade de dados.

### 5.3 O que evoluir

- **Auditoria**: migrar **e** corrigir o filtro silencioso de ações `EX` (REQ-AUD-05); adicionar query estruturada e exportação JSON.
- **Ciclo de pagamento**: preservar todas as fórmulas, **mas** adicionar `dry-run`/preview antes do flip, e métricas de divergência por competência.
- **Cadastro de beneficiário**: substituir CRUD em 3270 por API + UI; adicionar validação de CEP via ViaCEP em tempo real.
- **Integração SIAFI**: manter contrato CNAB 240, **mas** adicionar webhook de confirmação + reconciliação automática (hoje é manual).

---

## 6. Métricas do Estágio

| Métrica | Valor |
|---|---|
| Programas analisados | **15 / 15** |
| DDMs mapeados | **4 / 4** |
| Regras de negócio encontradas | **20** |
| Regras escondidas encontradas | **10 / 10** |
| Easter eggs encontrados | a confirmar com facilitação |
| Termos no glossário | **40** |
| Mistérios catalogados | **5** (M-01 a M-05) |
| Tempo total gasto | n/d (arqueologia consolidada por Par 2 durante Estágio 2) |

---

## 7. Notas para o Próximo Estágio

- A `SPECIFICATION.md` (Estágio 2) já consome este relatório: todos os REQ-IDs têm `source_legacy:` apontando para arquivos/linhas dos DDMs e NSNs aqui descritos.
- **5 perguntas pendentes (OQ-01..OQ-05)** estão registradas em [SPECIFICATION.md § Open Questions](../02-spec-moderna/SPECIFICATION.md) — duas delas (OQ-01 e OQ-02) são bloqueios externos que o PO precisa endereçar.
- A decisão de migrar em Strangler (Catálogo → Beneficiário → Pagamento → Auditoria) está formalizada em [ADR-003](../02-spec-moderna/ADRs/ADR-003-strangler-coexistencia-siafi.md). O plano só fecha quando OQ-05 (Event Replicator) for respondida.

---

## Definição de Pronto deste relatório

- [x] Todas as seções acima preenchidas (sem placeholders).
- [x] Pelo menos 5 regras críticas listadas em §3.1, cada uma referenciando uma `BR-XXX` do catálogo.
- [x] Decisões de migrar/descartar/evoluir em §5 cobrem as 8+ funcionalidades principais.
- [x] Métricas de §6 conferem com os outros artefatos (glossary.md = 40 termos, business-rules-catalog.md = 20 BRs, esta lista de mistérios = 5).

— Paula


---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="mysteries-found.md"><strong>mysteries-found.md</strong></a><br/>
<sub>Lista de mistérios.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="../02-spec-moderna/GUIDE.md"><strong>Estágio 2 — Spec</strong></a><br/>
<sub>Próximo estágio: spec moderna.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>

