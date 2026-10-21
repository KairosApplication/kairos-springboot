# Ambiente local de desenvolvimento e testes

Requisito: Docker Desktop com Compose v2 e contêineres Linux. Não é necessário
instalar Java, Maven, PostgreSQL ou Python na máquina. Este ambiente utiliza
somente `compose.dev.yaml`, sem utilizar as credenciais do `.env` da API nem acessar bancos externos.

```sh
docker compose -f compose.dev.yaml up -d --build api seed
docker compose -f compose.dev.yaml --profile check run --rm smoke
```

A API aplica Flyway e valida o schema antes da carga de dados fictícios.
O segundo comando aguarda os serviços e verifica via HTTP: login, permissões,
CSRF, expiração real do JWT, renovação automática na mesma requisição, logout
imediato e preservação da sessão de outro dispositivo.

| Serviço | Endereço no host |
| --- | --- |
| API | http://127.0.0.1:8081 |
| Swagger | http://127.0.0.1:8081/swagger-ui/index.html |
| PostgreSQL | 127.0.0.1:5433; banco/usuário kairos_dev; senha kairos-local-only |
| Redis | 127.0.0.1:6380 |

As portas são restritas a 127.0.0.1 e podem ser alteradas com `DEV_API_PORT`,
`DEV_POSTGRES_PORT` e `DEV_REDIS_PORT`. Containers usam os nomes internos dos
serviços, independentemente dessas portas.

Contas fictícias:

| Conta | E-mail | Senha pública local |
| --- | --- | --- |
| Gerente | manager@kairos.local | Kairos-Dev-Only!123 |
| Cliente | customer@kairos.local | Kairos-Dev-Only!123 |

A empresa fictícia é `company@kairos.local`. A carga é idempotente: repetir `seed`
não duplica registros nem troca senhas de contas existentes. Essas credenciais
são fixtures e pertencem exclusivamente ao banco local deste Compose.

O JWT dura **15 segundos neste ambiente**, para exercitar a renovação. O padrão da
API continua 15 minutos. `DEV_AUTH_ACCESS_TTL=15m` pode ser usado para desenvolvimento
normal; para o smoke de expiração, mantenha 15s (o teste limita a espera a 60s).
Cookies Secure ficam desabilitados somente neste Compose, que usa HTTP local.

A chave JWT é gerada aleatoriamente no primeiro início e guardada em um volume
com permissão restrita, compartilhado somente com a API. Ela não aparece no
Compose, Git, `.env` ou saída dos comandos. Reiniciar mantém a chave e os dados.

## Suíte automatizada

```sh
docker compose -f compose.dev.yaml --profile test build tests
docker compose -f compose.dev.yaml --profile test run --rm tests
```

O serviço `tests` executa Maven verify em Linux: testes de negócio com H2, testes
de autenticação com Redis real, JaCoCo, Checkstyle e SpotBugs. O Redis de testes
é separado do Redis usado pela API e não publica portas. Maven usa um volume de
cache; a primeira execução baixa dependências. Os relatórios são copiados para
`target/development-tests/surefire-reports` e `target/development-tests/site`.

O smoke complementa essa suíte usando PostgreSQL real e as migrações da aplicação.
Os testes PostgreSQL com Testcontainers existentes continuam disponíveis com
`./mvnw -Ppostgres-tests verify` em uma máquina com Java 21 e Docker.

## Execução pelo Maven ou IDE

É possível iniciar apenas `postgres` e `redis` neste Compose e apontar a API da
IDE para as portas locais da tabela. Configure uma chave JWT própria e siga o
[contrato de autenticação](authentication.md). O container da API fornece a
opção completa, sem configuração manual de segredos ou runtime.

## Logs, parada e limpeza

```sh
docker compose -f compose.dev.yaml ps -a
docker compose -f compose.dev.yaml logs api
docker compose -f compose.dev.yaml --profile check --profile test down
```

O projeto Compose se chama `kairos-development`; seus volumes são independentes
do Compose padrão. O comando acima preserva os dados. Para apagar **somente o
ambiente local deste projeto**, incluindo banco, sessões, chave e cache Maven:

```sh
docker compose -f compose.dev.yaml --profile check --profile test down -v
```

Após limpar, o próximo início recria a chave e as fixtures. Não use arquivos de
override que troquem os serviços locais por endpoints de produção.
