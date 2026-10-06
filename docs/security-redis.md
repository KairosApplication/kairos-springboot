# Segurança, Redis e migrações

## Redis

Defina `REDIS_URL` (por exemplo, `redis://localhost:6379`) e mantenha o
Redis disponível antes de iniciar a API. Spring Session guarda as sessões HTTP
no Redis; o cookie continua chamado `JSESSIONID`. O cache de
`GET /api/v1/categories/list` também usa Redis, expira em cinco minutos e é
invalidado ao criar, editar ou excluir uma categoria. A API não usa cache de
memória como substituto caso o Redis falhe. Use `rediss://` quando o serviço
Redis exigir TLS.

As sessões expiram depois de **15 dias sem requisições**. Spring Session atualiza
o último acesso e renova o TTL no Redis quando a sessão é usada. O cookie
`JSESSIONID` é persistente, HttpOnly e SameSite=Lax; sua validade também é
renovada em requisições autenticadas. Em HTTPS, ele recebe o atributo Secure;
`SERVER_SERVLET_SESSION_COOKIE_SECURE=true` também força esse atributo.

O Compose usa AOF e o volume `redis-data` para preservar as sessões ao reiniciar
o container. Excluir esse volume ou perder os dados do Redis encerra os logins.
O cache de categorias continua com seu prazo independente de cinco minutos.

O namespace usa `spring.session.data.redis.namespace`, correspondente ao Spring
Boot 4. Sessões antigas no namespace padrão `spring:session` precisam de novo
login depois de aplicar essa configuração.

### Retomar a sessão no mobile

1. No primeiro acesso, obtenha o CSRF com `GET /api/v1/auth/login` e preserve o cookie.
2. Envie o login como formulário para `POST /api/v1/auth/login`, ou o cadastro
   como JSON para `POST /api/v1/users/registration`, com cookie e `X-CSRF-TOKEN`.
   Um cadastro público bem-sucedido autentica o novo cliente automaticamente.
3. Guarde o cookie atualizado e obtenha o novo CSRF em `GET /api/v1/auth/login`.
4. Ao reabrir o aplicativo, envie o cookie salvo para `GET /api/v1/auth/me`.
   Com 200, continue usando a API; com 401, apresente a tela de login.
5. Use o cookie em todas as chamadas e o CSRF nos POST/PATCH/DELETE. No logout,
   a API invalida a sessão; descarte também o cookie e o CSRF no aparelho.

A consulta valida o ID específico do cookie e o contexto autenticado dessa
sessão. Contar chaves ou verificar se o Redis contém algum registro não identifica
o usuário: nele também existem sessões anônimas e cache de categorias.

O aplicativo precisa de um cookie jar persistente ou armazenamento seguro
do cookie. A API não identifica um aparelho por IP ou pelo tamanho do Redis.
Cada login pode criar uma sessão independente. Atividade contínua mantém a
sessão válida sem limite absoluto; as permissões ficam na sessão e mudanças de
perfil não são propagadas automaticamente para logins existentes. Redis precisa
estar disponível para validar e renovar sessões. O fluxo mantém CSRF obrigatório.

O mobile envia `companyId` junto dos dados do cadastro público. A API exige um
ID positivo, confirma que a empresa existe e grava o vínculo em
`customers.company_id` antes de autenticar. O cadastro administrativo de cliente
também exige `companyId`. Empresas inexistentes retornam 404; ID ausente ou
inválido retorna 400. As empresas precisam existir no PostgreSQL.

Para executar os testes com Redis isolado disponível na porta 6379:

```sh
./mvnw -Dintegration.redis=true verify
```

Use `-Dtest.redis.port=PORTA` para outra porta. Os testes usam H2 para os dados
de usuário e Redis real para validar cookie, TTL, expiração e logout.

Defina `CORS_ALLOWED_ORIGINS` com as origens do frontend separadas por vírgula,
sem `/` final. Em produção, use HTTPS e configure
`SERVER_SERVLET_SESSION_COOKIE_SECURE=true`.

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
