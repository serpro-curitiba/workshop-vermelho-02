<!-- markdownlint-disable MD013 MD025 MD040 -->

# ADR-003: Coexistência com legado via Strangler Fig

**Data**: 19/05/2026
**Status**: Aceita
**Decisores**: Par 2 — EA + SA + Par 9 (DevOps, consultado)

## Contexto

O SIFAP legado opera há 29 anos e tem dependências externas críticas que **não podem ser desligadas no dia 1**:

- Integração SIAFI (`NUM-OB-SIAFI`, `NUM-NE-SIAFI`, `COD-UG-EMITENTE` em [`PAGAMENTO.ddm` FA–FE](../../01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm)) — adicionada em 2002; cortar significa parar o repasse federal.
- Conciliação bancária via CNAB 240 com retorno assíncrono já em execução para ciclos em vôo.
- 180 M registros históricos em `PAGAMENTO` precisam continuar consultáveis durante a migração (auditoria TCU pode pedir 10 anos atrás).
- Batch mensal `BATCHPGT.NSN` roda no 1º dia útil — qualquer mudança de cronograma é janela negociada com MDS.

Substituição "big bang" foi explicitamente excluída no escopo do workshop e pela natureza regulatória do sistema.

## Opções Consideradas

### Opção 1: Big Bang em janela de manutenção

- **Descrição**: migrar tudo em fim-de-semana, desligar legado segunda-feira.
- **Vantagens**: simples conceitualmente.
- **Desvantagens**: risco regulatório altíssimo (180M registros + integrações ativas); janela exigida (~72h) incompatível com SLA de pagamento federal; impossível testar em produção antes.

### Opção 2: Refatoração in-place (continuar Natural)

- **Descrição**: manter Natural/Adabas, modernizar apenas a UI.
- **Vantagens**: zero risco de migração.
- **Desvantagens**: não resolve obsolescência tecnológica nem escassez de mão de obra Natural; cliente já decidiu pela modernização. Fora do escopo.

### Opção 3: Strangler Fig (escolhida)

- **Descrição**: SIFAP 2.0 expõe as mesmas operações via REST; um *Strangler Proxy* (front controller HTTP) roteia cada operação para 2.0 ou legado conforme feature flag. Operações migradas: vão para 2.0. Não migradas: passam pelo legado via Natural Web Services (já existente desde 2015 — confirmar com Par 1). Migração contexto por contexto, na ordem: **Catálogo → Beneficiário → Pagamento → Auditoria**.
- **Vantagens**: rollback granular (1 flag); valida 2.0 em produção sem comprometer total; cliente vê valor incremental; preserva integrações SIAFI/CNAB enquanto o legado as opera.
- **Desvantagens**: período de coexistência (estimado 6–12 meses) com 2 sistemas a operar; complexidade de roteamento; risco de divergência de dados se escritas duplicarem.

## Decisão

**Decidimos adotar Strangler Fig** com as seguintes regras:

1. **Roteamento por contexto, não por endpoint**. Um contexto inteiro migra de uma vez. Evita o pior caso (transação cruzando os dois lados).
2. **Ordem de migração**: Catálogo (45 registros, baixo risco) → Beneficiário (4,2 M, médio) → Pagamento (180 M, alto) → Auditoria (25 M, append-only, fácil).
3. **Fonte única da verdade por contexto**: durante a coexistência, **só um dos lados escreve**. O outro lê réplica.
4. **Replicação one-way (legado → 2.0)** nos contextos ainda não migrados, via Debezium + Adabas event replicator (ou ETL diário se replicator não estiver disponível — decisão diferida para Par 9).
5. **SIAFI e CNAB permanecem ligados ao legado** até a migração do contexto `Pagamento`. Quando Pagamento migrar, os adapters [`SiafiOrderAdapter`](../c4-context.md) e `CnabAdapter` assumem o papel.
6. **Feature flag por contexto** em config server (`sifap.routing.beneficiario = legacy|new`).
7. **Critério de "pronto para flip"**: 4 semanas de leitura paralela (shadow read) com divergência ≤ 0,01 % + assinatura do gestor de negócio.

## Justificativa

- Permite implantar 2.0 em produção dentro do prazo do workshop com baixo risco.
- Compatível com ADR-001 (monolito modular): o Strangler Proxy é um componente do API Gateway, não um serviço novo.
- Preserva continuidade regulatória — SIAFI/CNAB nunca ficam offline.
- Cada flip é reversível em segundos (mudar flag).

## Consequências

### Positivas

- Entregas incrementais visíveis (Catálogo em semana N, Beneficiário em N+M…).
- Validação real (shadow read) antes do flip.
- Aprendizado contínuo: ajustes no 2.0 sem pressa.

### Negativas

- 2 sistemas operando em paralelo → custo operacional dobrado durante a janela. Mitigar limitando janela a ≤12 meses por contexto.
- Replicação introduz lag (estimado segundos a minutos) → durante a coexistência, consultas críticas vão à fonte da verdade do contexto, não à réplica.

### Riscos

- **Divergência silenciosa**: bug de replicação que ninguém percebe. **Contingência**: job diário de reconciliação que compara contagens e hashes (`HASH-ARQ-REMESSA` em PAGAMENTO já dá precedente).
- **FATOR-K** (ver [ADR-002](ADR-002-mapeamento-adabas-postgresql.md)): se descobrirmos que o legado o usa em cálculo e nós não, divergência aparecerá no shadow read — **bloqueia flip de Pagamento** até resolvido.
- **RELAUDIT filtrando ações `EX`** (ver [`AUDITORIA.ddm` nota final](../../01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm)): 2.0 mostra tudo. Validar com cliente se isso é correção esperada ou paridade obrigatória — se paridade, abrir ADR-004.

## Referências

- [bounded-contexts.md](../bounded-contexts.md), [c4-context.md](../c4-context.md)
- [ADR-001](ADR-001-monolito-modular.md), [ADR-002](ADR-002-mapeamento-adabas-postgresql.md)
- Martin Fowler — [Strangler Fig Application](https://martinfowler.com/bliki/StranglerFigApplication.html)
- Decisão diferida para Par 9: implementação concreta da replicação Adabas → PostgreSQL.
