# Segurança, Redis e PostgreSQL

## Autenticação e Redis

JWT em Bearer ou cookie HttpOnly identifica a sessão. Cada requisição autenticada
consulta Redis para confirmar que ela está ativa: logout/reuso invalidam também
JWTs ainda não expirados. Veja o [contrato](authentication.md).

Cada sessão usa um hash `kairos:auth:{sid}`, em um único slot no Redis Cluster.
O hash contém usuário, prazo absoluto, revogação e hashes SHA-256 dos refresh
tokens ativos/consumidos. Lua usa o relógio Redis e mantém o TTL absoluto em sete
dias. Tokens desconhecidos não autorizam revogação.

A renovação automática aceita um sucessor idêntico por três segundos para suportar
requisições simultâneas. A API deriva o sucessor por HMAC; Redis nunca guarda o
token completo. Fora dessa janela, reuso revoga a sessão. A renovação explícita
não tem tolerância. Os testes cobrem ambas as regras e corridas entre instâncias.

Configure AOF, volume persistente, backups, ACL/TLS e `noeviction`. Falta de memória
ou indisponibilidade falha com 503, inclusive na validação de access tokens.
Remover chaves de sessão exige novo login. Restaurar um backup antigo pode
restaurar tokens anteriores à revogação: após recuperação com perda de dados,
invalide as sessões restauradas. AOF/replicação não garantem perda zero em falhas.
O Redis também é usado pelo cache de negócio.

As antigas sessões JSESSIONID não autenticam. As chaves antigas expiram com o
TTL anterior. Não há fallback de autenticação em memória.

Testes com Redis real descartável (Docker em execução):

```sh
./mvnw -B -ntp -Dintegration.redis=true -Dintegration.redis.container=true verify
```

Com Redis isolado já disponível, como na CI:

```sh
./mvnw -B -ntp -Dintegration.redis=true -Dtest.redis.port=6379 verify
```

A suíte remove somente as próprias chaves; nunca aponte para produção. O padrão
usa mocks nos testes web; TTL, rotação, concorrência, revogação e logout imediato
são verificados com Redis real quando `integration.redis=true`.

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
