# Segurança, Redis e PostgreSQL

## Autenticação e Redis

A API usa JWT Bearer e refresh tokens em cookie HttpOnly; veja o
[contrato e exemplos](authentication.md). Redis guarda apenas SHA-256 dos refresh
tokens e metadados, nunca o refresh em texto puro. PostgreSQL continua responsável
por usuários, senhas e permissões. A validação do access JWT não consulta os bancos.
Consultas exigidas pelo recurso de negócio e cache de categorias continuam normais.

Cada login cria uma chave `kairos:auth:{sessionId}`. O hash contém `uid`, `expires`
(em milissegundos), `revoked` e campos `t:<sha256>` com estado `active` ou `used`.
Os scripts Lua usam o relógio do Redis. O TTL inicial é 7 dias e a rotação nunca
altera a expiração absoluta. Todos os hashes consumidos ficam na mesma chave até
a expiração. Isso mantém a operação em um único hash slot no Redis Cluster.

A inspeção valida a prova do token antes de carregar permissões do PostgreSQL;
a rotação revalida e troca o estado atomicamente. Logout e rotação compartilham
a mesma chave, de modo que uma revogação impede toda renovação posterior.
Se uma rotação vencer a corrida com logout, o JWT emitido pode continuar válido
até expirar, mas o refresh resultante não pode renovar após a revogação.

Um locator conhecido com hash desconhecido não autoriza revogação. Um hash
consumido reapresentado revoga a família inteira. O cliente deve serializar
refresh: duas chamadas com o mesmo token podem produzir uma resposta 200 e
outra 401, deixando também o novo refresh revogado. Outras sessões não são afetadas.

O Compose mantém AOF e volume `redis-data` e configura `noeviction`. Monitore memória:
quando um limite é atingido, gravações podem falhar em vez de remover sessões.
O AOF padrão usa fsync periódico e pode perder gravações recentes em falhas abruptas.
Perda do volume exige novo login; perda parcial de registros consumidos compromete
a detecção de reuso. Considere instância separada do cache, backups e durabilidade
adequada ao ambiente. Não use comandos de limpeza global para testes ou operação.

Credenciais ficam em `REDIS_URL` (`rediss://` quando houver TLS); configure
`REDIS_CONNECT_TIMEOUT` e `REDIS_COMMAND_TIMEOUT` (padrão 2s). Login e refresh
falham com 503 em indisponibilidade e não emitem tokens sem sessão controlada.
Não há fallback em memória. Logout retorna 503 quando não consegue confirmar a
revogação; o cliente deve tentar novamente. Access JWTs válidos continuam funcionando
até expirar, exceto recursos cuja própria regra de negócio dependa do Redis.

As antigas sessões JSESSIONID deixam de autenticar: usuários precisam fazer novo
login. Não alteramos nem apagamos chaves antigas; elas expiram conforme o TTL anterior.

Testes com Redis real (Docker em execução, contêiner descartável):

```sh
./mvnw -B -ntp -Dintegration.redis=true -Dintegration.redis.container=true clean verify
```

Com Redis isolado já disponível, como na CI:

```sh
./mvnw -B -ntp -Dintegration.redis=true -Dtest.redis.port=6379 clean verify
```

A suíte remove apenas as chaves que ela própria criou; não use um Redis de produção.
A suíte padrão usa mocks somente para os contratos web. Atomicidade, TTL, reuso,
isolamento e corridas são verificados nos testes que exigem Redis real. Eles ficam
explicitamente desabilitados quando `integration.redis` não está definido.

## PostgreSQL

Flyway executa `V1__initial_schema.sql` em um banco vazio. Se o banco já tem
tabelas criadas pelo Hibernate, `baseline-on-migrate` marca o esquema existente
como versão 1 e aplica `V2__unique_user_identifiers.sql`. Antes de atualizar
um banco existente, confira CPFs e e-mails duplicados: a migração falhará se
houver duplicatas, para evitar escolher uma conta arbitrariamente. Faça backup
antes da primeira migração.

O Hibernate usa `ddl-auto=validate` por padrão. As credenciais do banco devem
ter permissão para executar as migrações, ou as migrações devem ser aplicadas
pelo processo de deploy.

## Primeiro gerente

Não existe endpoint público que conceda a role MANAGER. Para criar o primeiro
gerente, defina temporariamente:

```env
BOOTSTRAP_MANAGER_EMAIL=gerente@example.com
BOOTSTRAP_MANAGER_PASSWORD=senha-forte
BOOTSTRAP_MANAGER_CPF=CPF_VALIDO
BOOTSTRAP_MANAGER_NAME=Nome
BOOTSTRAP_MANAGER_LAST_NAME=Sobrenome
BOOTSTRAP_MANAGER_BIRTH_DATE=1990-01-01
```

Na inicialização, se ainda não houver gerente, a API cria usuário e funcionário
MANAGER numa transação. Se o e-mail já pertencer a outra conta, a aplicação falha
e não promove essa conta. Depois do primeiro início bem sucedido, remova todas
essas variáveis, especialmente a senha. O endpoint de funcionários só permite
novos cargos CASHIER e STOCKER.

## Permissões

| Recurso | Leitura | Escrita |
| --- | --- | --- |
| Produtos | CUSTOMER, EMPLOYEE ou MANAGER | MANAGER |
| Categorias e setores | Qualquer autenticado | MANAGER |
| Usuários, clientes e funcionários | MANAGER | MANAGER |
| Cadastro de usuário | Público; cria CUSTOMER | Público; cria CUSTOMER |

O login, logout e CSRF são descritos em [autenticação](authentication.md).
