# Kairos API PostgreSQL

API REST do projeto Kairos, desenvolvida com Spring Boot e PostgreSQL. O Aiven é uma opção de hospedagem do banco, não um requisito para executar a aplicação.

Esta documentação acompanha o código desta branch. As funcionalidades ainda não integradas à `main` ficam disponíveis nela somente após o merge das respectivas PRs.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Data JPA
- Hibernate
- PostgreSQL
- Redis
- Flyway
- Maven
- Spring Boot Actuator
- Spring Security (JWT Bearer e refresh tokens com rotação no Redis)

## Pré-requisitos

Antes de executar o projeto, tenha instalado:

- JDK 21;
- Maven, caso o projeto não possua Maven Wrapper;
- acesso ao PostgreSQL hospedado no Aiven;
- Redis acessível pela API.
- PostgreSQL acessível pela aplicação (local ou hospedado);
- Docker em execução apenas para os testes de integração com Testcontainers.

O repositório inclui Maven Wrapper (`mvnw` e `mvnw.cmd`), então não é necessário instalar Maven separadamente.

## Redis local com Docker

Para um ambiente completo e isolado, com API, PostgreSQL, Redis, contas fictícias
e testes HTTP de autenticação, use o Compose de desenvolvimento:

```sh
docker compose -f compose.dev.yaml up -d --build api seed
docker compose -f compose.dev.yaml --profile check run --rm smoke
```

A API fica em `http://127.0.0.1:8081`. Não é necessário configurar `.env`.
Veja [ambiente de desenvolvimento e testes](docs/development-environment.md)
para credenciais locais, testes automatizados, logs e limpeza.

Com o Docker em execução, inicie somente o Redis:

```sh
docker compose up -d redis
docker compose exec redis redis-cli ping
```

O segundo comando deve retornar `PONG`. O Redis fica acessível apenas na máquina local em `127.0.0.1:6379`, compatível com `REDIS_URL=redis://localhost:6379` da API. Para parar:

```sh
docker compose down
```

## API e Redis com Docker Compose

Preencha o `.env` conforme a seção de variáveis abaixo. Para construir a imagem
e iniciar a API junto com o Redis:

```sh
docker compose --profile app up -d --build
docker compose --profile app logs -f api
```

A API fica disponível em `http://localhost:8080` (ou na porta `API_PORT` do
`.env`). Dentro do contêiner, ela escuta em `8080` e usa `redis://redis:6379`;
o Compose sobrescreve apenas essas duas configurações de conexão/porta.
O PostgreSQL continua sendo o indicado por `DB_URL`, sem criar ou reinicializar
um banco no Compose. A inicialização da API mantém as migrações Flyway e a
estratégia `JPA_DDL_AUTO` já configuradas: confira o banco de destino antes de
subir. Se o PostgreSQL estiver na máquina Windows, use `host.docker.internal`
em vez de `localhost` na URL JDBC do contêiner.

O serviço `api` usa o perfil `app`: `docker compose up -d redis` continua
permitindo executar somente o Redis e iniciar a API pelo Maven/IDE.
Para parar ambos, preservando o volume do Redis:

```sh
docker compose --profile app down
```

O `Dockerfile` usa duas etapas: compilação com Java 21 e o Maven Wrapper do
projeto, seguida de execução com JRE 21 e usuário sem privilégios de root.
O cache Maven é reaproveitado pelo BuildKit. A imagem final contém o JAR;
`.env`, metadados Git e artefatos locais ficam fora do contexto pelo
`.dockerignore`. Os testes não rodam no build da imagem; execute a suíte
descrita em "Testes e integração contínua" antes de distribuir a imagem.
Esse build segue a [estrutura multi-stage do Docker](https://docs.docker.com/build/building/multi-stage/).

O Compose aguarda o health check do Redis antes de iniciar a API, conforme a
[ordenação de serviços do Docker](https://docs.docker.com/compose/how-tos/startup-order/).
O Redis mantém AOF no volume `redis-data` e usa `noeviction`, evitando remover
sessões automaticamente por pressão de memória. AOF com o fsync padrão pode
perder as gravações mais recentes em uma falha abrupta. Perder o volume exige
novo login; não use `down -v` se quiser preservar as sessões. O Redis também
armazena o cache atual da API: monitore a memória, pois `noeviction` recusa
novas gravações quando um limite configurado é atingido. O controle
de refresh tokens também depende de preservar os registros de tokens
consumidos para detectar reutilização.

## Variáveis de ambiente

Na raiz do projeto, copie `.env.example` para `.env` e preencha os dados do seu PostgreSQL:

```env
API_PORT=8080
DB_URL=jdbc:postgresql://SEU_HOST:SUA_PORTA/SEU_BANCO
DB_USERNAME=SEU_USUARIO
DB_PASSWORD=SUA_SENHA
REDIS_URL=redis://localhost:6379
CORS_ALLOWED_ORIGINS=http://localhost:5173
AUTH_SIGNING_KEY=BASE64_DE_PELO_MENOS_32_BYTES_ALEATORIOS
AUTH_COOKIE_SECURE=false
JPA_DDL_AUTO=update
```

O arquivo `.env` contém credenciais reais e não deve ser enviado para o GitHub.

O repositório já ignora esse arquivo em `.gitignore`:

```gitignore
/.env
```

O arquivo `.env.example` versionado contém as mesmas variáveis, sem credenciais reais. O valor `JPA_DDL_AUTO=update` é apropriado apenas para desenvolvimento; defina conscientemente a estratégia de schema em outros ambientes.

```env
# Porta HTTP da API
API_PORT=8080

# Conexão PostgreSQL
DB_URL=jdbc:postgresql://SEU_HOST:SUA_PORTA/SEU_BANCO
DB_USERNAME=SEU_USUARIO
DB_PASSWORD=SUA_SENHA
REDIS_URL=redis://localhost:6379
CORS_ALLOWED_ORIGINS=http://localhost:5173
AUTH_SIGNING_KEY=BASE64_DE_PELO_MENOS_32_BYTES_ALEATORIOS
AUTH_COOKIE_SECURE=false
JPA_DDL_AUTO=update
```

> O `.env.example` pode ser enviado ao GitHub, pois não deve conter nenhuma credencial real.

## Configuração da aplicação

A aplicação lê a conexão PostgreSQL e Redis do ambiente. O Flyway aplica as
migrações e o Hibernate valida o esquema; não use `ddl-auto=update` em produção.
Veja [segurança, Redis e migrações](docs/security-redis.md) para a configuração
completa.
O trecho abaixo resume a conexão e o health check. Os parâmetros de JWT e cookies
ficam em `app.auth` no mesmo arquivo; veja o [guia de autenticação](docs/authentication.md).

```yaml
server:
  port: ${API_PORT:8080}

spring:
  application:
    name: kairos-api-postgres

  config:
    import: "optional:file:./.env[.properties]"

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    database: postgresql
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}
    show-sql: false
    open-in-view: false

management:
  endpoints:
    web:
      base-path: /
      exposure:
        include: health

  endpoint:
    health:
      show-details: when-authorized
```

O Spring interpreta o `.env` como um arquivo de propriedades por causa desta configuração:

```yaml
spring:
  config:
    import: "optional:file:./.env[.properties]"
```

Uma URL JDBC PostgreSQL deve começar com:

```text
jdbc:postgresql://
```

Se o provedor exigir SSL, acrescente o parâmetro correspondente; no Aiven, normalmente é:

```text
?sslmode=require
```

## Arquivos principais

```text
kairos-api-postgres/
├── src/main/resources/application.yaml
├── src/test/
├── docs/
├── .github/workflows/
├── .env
├── .env.example
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## Executando a API

### Windows

```powershell
./mvnw.cmd spring-boot:run
```

### Linux ou macOS

```bash
./mvnw spring-boot:run
```

Por padrão, a API ficará disponível em:

```text
http://localhost:8080
```

## Health check

Para verificar o funcionamento da aplicação, acesse:

```http
GET http://localhost:8080/health
```

Exemplo de resposta:

```json
{
  "status": "UP"
}
```

Se o banco estiver inacessível, o health check poderá retornar:

```json
{
  "status": "DOWN"
}
```

## API e autenticação

Os recursos ficam sob `/api/v1`; Swagger em `/swagger-ui/index.html` e OpenAPI
em `/v3/api-docs`. A autenticação usa access JWT Bearer de 15 minutos e refresh
opaco em cookie HttpOnly, com sessão absoluta de 7 dias no Redis. Cada login
cria uma sessão independente. Rotação atômica por Lua preserva os hashes consumidos;
reuso revoga apenas a família correspondente. Logout invalida também os JWTs emitidos, com verificação da sessão no Redis
a cada requisição. A API renova automaticamente os cookies na próxima chamada
quando o access token expira.

Configure `AUTH_SIGNING_KEY` com pelo menos 32 bytes aleatórios em Base64,
`AUTH_COOKIE_SECURE=false` somente em HTTP local, e origens explícitas em
`CORS_ALLOWED_ORIGINS`. Em produção, use HTTPS e cookies Secure; para cross-site,
use `AUTH_COOKIE_SAME_SITE=None` com Secure e CSRF. O `.env.example` documenta
todos os parâmetros. Nenhum segredo de assinatura padrão é fornecido.

O contrato do login continua sendo formulário `email`/`password`, com resposta
200 contendo `accessToken`, `tokenType` e `expiresIn`. Login, cadastro público,
refresh e logout exigem cookie CSRF e o header obtido em `GET /api/v1/auth/login`.
Após login/cadastro, obtenha novo CSRF. Nas rotas protegidas, use
`Authorization: Bearer <accessToken>` ou cookies HttpOnly. Com cookies, a API
renova o JWT e atende a mesma requisição sem intervenção do frontend.
O cadastro público preserva `id`, `name`, `email` e acrescenta os campos de access
token para visitantes; se já houver JWT autenticado, mantém a identidade atual.

As regras de CUSTOMER, EMPLOYEE e MANAGER foram preservadas no `SecurityConfig`.
Para o contrato completo, exemplos e integração do cliente, consulte o
[guia de autenticação](docs/authentication.md) e o [guia do Swagger](docs/swagger.md).

## Configuração no IntelliJ IDEA

Se o Spring não encontrar o `.env`:

1. Acesse **Run → Edit Configurations**;
2. selecione a configuração da aplicação;
3. encontre o campo **Working directory**;
4. selecione a raiz do projeto, onde ficam o `.env` e o `pom.xml`;
5. encerre a aplicação e execute novamente.

Se existirem variáveis antigas em **Environment variables**, remova:

```text
API_PORT
DB_URL
DB_USERNAME
DB_PASSWORD
SPRING_DATASOURCE_URL
```

Essas variáveis têm prioridade e podem sobrescrever os valores carregados do `.env`.

## Erros comuns

### URL não encontrada

```text
Failed to configure a DataSource: 'url' attribute is not specified
```

Confira se:

- o arquivo se chama exatamente `.env`;
- ele não foi salvo como `.env.txt`;
- o `.env` está na raiz do projeto;
- `DB_URL` está preenchida;
- o diretório de execução do IntelliJ está correto.

### URL recusada pelo driver

```text
Driver org.postgresql.Driver claims to not accept jdbcUrl
```

A URL deve utilizar:

```text
jdbc:postgresql://host:porta/banco?sslmode=require
```

Não utilize:

```text
postgres://
jdbc:postgres://
```

### Dialect não identificado

```text
Unable to determine Dialect without JDBC metadata
```

Confira se o bloco `jpa` está no mesmo nível de `datasource` dentro de `spring`:

```yaml
spring:
  datasource:
    # Configuração do banco

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

## Segurança

- Nunca envie senhas ou credenciais para o GitHub;
- não coloque a senha diretamente no `application.yaml`;
- mantenha o `.env` no `.gitignore`;
- use apenas valores fictícios no `.env.example`;
- troque imediatamente qualquer senha que tenha sido exposta;
- em produção, utilize `show-details: never` no health check para não revelar informações internas.

## Testes e integração contínua

Execute a suíte com H2 e gere a cobertura JaCoCo:

```powershell
./mvnw.cmd -B -ntp clean verify
```

O relatório HTML fica em `target/site/jacoco/index.html`, acompanhado do XML para
integração com outras ferramentas. O build exige pelo menos 90% de cobertura de
linhas; em pull requests, o workflow também publica o resultado no resumo da
execução e atualiza um comentário da automação quando o token possui permissão.

O mesmo `verify` executa Checkstyle e SpotBugs. As regras locais do Checkstyle
ficam em `config/checkstyle/checkstyle.xml`; qualquer violação ou achado SpotBugs
de severidade média ou superior falha o build.

Para executar a mesma suíte com PostgreSQL 17 isolado via Testcontainers:

```powershell
./mvnw.cmd -B -ntp -Ppostgres-tests clean verify
```

Esse perfil exige Docker em execução, baixa a imagem `postgres:17-alpine` e cria
um banco descartável. Um teste adicional confirma que o banco usado é PostgreSQL,
não H2. A execução falha se Docker não estiver disponível; não há fallback silencioso.
Os testes não precisam de `.env` nem acessam o Aiven. No Linux/macOS, substitua
`./mvnw.cmd` por `bash ./mvnw`.

### Verificação de dependências

O OWASP Dependency-Check substitui o Dependency Review, sem exigir GitHub Code
Security. Analisa as dependências Maven de produção, inclusive transitivas, e
falha em vulnerabilidades com CVSS >= 7 ou erros de análise. Não compara apenas
as dependências alteradas na PR. O analisador OSS Index está desativado porque
exige credenciais próprias; a análise com NVD permanece habilitada.

```powershell
./mvnw.cmd -B -ntp -Pdependency-check dependency-check:check
```

Os relatórios HTML/JSON ficam em `target/dependency-check-report.*`. A base local
fica em `.dependency-check-data/` (ignorada pelo Git). A primeira atualização pode
demorar bastante. A verificação não faz parte do `clean verify` local padrão.

Para reduzir limites de requisições, obtenha uma chave gratuita em
[NVD API](https://nvd.nist.gov/developers/request-an-api-key) e cadastre-a como
secret `NVD_API_KEY` em **Settings → Secrets and variables → Actions**. Localmente,
o plugin lê a variável de ambiente de mesmo nome; nunca coloque a chave no POM
ou em comandos versionados. Sem a chave, o download usa o acesso público, sujeito
a lentidão e limites. PRs de forks e do Dependabot não recebem os Actions secrets
comuns; se necessário, configure também um Dependabot secret com esse nome.

### Workflows no GitHub

- `CI`: build/testes H2 com cobertura e uma segunda execução com PostgreSQL.
- `CodeQL`: analisa o código Java em PRs para a `main`, pushes na `main`,
  semanalmente e manualmente, publicando o resultado no Code Scanning.
- `Dependency check`: atualiza/cacheia a base NVD antes da análise e executa em
  PRs que alterem o build Maven, pushes na `main`, semanalmente e manualmente.
  Falhas não são ignoradas nessas execuções.
- `Secret scan`: executa Gitleaks sobre o histórico em PRs para a `main`, pushes
  na `main` e manualmente, ocultando qualquer segredo encontrado nos logs.
- Relatórios de testes, cobertura e vulnerabilidades são publicados como artefatos
  por 14 dias; relatórios ausentes em falhas de inicialização geram aviso.
- [Dependabot](docs/dependabot.md) abre PRs semanais somente para GitHub
  Actions a partir da branch padrão. As dependências Maven permanecem sob
  atualização manual.

No ruleset da `main`, exija `Analyze Java`, `Build and test`,
`PostgreSQL integration tests` e `Secret scan`. Não exija
`Dependency vulnerability check`, pois esse workflow roda apenas quando o build
Maven é alterado. Rulesets e o secret scanning nativo são configurações do
repositório no GitHub e não são representados integralmente por estes arquivos.
