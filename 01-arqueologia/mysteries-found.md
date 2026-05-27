<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Mistérios Encontrados — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **mysteries-found**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Registre aqui toda lógica, comportamento ou código que o time não conseguiu explicar.
> "Mistérios" são trechos de código sem documentação, com lógica não-óbvia ou que parecem workarounds.
>
> **Cota mínima para passar pelo portão do Estágio 2:** 5 mistérios documentados.

## O que conta como "mistério"?

- Código que faz algo inesperado sem comentário explicando por quê
- Valores hardcoded sem explicação (números mágicos)
- Lógica condicional que parece um workaround ou gambiarra
- Campos no DDM que não são usados por nenhum programa
- Programas que existem mas não são chamados por ninguém
- Comportamento diferente entre o que a documentação diz e o que o código faz
- Easter eggs deixados pelos desenvolvedores originais

## Níveis de Confiança

| Nível     | Significado                                         |
| --------- | --------------------------------------------------- |
| **ALTA**  | Temos certeza de que há algo estranho aqui          |
| **MÉDIA** | Parece suspeito, mas pode ter explicação            |
| **BAIXA** | Pode ser intencional, mas não conseguimos confirmar |

## Mistérios Catalogados

| ID | Descrição | Onde Encontrado | Impacto Potencial | Confiança |
|---|---|---|---|---|
| ~~MYS-001~~ **RESOLVIDO** | ~~`FATOR-K` cadastrado em PROGRAMA-SOCIAL mas nunca referenciado em nenhuma fórmula.~~ Arqueologia de `CADPROG.NSN` (BR-018/019) demonstrou que **é calculado e usado** no momento do cadastro: `FATOR-K = 1 + (FATOR-REAJUSTE × 0.347215)`; `VLR-BASE *= FATOR-K`. Comentário "NAO DOCUMENTADO" no DDM era enganoso. | `CADPROG.NSN` + `PROGRAMA-SOCIAL.ddm` `BG FATOR-K` | Resolvido — porém abre **MYS-011** (suspeita de duplo reajuste). | ALTA |
| MYS-011 | Suspeita de **duplo reajuste**: `VLR-BASE` é persistido ajustado por `FATOR-K` em `CADPROG.NSN` (BR-019), mas `CALCBENF.NSN` aplica novamente `(1 + FATOR-REAJUSTE)` na fórmula central (BR-022). | `CADPROG.NSN` + `CALCBENF.NSN#L215-L222` | ALTO — pode estar dobrando o reajuste em todos os pagamentos. Validar com SENARC + shadow read. | ALTA |
| MYS-002 | `RELAUDIT.NSN` filtra silenciosamente ações `EX` (exclusão) na exibição. | `01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm` nota final (`CUIDADO - PROGRAMA RELAUDIT.NSN FILTRA ACOES 'EX' NA EXIBICAO`) | MÉDIO — pode ser requisito regulatório camuflado ou bug de longa data. | ALTA |
| MYS-003 | Tabela `parametro_regional` hardcoded em 28 linhas no `CALCBENF.NSN` em vez de ler do DDM. | `01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN#L95-L122` | MÉDIO — toda mudança de fator regional exige recompilação; sintoma de débito técnico. | ALTA |
| MYS-004 | `BATCHPGT.NSN` ordena por CPF crescente com comentário "OTIMIZACAO 1999 / SISTEMAS DOWNSTREAM DEPENDEM DESTA ORDENACAO" — contrato silencioso. | `01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L176-L179` | ALTO — quebrar ordem geraria incidente em sistemas não catalogados. | ALTA |
| MYS-005 | Código de região `99` faz bypass total das validações de elegibilidade em `VALELEG.NSN`. | `01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L94-L98` | BAIXO — provavelmente intencional para casos diplomáticos/missões, mas não documentado. | MÉDIA |
| MYS-006 | Truncamento manual com gambiarra `× 100 / 100` em vez de função de biblioteca. | `01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN#L225-L227, L233-L234, L244-L245` | MÉDIO — sintoma de truncamento (não arredondamento) que precisa ser preservado em `BigDecimal`. | ALTA |
| MYS-007 | Fórmula central do cálculo de benefício aparece duplicada em `CALCBENF.NSN` e `BATCHPGT.NSN`. | `CALCBENF.NSN#L215-L222` e `BATCHPGT.NSN#L268-L272` | ALTO — risco de divergência silenciosa se uma cópia mudar e a outra não. | ALTA |
| MYS-008 | Abono natalino de 15% para tipo `A` em dezembro é somado ao valor já com 13º, sem documentação. | `01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN#L240-L248` | MÉDIO — efeito financeiro real, pode estar fora dos manuais oficiais. | MÉDIA |
| MYS-009 | Campos de biometria (`FA`–`FD` em BENEFICIARIO, adicionados em 2005) e a flag `IND-EXIGE-BIOMETRIA` em PROGRAMA-SOCIAL parecem nunca ter sido ativados em programas reais. | `BENEFICIARIO.ddm` `FA`–`FD` + `PROGRAMA-SOCIAL.ddm` `CI` | BAIXO — schema preservado para não quebrar leitura; uso pode ressurgir. | MÉDIA |
| MYS-010 | Auditoria de ações `CO` (consulta) gera volume enorme — todos os programas online registram. Trade-off LGPD vs. rastreabilidade nunca foi reavaliado. | `AUDITORIA.ddm` `BA COD-ACAO` valor `CO` | MÉDIO — decisão arquitetural a tomar em 2.0 (manter ou desligar). | MÉDIA |

## Detalhamento dos Mistérios

### MYS-001: FATOR-K órfão no PROGRAMA-SOCIAL — **RESOLVIDO**

- **Status**: Resolvido em 27/05/2026 pela arqueologia de `CADPROG.NSN` (outra frente do time).
- **Resolução**: o campo **é** usado, mas no programa de **cadastro** (não nos programas de cálculo). Fórmula confirmada: `FATOR-K = 1 + (FATOR-REAJUSTE × 0.347215)` (BR-018); `VLR-BASE` é gravado já como `vlr_base × FATOR-K` (BR-019). O comentário "NAO DOCUMENTADO" no DDM era enganoso (referia-se à semântica da constante `0.347215`, não ao uso do campo).
- **Impacto na 2.0**: REQ-CAT-03 atualizado para incluir a fórmula; ADR-002 item 5 atualizado; constante `0.347215` deve ser isolada em `CatalogoProgramasService.FATOR_K_CONSTANTE` com referência a BR-018.
- **Novo mistério aberto**: MYS-011 (suspeita de duplo reajuste).

### MYS-011: Suspeita de duplo reajuste em CADPROG + CALCBENF

- **Arquivos**: `CADPROG.NSN` (BR-018, BR-019) + `01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN#L215-L222` (BR-022).
- **O que esperávamos**: reajuste aplicado em **um** ponto único.
- **O que o código faz**: `CADPROG` grava `VLR-BASE` já ajustado por `FATOR-K` (que embute o reajuste). `CALCBENF` lê esse `VLR-BASE` e multiplica novamente por `(1 + FATOR-REAJUSTE)`. Resultado: reajuste pode estar sendo aplicado duas vezes em todos os pagamentos.
- **Hipótese do time**: bug histórico não-detectado, **ou** intenção legal (algum tipo de "reajuste composto" autorizado por norma). A constante `0.347215` sugere derivação atuarial específica.
- **Risco se ignorarmos**: replicar fielmente o legado em 2.0 perpetua possível bug financeiro federal; corrigir sem aprovação pode ser não-conformidade legal.
- **Decisão**: shadow read em paralelo (4 semanas) medindo divergência por programa; consulta formal à SENARC. Se confirmado bug, abrir ADR-004.

### MYS-002: filtro silencioso de ações `EX` em RELAUDIT

- **Arquivo**: `01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm` (nota final).
- **Trecho**:

```text
* NOTA2: CUIDADO - PROGRAMA RELAUDIT.NSN FILTRA ACOES 'EX' NA EXIBICAO
```

- **O que esperávamos**: relatório expor todas as ações (IN-TCU 63/2010 exige rastreabilidade).
- **O que o código faz**: filtro ativo há anos sem ADR ou ticket que justifique.
- **Hipótese do time**: pode ter sido pedido informal de algum coordenador para "limpar" o relatório; ou requisito regulatório que não ficou documentado.
- **Risco se ignorarmos**: exibir `EX` em 2.0 pode ser violação não-conhecida; **não** exibir pode ser violação do TCU. **Decisão temporária**: REQ-AUD-05 exibe tudo; validação externa em OQ-02.

### MYS-004: contrato silencioso de ordenação por CPF (BATCHPGT)

- **Arquivo**: `01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L176-L179`.
- **Trecho**:

```natural
READ BENEFICIARIO-V BY CPF
* OTIMIZACAO 1999 / SISTEMAS DOWNSTREAM DEPENDEM DESTA ORDENACAO
```

- **O que esperávamos**: ordem arbitrária (apenas lookup por chave).
- **O que o código faz**: ordem ascendente explícita, mantida há 27 anos.
- **Hipótese do time**: sistemas downstream (não catalogados) recebem o CNAB 240 e fazem matching posicional ou comparam diffs assumindo ordem estável.
- **Risco se ignorarmos**: mudar ordem pode gerar divergência em conciliadores ou dashboards externos. **Decisão**: REQ-PAY-13 preserva contrato.

> Replique o detalhamento para os demais conforme aparecerem novos mistérios.

## Easter Eggs

> Os 3 easter eggs sugeridos pela facilitação não foram localizados durante a arqueologia consolidada. Itens para inspeção manual:

1. [ ] Inspecionar comentários e blocos `* EOF` em `CALCCORR.NSN` (autor místico recorrente nas datas).
2. [ ] Verificar inicialização de constantes em `BATCHREL.NSN` (possível ASCII art em comentários).
3. [ ] Procurar comentários autorais nas últimas linhas dos DDMs (`PAGAMENTO.ddm` tem assinaturas com data).

## Resumo

- Total de mistérios encontrados: **10**
- Confiança alta: **6** (MYS-001, MYS-002, MYS-003, MYS-004, MYS-006, MYS-007)
- Confiança média: **4** (MYS-005, MYS-008, MYS-009, MYS-010)
- Confiança baixa: **0**
- Easter eggs encontrados: **0 / 3** (pendente inspeção com facilitação)

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="mysteries-checklist.md"><strong>mysteries-checklist.md</strong></a><br/>
<sub>Lista do que procurar.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="discovery-report.md"><strong>discovery-report.md</strong></a><br/>
<sub>Síntese final.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

