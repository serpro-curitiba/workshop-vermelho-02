<!-- markdownlint-disable MD013 MD025 MD040 -->

# ADR-002: Estratégia de mapeamento Adabas → PostgreSQL

**Data**: 19/05/2026
**Status**: Aceita
**Decisores**: Par 2 — Enterprise Architect + Software Architect + Par 7 (DBA, consultado)

## Contexto

Os 4 DDMs Adabas usam estruturas inexistentes em modelos relacionais puros:

| Estrutura Adabas | Onde aparece | Cardinalidade |
|---|---|---|
| **DE** (Descriptor — campo indexado) | `BENEFICIARIO.AB` (CPF), `BENEFICIARIO.BG` (UF), `PAGAMENTO.AB/AD/AE/DB`, `PROGRAMA-SOCIAL.AA`, `AUDITORIA.AB/BA/CB/CC/EA` | n/a |
| **PE** (Periodic Group — grupo repetido) | `BENEFICIARIO.GRP-DEPENDENTE` (max 10), `PAGAMENTO.GRP-DESCONTO` (max 8), `PROGRAMA-SOCIAL.GRP-FAIXA-CALCULO` (max 5), `GRP-PARAM-REGIONAL` (max 6) | bounded |
| **MU** (Multiple Value Field — array escalar) | `PROGRAMA-SOCIAL.TIPO-DSCT-APLIC` (max 8), `AUDITORIA.CAMPO-ALTERADO-ANT/DEP` (max 20), `VALOR-ANTERIOR/POSTERIOR` (max 20) | bounded |
| **Superdescriptor** (índice composto) | `BENEFICIARIO.S1/S2/S3`, `PAGAMENTO.S1/S2/S3`, `AUDITORIA.S1/S2/S3` | n/a |

Volumes: BENEFICIARIO 4,2 M; PAGAMENTO 180 M; AUDITORIA 25 M (todos sem purga desde 1998 — confirmado em [`PAGAMENTO.ddm` nota final](../../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm) e [`AUDITORIA.ddm` nota final](../../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm)).

## Opções Consideradas

### Opção 1: Normalização total (1 tabela por PE/MU)

- **Descrição**: cada PE/MU vira tabela filha com FK + posição.
- **Vantagens**: 3FN ortodoxa; queries SQL puras.
- **Desvantagens**: 4 JOINs no hot path do cálculo (Pagamento × Descontos × Dependentes × Faixas); explosão de tabelas (16+); migração mais cara; perde a localidade que a PE oferece.

### Opção 2: JSONB para todas as PE/MU + colunas escalares para descriptors

- **Descrição**: campos escalares e descriptors viram colunas indexadas; PE/MU viram colunas `JSONB`; tipo Java é POJO simples mapeado com `@JdbcTypeCode(SqlTypes.JSON)`.
- **Vantagens**: leitura do aggregate em 1 SELECT; permite query parcial (`jsonb_path_query`); GIN index sob demanda; migração ETL mais simples (1 PE = 1 JSON array).
- **Desvantagens**: integridade referencial dentro do JSONB é responsabilidade do aplicativo; consultas analíticas mais lentas (mitigável com views materializadas).

### Opção 3: Híbrido por critério de uso

- **Descrição**: PE/MU usadas frequentemente em queries analíticas → tabela filha; demais → JSONB.
- **Vantagens**: equilíbrio.
- **Desvantagens**: decisão ad-hoc por campo; aumenta complexidade de revisão.

## Decisão

**Decidimos:**

1. **Descriptors Adabas → colunas escalares com índice B-Tree** (PostgreSQL).
2. **Superdescriptors → índices compostos** com a mesma ordem de campos.
3. **PE e MU → coluna `JSONB`** com POJO mapeado via Hibernate 6 (`@JdbcTypeCode(SqlTypes.JSON)`).
4. **Exceção AUDITORIA.GRP-ANTES/DEPOIS**: permanece `JSONB` (já é dado opaco por natureza).
5. **Campo `FATOR-K` (`PROGRAMA-SOCIAL.BG`)** — **mistério resolvido em 27/05/2026** (BR-018, BR-019): persistido como `numeric(7,6)` e calculado no cadastro do programa por `FATOR-K = 1 + (FATOR-REAJUSTE × 0.347215)`. `VLR-BASE` é persistido já como `vlr_base_original × FATOR-K`. A constante mágica `0.347215` deve ser isolada como `private static final BigDecimal FATOR_K_CONSTANTE` em `CatalogoProgramasService` com referência explícita a BR-018.
6. **Migração**: ETL one-shot via Spring Batch lendo dump Adabas → escrita PostgreSQL; idempotente por `(CPF, NUM-PAGAMENTO, NUM-AUDITORIA)`.

## Justificativa

- O *hot path* dominante é a leitura completa do aggregate (Beneficiário + dependentes; Pagamento + descontos), o que JSONB serve em 1 SELECT.
- PostgreSQL 16 indexa JSONB via GIN; performance comprovada em workloads similares.
- 180 M pagamentos com 8 descontos cada gerariam ~1,4 bilhões de linhas filhas — JSONB economiza I/O significativo.
- Descriptors são naturalmente índices; não há razão para fugir do modelo relacional para eles.

## Consequências

### Positivas

- Modelo Java limpo (entidade JPA + `record` para itens de PE).
- Migração ETL direta (1 PE Adabas = 1 array JSON).
- Permite versionar o schema da PE sem `ALTER TABLE` (apenas validação no aplicativo).

### Negativas

- Queries analíticas em descontos exigem `jsonb_array_elements` ou view materializada → mitigar com view `pagamento_desconto_view` (refresh diário).
- Validação de integridade dos itens de PE migra para a aplicação → mitigar com Bean Validation no POJO + teste de schema JSONB.

### Riscos

- `pg_total_relation_size` da tabela `pagamento` pode crescer mais que o esperado por causa de TOAST. **Contingência**: particionamento por `ano_mes_ref` (PostgreSQL declarative partitioning) — já planejado para tabelas > 100 GB.
- **Suspeita de duplo reajuste**: `VLR-BASE` é gravado já ajustado por `FATOR-K` (BR-019), mas `CALCBENF.NSN` aplica novamente `(1 + FATOR-REAJUSTE)` na fórmula central (BR-022). Pode ser bug histórico ou intenção legal. **Contingência**: validar com SENARC; se for bug, abrir ADR-004 corrigindo. Até lá, replicar fielmente em shadow read e medir divergência.

## Referências

- [bounded-contexts.md](../bounded-contexts.md)
- [c4-context.md](../c4-context.md)
- [ADR-001](ADR-001-monolito-modular.md)
- DDMs: [`BENEFICIARIO.ddm`](../../01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm), [`PAGAMENTO.ddm`](../../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm), [`PROGRAMA-SOCIAL.ddm`](../../01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm), [`AUDITORIA.ddm`](../../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm)
- PostgreSQL 16 — [JSONB indexing](https://www.postgresql.org/docs/16/datatype-json.html#JSON-INDEXING)
