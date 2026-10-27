# Consulta do mapa por empresa

`GET /api/v1/store-map` usa a autenticação JWT existente, inclusive cookies. Exige role `MANAGER` e funcionário associado ao e-mail autenticado, com cargo autorizado e empresa cadastrada. A empresa é resolvida no servidor; o navegador não escolhe `companyId`. A resposta usa `Cache-Control: no-store`.

## Resposta

Objeto com cinco listas, usando os DTOs existentes:

- `sectors`: `SectorResponse`.
- `aisles`: `AisleResponse`.
- `shelves`: `ShelfResponse`.
- `products`: `ProductResponse`, incluindo categoria.
- `productShelves`: `ProductShelfResponse`, incluindo quantidades zero.

Setores e produtos são filtrados por empresa; corredores e gôndolas seguem a empresa do setor. Vínculos são retornados somente quando gôndola e produto pertencem à empresa atual. Vínculos inconsistentes entre empresas não expõem produtos de terceiros. Uma empresa sem cadastros recebe listas vazias. Conta sem funcionário/empresa recebe 403; sem autenticação, 401.

## Desempenho e compatibilidade

O serviço faz uma consulta para resolver o funcionário e cinco consultas de dados, com relações necessárias carregadas junto. Evita N+1 e o download do catálogo global. Os endpoints legados de listagem continuam disponíveis e com seus escopos anteriores; este endpoint não altera a autorização de toda a API nem corrige todos os fluxos de escrita.

Também foram otimizados `findAll` de corredores, gôndolas e produtos com `EntityGraph`, preservando os DTOs e as rotas existentes. Cadastro, edição e exclusão de gôndolas mantêm o contrato anterior.

O frontend deve usar esta consulta para mapa e listagem de gôndolas, em vez de combinar cinco listagens globais. Não é necessário aumentar o timeout para compensar o catálogo de outras empresas.

## Verificação

`StoreMapControllerIntegrationTest` verifica isolamento entre empresas, ausência de vínculos cruzados, saldo zero, empresa vazia, 401/403 e seis consultas SQL após limpar o contexto JPA. `MapListQueryPerformanceIntegrationTest` verifica uma única consulta SQL por listagem legada afetada. Os testes usam H2 em memória, sem alterações no banco real.

A reprodução real encontrou 50 mil gôndolas e 80 mil produtos nas listagens globais. Na conta de teste, a consulta por empresa retornou 500 gôndolas e 800 produtos em aproximadamente três segundos e permitiu abrir o mapa. Esses tempos descrevem uma medição local, não um SLA.
