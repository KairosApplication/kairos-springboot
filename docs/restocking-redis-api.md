# Histórico semanal com a API Redis do kairos-data

A API principal consome o serviço FastAPI de
[KairosApplication/kairos-data](https://github.com/KairosApplication/kairos-data/tree/10d6b08745b3cd885223670ec19d2befd3f87416/redis),
fixado no commit `10d6b08745b3cd885223670ec19d2befd3f87416` no Docker Compose.
O código Python é obtido diretamente desse repositório no build, sem uma cópia
reescrita do serviço. A autenticação JWT/refresh existente é preservada.

## Fluxo

1. O aplicativo autenticado registra uma reposição em
   `POST /api/v1/restockings/registration`.
2. A API valida o cargo, a empresa e os vínculos de funcionário, estoque,
   produto e prateleira. O repositor pode registrar somente para si; o gerente
   pode selecionar funcionários da própria empresa. Avarias não podem exceder
   a quantidade reposta.
3. Na mesma transação PostgreSQL, grava a reposição e um evento de entrega. O ID
   vem do registro persistido pelo JPA; não há `MAX(id)` nem publicação antes do commit.
   O cadastro também desconta `restockedQuantity` de `product_inventories` e soma
   `restockedQuantity - damagedQuantity` em `product_shelves`, criando o vínculo
   de prateleira quando necessário. Avarias saem do estoque e não entram na
   prateleira. Sem saldo suficiente ou sem vínculo no estoque, retorna 400 sem
   gravar reposição, movimentação ou evento. Os saldos são bloqueados durante
   a transação para serializar reposições concorrentes. Essa operação é feita
   pelo serviço Java, sem depender de uma procedure externa.
   PATCH e DELETE continuam alterando apenas o registro histórico, sem estornar
   ou refazer a movimentação física efetuada no cadastro.
4. Depois do commit, envia `POST /v1/restockings/events` ao serviço interno com
   `X-Internal-API-Key`. O payload usa exatamente os nomes snake_case, IDs e
   timestamp com fuso exigidos pelo kairos-data.
5. O serviço usa Redis LIST, Lua para idempotência e expiração na próxima
   segunda-feira às 00:00 de São Paulo. Um reenvio reconhecido como duplicado
   também confirma a entrega. A expiração não é prorrogada pelo backend.
6. O aplicativo consulta `GET /api/v1/restockings/week`; somente o backend chama
   `GET /v1/restockings/week` do serviço Redis. A chave interna nunca é enviada
   ao navegador/mobile nem incluída nas respostas públicas.

## Consulta pública autenticada

```http
GET /api/v1/restockings/week?limit=100
Authorization: Bearer <access-token>
```

Também aceita os cookies de autenticação existentes. Um gerente pode acrescentar
`employeeId`; repositor acessa somente a própria identificação. `companyId` é
resolvido no PostgreSQL a partir do funcionário autenticado, sem confiar em
um valor enviado pelo aplicativo. Caixa, cliente, funcionário sem empresa e
funcionário de outra empresa recebem 403.

A resposta contém `companyId`, `employeeId`, `weekStart`, `weekEnd`, `total` e
`items`. Cada item segue o contrato snake_case do kairos-data: `restocking_id`,
`company_id`, `employee_id`, `product_id`, `shelf_id`, `inventory_id`,
`restocked_quantity`, `qnt_avarias`, `restocking_date` e `product_name`.
`total` representa a quantidade oficial no PostgreSQL; `limit` aceita 1 a 500,
com padrão 100. A ordenação é por data e ID, dos mais recentes aos mais antigos,
mesmo quando os eventos chegaram fora de ordem ao cache.

A semana é segunda inclusiva até a próxima segunda exclusiva em
`America/Sao_Paulo`, independentemente do fuso do servidor. O campo PostgreSQL
`restocking_date`, sem timezone no schema existente, é interpretado nesse fuso.
Semanas anteriores continuam disponíveis pelas consultas existentes ao PostgreSQL.

## Consistência e recuperação

`restocking_history_delivery` é uma fila transacional durável. Se a chamada HTTP
falhar após o commit, a reposição continua registrada e o cadastro mantém sua
resposta de sucesso. A cada cinco segundos, o worker busca até 25 entregas
pendentes; falhas aguardam 30 segundos antes de nova tentativa. O lock de linha
e a idempotência do serviço suportam múltiplas instâncias. Reiniciar a API não
descarta pendências. Metadados de semanas encerradas são limpos periodicamente.

Redis vazio não significa histórico vazio. A API confere a quantidade da semana
no PostgreSQL e só usa um cache completo, da empresa/funcionário/semana corretos,
sem entregas pendentes. Em falha HTTP, resposta inválida, cache incompleto ou
semana com mais de 500 registros, retorna o resultado oficial do PostgreSQL.
Uma leitura de cache ausente/incompleto agenda sua reconstrução para até 500
registros, sem bloquear a resposta em centenas de chamadas HTTP.

O contrato atual do kairos-data só permite inclusões idempotentes, sem substituir
ou remover eventos. Por isso, PATCH e DELETE das reposições registram uma marca
em `restocking_history_bypass` na mesma transação. O histórico daquele funcionário
naquela semana passa a usar PostgreSQL até a virada de semana; assim, nunca
retorna a versão anterior nem um registro excluído. Novas reposições continuam
sendo notificadas ao serviço Redis. Essa escolha preserva as rotas existentes
sem inventar endpoints que o serviço externo não possui.

Os vínculos `company_id` já existentes em Employee, Inventory, Product e Sector
agora são mapeados. Registros usados neste fluxo devem possuir empresas válidas;
o backend não atribui empresa arbitrária a registros sem vínculo. A migração V3
adiciona as duas tabelas de integração e um índice para a consulta semanal.

## Configuração fora do Compose de desenvolvimento

```env
KAIROS_REDIS_API_ENABLED=true
KAIROS_REDIS_API_URL=http://localhost:8001
KAIROS_INTERNAL_API_KEY=<chave-interna-aleatoria-de-pelo-menos-32-caracteres>
KAIROS_REDIS_CONNECT_TIMEOUT=2s
KAIROS_REDIS_READ_TIMEOUT=2s
KAIROS_REDIS_RETRY_DELAY=5000
```

Configure a mesma `KAIROS_INTERNAL_API_KEY` no processo FastAPI. Use rede privada
ou HTTPS fora do ambiente local. `REDIS_URL` do FastAPI aponta para o cache de
histórico; `REDIS_URL` da API principal continua sendo a conexão de sessões JWT
e do cache de categorias. Com a integração desabilitada, a consulta semanal
continua disponível pelo PostgreSQL, sem chamadas ao serviço externo.

O Compose padrão, no profile `app`, inclui `redis-history-api` e `redis-history`;
a chave interna deve estar no `.env`. O Compose de desenvolvimento gera essa
chave em um volume próprio e a compartilha somente com os dois backends. O
microserviço não tem acesso ao volume da chave de assinatura JWT.

O cache de histórico tem Redis próprio: parar esse cache/microserviço permite
fallback no PostgreSQL e não interfere no Redis de autenticação. A falha do
Redis de autenticação continua retornando 503 para recursos protegidos, conforme
o contrato de revogação imediata existente.

## Desenvolvimento e validação

```sh
docker compose -f compose.dev.yaml up -d --build api seed
docker compose -f compose.dev.yaml --profile check run --rm smoke
docker compose -f compose.dev.yaml --profile check run --rm smoke python /checks/history_smoke.py
```

A carga local inclui `stocker@kairos.local`, além de gerente/cliente, produto,
estoque e prateleira fictícios. A senha pública local é `Kairos-Dev-Only!123`.
O smoke cria, consulta, edita e exclui uma reposição pela API principal.

Os testes Java cobrem o contrato HTTP real do kairos-data, publicação depois do
commit, ausência de evento em rollback, reenvio, virada de semana, cache frio,
isolamento por empresa/cargo, edição e exclusão. O smoke do Compose usa PostgreSQL,
o FastAPI do repositório referenciado e Redis reais.
