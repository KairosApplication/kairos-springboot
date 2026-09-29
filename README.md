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
- Spring Security (autenticação por sessão)

## Pré-requisitos

Antes de executar o projeto, tenha instalado:

- JDK 21;
- Maven, caso o projeto não possua Maven Wrapper;
- acesso ao PostgreSQL hospedado no Aiven;
- Redis acessível pela API.
- PostgreSQL acessível pela aplicação (local ou hospedado);
- Docker em execução apenas para os testes de integração com Testcontainers.

O repositório inclui Maven Wrapper (`mvnw` e `mvnw.cmd`), então não é necessário instalar Maven separadamente.

## Variáveis de ambiente

Na raiz do projeto, copie `.env.example` para `.env` e preencha os dados do seu PostgreSQL:

```env
API_PORT=8080
DB_URL=jdbc:postgresql://SEU_HOST:SUA_PORTA/SEU_BANCO
DB_USERNAME=SEU_USUARIO
DB_PASSWORD=SUA_SENHA
REDIS_URL=redis://localhost:6379
CORS_ALLOWED_ORIGINS=http://localhost:5173
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
JPA_DDL_AUTO=update
```

> O `.env.example` pode ser enviado ao GitHub, pois não deve conter nenhuma credencial real.

## Configuração da aplicação

A aplicação lê a conexão PostgreSQL e Redis do ambiente. O Flyway aplica as
migrações e o Hibernate valida o esquema; não use `ddl-auto=update` em produção.
Veja [segurança, Redis e migrações](docs/security-redis.md) para a configuração
completa.
O arquivo `src/main/resources/application.yaml` já contém a configuração abaixo:

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
      ddl-auto: ${JPA_DDL_AUTO:update}
    show-sql: true
    open-in-view: false

management:
  endpoints:
    web:
      base-path: /
      exposure:
        include: health

  endpoint:
    health:
      show-details: always
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

Os recursos de usuários, funcionários, clientes, categorias, setores, produtos e compras ficam sob `/api/v1`. A documentação interativa está em [`/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html) e a especificação JSON em [`/v3/api-docs`](http://localhost:8080/v3/api-docs) quando a aplicação estiver em execução. Veja o [guia do Swagger](docs/swagger.md) para testar autenticação por sessão e CSRF.

A autenticação usa sessão HTTP e cookie `JSESSIONID`, não JWT nem HTTP Basic. O fluxo é: obter um token CSRF com `GET /api/v1/auth/login`, enviar `POST /api/v1/auth/login` como formulário com `email`, `password` e o header CSRF, e consultar `GET /api/v1/auth/me`. Login válido retorna `204`; o logout é `POST /api/v1/auth/logout` e também exige CSRF. No frontend, envie `credentials: "include"` em todas as chamadas.

Além do login, são públicos `POST /api/v1/users/registration`, `/health` e as rotas do Swagger/OpenAPI. Os demais acessos seguem as regras atuais:

| Rotas | Permissão |
| --- | --- |
| `GET /api/v1/products/**` | Role `CUSTOMER` |
| `/api/v1/employees/**` | Role `MANAGER` |
| `GET /api/v1/purchases/**` | Role `MANAGER` |
| `GET /api/v1/customers/{id}/recommendations` | O próprio cliente ou um gerente |
| `/api/v1/users/**`, `/api/v1/customers/**`, `/api/v1/categories/**`, `/api/v1/sectors/**` | Usuário autenticado |

As demais rotas, inclusive métodos de escrita em `/api/v1/products/**`, são negadas pela configuração atual. Requisições que alteram estado exigem token CSRF válido. Para o fluxo completo e as limitações atuais, consulte o [guia de autenticação](docs/authentication.md).

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
