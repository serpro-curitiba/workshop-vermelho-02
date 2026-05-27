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

| ID | Regra de Negócio | Programa Fonte | Campos DDM | Nível de Risco | Notas |
|---|---|---|---|---|---|
| BR-001 | O cadastro de beneficiário só aceita operações I (inclusão) ou A (alteração). | CADBENEF.NSN | BENEFICIARIO.OPERACAO | ALTO | Regra de entrada do fluxo. |
| BR-002 | CPF do beneficiário é obrigatório para cadastrar/alterar. | CADBENEF.NSN | BENEFICIARIO.CPF | ALTO | Chave funcional primária no processo. |
| BR-003 | CPF do beneficiário deve ser válido pelo algoritmo módulo 11. | CADBENEF.NSN | BENEFICIARIO.CPF | CRÍTICO | Falha pode gerar cadastro inválido e pagamento indevido. |
| BR-004 | Nome do beneficiário é obrigatório. | CADBENEF.NSN | BENEFICIARIO.NOME | MÉDIO | Regra de qualidade de cadastro. |
| BR-005 | Sexo do beneficiário só aceita M ou F. | CADBENEF.NSN | BENEFICIARIO.SEXO | MÉDIO | Domínio fechado legado. |
| BR-006 | Inclusão de beneficiário é bloqueada quando CPF já existe. | CADBENEF.NSN | BENEFICIARIO.CPF | CRÍTICO | Evita duplicidade de benefício. |
| BR-007 | Alteração de beneficiário só ocorre se o CPF já existir. | CADBENEF.NSN | BENEFICIARIO.CPF | ALTO | Evita atualização fantasma. |
| BR-008 | Na inclusão, beneficiário inicia com status A. | CADBENEF.NSN | BENEFICIARIO.STATUS | ALTO | Regra de estado inicial. |
| BR-009 | Beneficiário com idade superior a 75 anos recebe status S. | CADBENEF.NSN | BENEFICIARIO.DT-NASCIMENTO, BENEFICIARIO.STATUS | ALTO | Regra especial etária prevalece sobre status inicial. |
| BR-010 | Não permite incluir dependente para titular com status C ou D. | CADDEPEND.NSN | BENEFICIARIO.STATUS, BENEFICIARIO.CPF | ALTO | Regra de elegibilidade do titular. |
| BR-011 | Há limite máximo de dependentes por titular. | CADDEPEND.NSN | BENEFICIARIO.NUM-DEPENDENTES, BENEFICIARIO.DEPENDENTES.* | ALTO | Condição atual indica possível off-by-one. |
| BR-012 | Parentesco de dependente só aceita FI, CO, IR ou OU. | CADDEPEND.NSN | BENEFICIARIO.DEPENDENTES.PARENTESCO | MÉDIO | Domínio fechado de parentesco. |
| BR-013 | Não permite CPF de dependente duplicado para o mesmo titular. | CADDEPEND.NSN | BENEFICIARIO.CPF, BENEFICIARIO.DEPENDENTES.CPF-DEP | ALTO | Deduplicação é local por titular. |
| BR-014 | Inclusão de dependente incrementa NUM-DEPENDENTES do titular. | CADDEPEND.NSN | BENEFICIARIO.NUM-DEPENDENTES, BENEFICIARIO.DEPENDENTES.* | ALTO | Impacta regras de elegibilidade e cálculo. |
| BR-015 | Cadastro de programa social só aceita operações I (inclusão) ou C (consulta). | CADPROG.NSN | PROGRAMA-SOCIAL.OPERACAO | MÉDIO | Regra de fluxo operacional. |
| BR-016 | Operação C executa consulta somente leitura por código do programa. | CADPROG.NSN | PROGRAMA-SOCIAL.COD-PROGRAMA | BAIXO | Não altera dados. |
| BR-017 | Inclusão de programa é bloqueada quando código já existe. | CADPROG.NSN | PROGRAMA-SOCIAL.COD-PROGRAMA | ALTO | Garante unicidade de programa. |
| BR-018 | Fator K é calculado por 1 + (fator reajuste * 0.347215). | CADPROG.NSN | PROGRAMA-SOCIAL.FATOR-REAJUSTE | CRÍTICO | Constante mágica sensível de cálculo financeiro. |
| BR-019 | Valor armazenado do programa é VLR-BASE multiplicado por FATOR-K. | CADPROG.NSN | PROGRAMA-SOCIAL.VLR-BASE, PROGRAMA-SOCIAL.FATOR-REAJUSTE | CRÍTICO | Campo VLR-BASE passa a carregar valor ajustado. |
| BR-020 | Programa novo é criado com status ativo A. | CADPROG.NSN | PROGRAMA-SOCIAL.STATUS-PROG | ALTO | Estado inicial afeta uso imediato em elegibilidade. |


> Adicione mais linhas conforme necessário. Lembre-se: existem **10 regras escondidas** no código!

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

- Total de regras encontradas: 20
- Regras críticas: 4
- Regras com duplicação: 3
- Regras sem documentação (escondidas): \_\_\_

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

