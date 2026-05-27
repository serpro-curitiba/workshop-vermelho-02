<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Decisões de Escopo — SIFAP 2.0

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S2](https://img.shields.io/badge/PREENCHA-Durante%20S2-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 2](README.md) → **Scope Decisions**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 2 (Spec Moderna).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento preenchido para sua feature
> 2. Rastreabilidade `source_legacy:` para cada REQ-ID
> 3. Sign-off do Product Owner antes da passagem H2
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Para cada funcionalidade encontrada no Estágio 1, decida: **Migrar**, **Descartar** ou **Evoluir**.
>
> - **Migrar**: trazer para o SIFAP 2.0 como está (mesma lógica, nova tecnologia)
> - **Descartar**: não trazer — funcionalidade obsoleta ou desnecessária
> - **Evoluir**: trazer E melhorar (nova UX, novo fluxo, nova capacidade)

**Time**: Vermelho-02
**Data**: 27/05/2026
**Edição**: v1.0 — handoff para Estágio 3
**Par 1 (Product Owner) responsável**: a definir (preenchimento delegado ao Par 2 com pendência de sign-off do PO na Passagem #2)

## Por que isso importa

O escopo é o que protege o time de chegar às 17h00 com 12 features pela metade. Se o Par 1 não cortar, o Estágio 3 não fecha. **Decisão difícil é tomada aqui, não no Estágio 3.**

## Como decidir

Pergunte de cada funcionalidade:

1. **Afeta o ciclo mensal de pagamento?** Sim → Migrar. Não → considere descartar.
2. **Tem uso documentado nos últimos 12 meses?** Não → descartar.
3. **Faz parte de um relatório regulatório obrigatório (TCU, CGU, BB)?** Sim → Migrar como está.
4. **Tem uma versão moderna mais barata de implementar?** Sim → Evoluir.

---

## Decisões por Funcionalidade

| # | Funcionalidade | Decisão | Justificativa | Regra de Negócio (BR-XXX) | Prioridade |
|---|---|---|---|---|---|
| 1 | Cadastro de Beneficiários + Dependentes (`CADBENEF`, `CADDEPEND`) | **Evoluir** | Preservar regras (BR-003, BR-005); evoluir UX (formulário web) e adicionar ViaCEP. | BR-003, BR-005 | Alta |
| 2 | Consulta de Beneficiários (`CONSBENF`) | **Evoluir** | Substituir terminal 3270 por API REST + dashboard. Manter perfis (BR-018 mapeamento). | REQ-SYS-02 | Alta |
| 3 | Catálogo de Programas Sociais (`CADPROG`, `PROGRAMA-SOCIAL.ddm`) | **Evoluir** | Mover `parametro_regional` (hoje hardcoded em `CALCBENF`) para tabela paramétrica. | BR-003 | Alta |
| 4 | Ciclo mensal de Pagamento (`BATCHPGT`) | **Migrar** | Preservar 100% das regras (BR-002, BR-008, BR-010, BR-012, BR-013). Mesmo comportamento, nova tecnologia. | BR-001, BR-002, BR-006, BR-007, BR-009, BR-010, BR-011, BR-012, BR-013 | Alta |
| 5 | Cálculo de Benefícios (`CALCBENF`) | **Migrar** | Fórmula central duplicada — unificar em `PagamentoService.calcular`. | BR-002, BR-003, BR-004, BR-005, BR-006, BR-007, BR-008 | Alta |
| 6 | Cálculo de Descontos (`CALCDSCT`) | **Migrar** | Mesmo critério; regras críticas BR-009/010/011 preservadas. | BR-009, BR-010, BR-011 | Alta |
| 7 | Validação de Elegibilidade (`VALELEG`) | **Migrar** | Regras por tipo (BR-014/15/16/17) e bypass região 99. | BR-001, BR-014, BR-015, BR-016, BR-017 | Alta |
| 8 | Correção monetária (`CALCCORR`) | **Migrar** | Aplicar `(1 + reajuste)` na fórmula central. | parte de BR-002 | Média |
| 9 | Validação de cadastro/docs (`VALBENEF`, `VALDOCS`) | **Evoluir** | Modernizar validações (CPF dígito verificador, formato, deduplicação). | REQ-BEN-01 | Média |
| 10 | Relatórios (`RELPGT`, `RELAUDIT`) | **Evoluir** | Substituir impressão 3270 por dashboard + export. Corrigir filtro silencioso de `EX` (REQ-AUD-05). | BR-018 | Média |
| 11 | Auditoria (`AUDITORIA.ddm`) | **Migrar** | Append-only + REVOKE UPDATE/DELETE no PostgreSQL. Retenção 10 anos preservada. | BR-018, BR-019 | Alta |
| 12 | Consultas em lote (`BATCHCON`) | **Descartar** | Substituir por API REST com filtros. Órgãos de controle exportam sob demanda. | — | Baixa |
| 13 | Orquestrador de relatórios (`BATCHREL`) | **Descartar** | Vira scheduler (cron/Quartz) chamando endpoints REST. | — | Baixa |
| 14 | Tabela `parametro_regional` hardcoded em `CALCBENF` (L95–122) | **Descartar** | Mover para parâmetro (item 3). | parte de BR-003 | Alta |
| 15 | Uso do `FATOR-K` em fórmulas | **Descartar** | Preservar valor no schema, **não** usar em cálculo (REQ-CAT-03). | BR-020 | Bloqueado em SENARC (OQ-01) |
| 16 | Gestão de Usuários (perfis ADM/OPR/CON/AUD/SUP) | **Evoluir** | Migrar para OAuth2/JWT (REQ-SYS-02), preservar mapeamento de perfis. | REQ-SYS-02 | Alta |

---

## Funcionalidades Novas (não existem no legado)

| # | Funcionalidade Nova | Justificativa | Prioridade | Complexidade |
|---|---|---|---|---|
| N1 | API REST versionada (`/api/v1/*`) com OpenAPI 3.1 publicado | Habilita integração moderna; legado só atendia 3270. Formaliza REQ-SYS-01. | Alta | Média |
| N2 | Autenticação OAuth2 / JWT com perfis mapeados do legado | Substitui controle de acesso amarrado ao terminal mainframe. Formaliza REQ-SYS-02. | Alta | Média |
| N3 | Feature flag de roteamento `sifap.routing.<context>` (legacy / new) | Estratégia Strangler — viabiliza coexistência sem redeploy. ADR-003. | Alta | Alta |
| N4 | Dashboard Next.js para consulta de beneficiários, pagamentos e auditoria | Substituição da experiência de relatório impresso 3270. | Média | Média |
| N5 | Reconciliação automática de retorno SIAFI/CNAB 240 | Hoje é manual; webhook + matching automático reduz incidentes. | Média | Alta |
| N6 | Dry-run / preview do ciclo mensal por competência | Permite ver divergências antes do flip Strangler e antes do envio CNAB. | Alta | Média |
| N7 | Observabilidade (métricas, traces, logs estruturados) | Inexistente no legado; obrigatória para a operação 2.0. | Alta | Média |

---

## Resumo de Escopo

| Decisão | Quantidade | Percentual |
|---|---|---|
| Migrar | 7 | 44% |
| Descartar | 4 | 25% |
| Evoluir | 5 | 31% |
| **Total** | **16** | **100%** |

Funcionalidades novas (greenfield): **7** (N1–N7), não somam ao escopo legado.

## Riscos de Escopo

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Decisão sobre `FATOR-K` (OQ-01) demora além da janela de Estágio 3 | Alta | Médio | REQ-CAT-03 preserva campo sem usar; campo está presente no schema; ativação fica como evolução futura. |
| Filtro `EX` em `RELAUDIT` ser regulatório (OQ-02) | Média | Alto | REQ-AUD-05 exibe tudo; flag de exibição pode ser revertida se auditoria interna confirmar restrição. |
| `Adabas Event Replicator` indisponível para shadow read (OQ-05) | Média | Alto | Fallback CDC custom ou batch noturno (mais demorado mas viável); ADR-003 cita ambos os caminhos. |
| Mudança em fator regional/familiar quebrar contrato downstream (BR-013 ordem CPF) | Baixa | Alto | Testes de contrato + paralelo legado/novo em shadow read por 4 semanas com divergência ≤ 0.01% antes do flip. |
| Greenfield N5 (reconciliação SIAFI) consumir tempo do MVP | Média | Médio | Empurrar para Estágio 4 (Evolução). MVP entrega N1+N2+N3+N4 e os 7 itens "Migrar". |
| Sign-off do PO atrasar Passagem #2 | Média | Alto | Par 2 produziu decisões com base na arqueologia; PO precisa só validar (≤ 30 min). |

## Aprovação

- [ ] Par 1 (Product Owner) aprovou as decisões de escopo — **pendente sign-off**
- [x] Par 2 (Enterprise Architect) validou a viabilidade técnica
- [ ] Par 3 (Technical Lead) confirmou que cabe nas 3 horas do Estágio 3 — **pendente**
- [ ] Time concordou com as prioridades — **pendente Passagem #2**

> **Aprovação obrigatória na Passagem #2** (~16:00). Sem ela, o Estágio 3 não começa.

— Paula


---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="GUIDE.md"><strong>GUIDE do Estágio 2</strong></a><br/>
<sub>Passo a passo do estágio.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="ADR-TEMPLATE.md"><strong>ADR-TEMPLATE</strong></a><br/>
<sub>Template de ADR.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>

