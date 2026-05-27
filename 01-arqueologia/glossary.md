<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Glossário do SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **glossary**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Preencha esta tabela com todos os termos, abreviações e siglas encontrados no código Natural/Adabas.
> **Meta: no mínimo 30 termos.**

## Por que isso importa

Sistemas legados têm vocabulário próprio que ninguém documenta em lugar nenhum — só está no nome das variáveis. Se o time do Estágio 2 não souber o que `DSCT`, `BENF`, `PE` ou `CTC` significam, vai escrever uma spec sobre o que ele _acha_ que isso significa. Glossário é o que evita esse desencontro.

## Como preencher

- **Termo**: a abreviação ou sigla exatamente como aparece no código
- **Expansão**: o significado completo do termo
- **Programa**: em qual arquivo `.NSN` ou `.ddm` o termo foi encontrado
- **Contexto**: breve explicação de como/onde o termo é usado

## Dica de extração

Prompt útil no Copilot Chat (cole o conteúdo de 2–3 arquivos `.NSN` no chat antes):

> _"Liste todas as abreviações e siglas usadas neste código Natural. Para cada uma, sugira a expansão e marque com 'CONFIRMADO' ou 'HIPÓTESE'."_

## Termos encontrados

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 1   | `SIFAP` | Sistema de Identificação e Folha de Apoio (legado) | todos | Nome do sistema legado a ser modernizado. |
| 2   | `BENF` / `BENEF` | Beneficiário | `BENEFICIARIO.ddm`, `CADBENEF.NSN`, `CONSBENF.NSN` | Pessoa que recebe pagamento de programa social. |
| 3   | `DEPEND` / `DEP` | Dependente | `CADDEPEND.NSN`, `BENEFICIARIO.ddm` grupo `DA` | Pessoa ligada ao beneficiário (até 10), com grau de parentesco. |
| 4   | `PGT` / `PGTO` | Pagamento | `BATCHPGT.NSN`, `RELPGT.NSN`, `PAGAMENTO.ddm` | Evento mensal de transferência ao beneficiário. |
| 5   | `DSCT` | Desconto | `CALCDSCT.NSN`, `PAGAMENTO.ddm` grupo `CA` | Dedução aplicada sobre valor bruto. Tipos: `IR`/`JD`/`CS`/`PA`/`EM`/`TX`/`OU`/`EX`. |
| 6   | `PROG` | Programa Social | `CADPROG.NSN`, `PROGRAMA-SOCIAL.ddm` | Programa de governo (assistencial / trabalho / previdenciário). |
| 7   | `ELEG` | Elegibilidade | `VALELEG.NSN` | Validação se beneficiário cabe no programa. |
| 8   | `AUDIT` | Auditoria | `RELAUDIT.NSN`, `AUDITORIA.ddm` | Log imutável de todas as operações (IN-TCU 63/2010). |
| 9   | `CORR` | Correção monetária | `CALCCORR.NSN` | Reajuste anual aplicado sobre valor base. |
| 10  | `BATCH` | Processamento em lote noturno | `BATCHPGT.NSN`, `BATCHCON.NSN`, `BATCHREL.NSN` | Job que roda fora do horário online. |
| 11  | `REL` | Relatório | `RELPGT.NSN`, `RELAUDIT.NSN`, `BATCHREL.NSN` | Saída tabular para gestores/órgãos de controle. |
| 12  | `CON` / `CONS` | Consulta (read-only) | `CONSBENF.NSN`, `BATCHCON.NSN` | Operação de leitura sem efeito colateral. |
| 13  | `VAL` | Validação | `VALBENEF.NSN`, `VALDOCS.NSN`, `VALELEG.NSN` | Sub-rotina de checagem (CPF, docs, elegibilidade). |
| 14  | `CAD` | Cadastro | `CADBENEF.NSN`, `CADDEPEND.NSN`, `CADPROG.NSN` | Tela/rotina de inclusão. |
| 15  | `UG` | Unidade Gestora (SIAFI) | `PAGAMENTO.ddm` `FC COD-UG-EMITENTE` | Identificador SIAFI da unidade que emite a OB. |
| 16  | `OB` | Ordem Bancária (SIAFI) | `PAGAMENTO.ddm` `FA NUM-OB-SIAFI` | Documento SIAFI de pagamento ao banco. |
| 17  | `NE` | Nota de Empenho (SIAFI) | `PAGAMENTO.ddm` `FB NUM-NE-SIAFI` | Reserva orçamentária prévia ao pagamento. |
| 18  | `SIAFI` | Sistema Integrado de Administração Financeira do Governo Federal | `PAGAMENTO.ddm` campos `FA–FE` | Sistema externo upstream para tesouro nacional. |
| 19  | `CNAB 240` | Padrão FEBRABAN de remessa bancária (240 colunas) | `PAGAMENTO.ddm` `HA HASH-ARQ-REMESSA` | Arquivo de remessa enviado ao banco pagador. |
| 20  | `COMPETENCIA` | Mês de referência do pagamento (`AAAAMM`) | `PAGAMENTO.ddm` `BB COMPETENCIA` | Distinto de data de processamento; usado em idempotência. |
| 21  | `PE` | Periodic Group (Adabas) | DDMs com `PE` | Lista ordenada de tamanho fixo (ex.: 10 dependentes, 5 faixas). |
| 22  | `MU` | Multiple-Value Field (Adabas) | `AUDITORIA.ddm` grupos `DA`/`DD` | Campo que aceita múltiplas instâncias (até 20). |
| 23  | `DDM` | Data Definition Module (Adabas) | todos os `.ddm` | Schema lógico de tabela Adabas. |
| 24  | `S1`, `S2` ... | Descriptor / Superdescriptor (Adabas) | `BENEFICIARIO.ddm` `AB NUM-CPF` | Índice Adabas. Superdescriptor = composto. |
| 25  | `CPF` | Cadastro de Pessoas Físicas | `BENEFICIARIO.ddm` `AB`, `PAGAMENTO.ddm` `AB` | 11 dígitos sem formatação. |
| 26  | `IBGE` | Código de município IBGE (7 dígitos) | `BENEFICIARIO.ddm` `BD COD-MUN-IBGE` | — |
| 27  | `FATOR-K` | Fator multiplicador não documentado | `PROGRAMA-SOCIAL.ddm` `BG FATOR-K` | **MISTÉRIO** — preservado, não usado. Bloqueado em SENARC (OQ-01). |
| 28  | `FATOR_REG` | Fator regional aplicado por UF | `CALCBENF.NSN` linhas 95–122, 178–183 | Hardcoded no legado; migra para parâmetro. |
| 29  | `FATOR_FAM` | Fator familiar por nº de dependentes | `CALCBENF.NSN` linhas 185–199 | Vide BR-004. |
| 30  | `FATOR_RND` | Fator de renda familiar | `CALCBENF.NSN` linhas ~170 | Aplicado a programas assistenciais. |
| 31  | `FATOR_IDADE` | Fator etário (≥65, 60–64, <18, 18–59) | `CALCBENF.NSN` linhas 203–214 | Vide BR-005. |
| 32  | `FATOR_REAJUSTE` | Percentual anual de reajuste por programa | `PROGRAMA-SOCIAL.ddm` `BE PCT-REAJUSTE-ANUAL` | Aplicado como `(1 + reajuste)` na fórmula. |
| 33  | `13º` | Décimo terceiro (gratificação natalina) | `CALCBENF.NSN` linhas 230–237 | Disparado quando `MM=12`. |
| 34  | `ABONO NATALINO` | Adicional de 15% para programas tipo `A` em dezembro | `CALCBENF.NSN` linhas 240–248 | Empilha com 13º. |
| 35  | `STATUS` (beneficiário) | A / S / C / I / D | `BENEFICIARIO.STATUS` | A=Ativo · S=Suspenso · C=Cancelado · I=Inativo · D=Desligado. Só `A` paga. |
| 36  | `SIT-PAGAMENTO` | P / G / E / C / D / X / R | `PAGAMENTO.ddm` `DA` | P=Pendente · G=Gerado · E=Enviado · C=Confirmado · D=Devolvido · X=Cancelado · R=Rejeitado. |
| 37  | `TIPO-PROGRAMA` | A / T / P | `PROGRAMA-SOCIAL.ddm` `AD` | A=Assistencial · T=Trabalho · P=Previdenciário. Governa regras de elegibilidade. |
| 38  | `COD-ACAO` (auditoria) | IN/AL/EX/CO/LG/LO/BT/ER/AU/RE | `AUDITORIA.ddm` `BA` | Inclusão/Alteração/Exclusão/Consulta/LoGin/LogOut/Batch/ERro/AUtorização/REjeição. |
| 39  | `PERFIL` | ADM / OPR / CON / AUD / SUP | `AUDITORIA.ddm` `EC` | Administrador · Operador · Consulta · Auditor · Suporte. |
| 40  | `GRP-` (prefixo) | Convenção de nomenclatura para grupos Adabas | todos os DDMs | Ex.: `GRP-DEPENDENTE`, `GRP-ENDERECO`, `GRP-DESCONTO`, `GRP-FAIXA-CALCULO`. |

> Total: **40 termos** confirmados. Adicione mais conforme novas descobertas.

## Exemplo de linha bem preenchida

| #   | Termo  | Expansão | Programa                        | Contexto                                                                                                         |
| --- | ------ | -------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 1   | `DSCT` | Desconto | `CALCDSCT.NSN`, `PAGAMENTO.ddm` | Tipo de dedução aplicada sobre valor bruto do pagamento. Tipos: 'J' (judicial), 'I' (imposto), 'T' (trabalhista) |

## Observações

- **Padrões de nomenclatura identificados**:
  - Prefixo `CAD` = cadastro (CRUD); `CON`/`CONS` = consulta; `CALC` = cálculo; `VAL` = validação; `REL` = relatório; `BATCH` = batch noturno.
  - Prefixo `GRP-` em DDMs = grupos compostos (Adabas Periodic Group ou Multiple-Value).
  - Sufixo `-V` em programas = view (cópia local da estrutura DDM em working storage).
  - Códigos de 2 letras para tipos enumerados (`IR`, `JD`, `CS`, `IN`, `AL`, `EX`, `ADM`, `OPR` …).
- **Convenções de prefixo/sufixo encontradas**:
  - Variáveis locais Natural começam com `#` (ex.: `#VLR-BENF`, `#FATOR-REG`).
  - Campos Adabas seguem `LETRA-LETRA` (`AA`, `AB`, `BA`, `CA` …) para sequência de armazenamento.
  - IDs de tabelas externas usam siglas oficiais sem hífen: `CPF`, `IBGE`, `UG`, `OB`, `NE`.
- **Termos ambíguos que precisam de validação com especialista**:
  - `FATOR-K` — semântica nunca documentada (BR-020 / OQ-01). **Bloqueio em SENARC.**
  - `FATOR_RND` (fator de renda) — fórmula não está explícita no código, parece subentendida.
  - Filtro de ações `EX` em `RELAUDIT.NSN` — não se sabe se é **bug** ou **requisito regulatório** (OQ-02). **Bloqueio com auditoria interna.**

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
<a href="business-rules-catalog.md"><strong>business-rules-catalog.md</strong></a><br/>
<sub>Catálogo de regras.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

