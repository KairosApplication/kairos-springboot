# Segurança, Redis e migrações

## Redis

Defina `REDIS_URL` (por exemplo, `redis://localhost:6379`) e mantenha o
Redis disponível antes de iniciar a API. Spring Session guarda as sessões HTTP
no Redis; o cookie continua chamado `JSESSIONID`. O cache de
`GET /api/v1/categories/list` também usa Redis, expira em cinco minutos e é
invalidado ao criar, editar ou excluir uma categoria. A API não usa cache de
memória como substituto caso o Redis falhe. Use `rediss://` quando o serviço
Redis exigir TLS.

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
BOOTSTRAP_MANAGER_ZIP_CODE=01000-000
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
