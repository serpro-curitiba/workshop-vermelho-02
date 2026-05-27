<!-- markdownlint-disable MD013 MD025 MD040 -->

# ADR-001: Adotar Monolito Modular (não microsserviços)

**Data**: 19/05/2026
**Status**: Aceita
**Decisores**: Par 2 — Enterprise Architect + Software Architect

## Contexto

O SIFAP 2.0 substitui um sistema mainframe Natural/Adabas de 29 anos que processa ~3,8 M pagamentos/mês sobre uma base de ~4,2 M beneficiários ([`PAGAMENTO.ddm`](../../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm), [`BENEFICIARIO.ddm`](../../01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm)).

Restrições:

- Time pequeno (5 pessoas por par no workshop; equipes reais com poucas dezenas).
- Janela de implantação restrita (regulada por ITSM-SIFAP-002).
- 4 contextos com transações que cruzam fronteiras (Pagamento lê Beneficiário + Catálogo no mesmo cálculo — ver [`CALCBENF.NSN` linhas 130–180](../../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN)).
- Necessidade de migração progressiva (Strangler Fig) sem distribuir transação entre legado e 2.0.

## Opções Consideradas

### Opção 1: Microsserviços (1 serviço por bounded context)

- **Descrição**: 4 deployáveis independentes (`beneficiario-svc`, `catalogo-svc`, `pagamento-svc`, `auditoria-svc`) + API Gateway, comunicação REST/eventos.
- **Vantagens**: escala independente; deploy isolado; equipes autônomas.
- **Desvantagens**: transações distribuídas no cálculo (sagas para algo que hoje é uma READ + WRITE local); 4× infra (CI/CD, observabilidade, segredos); latência inter-serviço no hot path do `BATCHPGT`; complexidade incompatível com tamanho do time e janela do workshop.

### Opção 2: Monolito tradicional (sem fronteiras)

- **Descrição**: um JAR Spring Boot com pacotes livres.
- **Vantagens**: simples.
- **Desvantagens**: degenera rápido para "big ball of mud" — exatamente o que o legado virou em 29 anos. Não permite extração futura.

### Opção 3: Monolito Modular (escolhida)

- **Descrição**: um único JAR Spring Boot, mas com **módulos Maven separados** por bounded context, comunicação cross-module via interfaces publicadas no `shared-kernel`, schema PostgreSQL por módulo.
- **Vantagens**: simplicidade operacional do monolito + fronteiras físicas (build falha em violação); extração para microsserviço é refactor mecânico amanhã.
- **Desvantagens**: precisa disciplina de revisão para não importar classes internas de outro módulo (mitigado por Maven + ArchUnit opcional).

## Decisão

**Decidimos adotar Monolito Modular** com 4 módulos Maven (`sifap-beneficiario`, `sifap-catalogo`, `sifap-pagamento`, `sifap-auditoria`) + `sifap-shared-kernel` + `sifap-app` (composição). Empacotamento: **um único** Spring Boot JAR.

## Justificativa

- O cálculo do pagamento ([`CALCBENF.NSN`](../../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN), [`CALCDSCT.NSN`](../../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN), [`BATCHPGT.NSN`](../../01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN)) lê Beneficiário e Programa e escreve Pagamento numa única unidade de trabalho. Distribuir isso seria criar uma saga para um problema que não existe.
- O volume justifica robustez (180 M pagamentos históricos), não fragmentação — PostgreSQL 16 com índices adequados absorve a carga; o gargalo histórico foi reorganização Adabas trimestral, não throughput.
- Strangler Fig (ver [ADR-003](ADR-003-strangler-coexistencia-siafi.md)) é mais simples quando o "novo" é um único deploy.
- Aderente aos antipadrões explicitados pelo `@architect-agent` (`/.github/agents/architect.agent.md`): "resistir à tentação de microsserviços".

## Consequências

### Positivas

- 1 pipeline CI/CD; 1 conjunto de credenciais; 1 dashboard de observabilidade.
- Refatorações cross-module sem versionar API.
- Onboarding rápido para equipes do workshop.
- Possível extrair contexto para serviço próprio sob demanda futura (caminho preservado).

### Negativas

- Deploy global a cada release → mitigar com feature flags por módulo e versionamento de schema (Flyway por schema).
- Tentação de criar atalhos cross-module → mitigar com revisão de PR + opcional ArchUnit rule rejeitando imports `internal.*`.

### Riscos

- Crescimento descontrolado de um módulo (provavelmente `pagamento`). **Contingência**: revisão arquitetural por trimestre; quando ultrapassar ~30 % do código total, avaliar split.

## Referências

- [bounded-contexts.md](../bounded-contexts.md)
- [c4-context.md](../c4-context.md)
- [ADR-002](ADR-002-mapeamento-adabas-postgresql.md), [ADR-003](ADR-003-strangler-coexistencia-siafi.md)
- Antipadrão "Arquitetura pronta sem grounding" — [`.github/agents/architect.agent.md`](../../.github/agents/architect.agent.md)
