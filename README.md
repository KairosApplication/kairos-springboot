# Kairos API PostgreSQL

API REST do projeto Kairos, desenvolvida com Spring Boot e conectada a um banco PostgreSQL hospedado no Aiven.

## Tecnologias

- Java 17 ou superior
- Spring Boot 4
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- Spring Boot Actuator

## Pré-requisitos

Antes de executar o projeto, tenha instalado:

- JDK 17 ou superior;
- Maven, caso o projeto não possua Maven Wrapper;
- acesso ao PostgreSQL hospedado no Aiven.

## Variáveis de ambiente

Na raiz do projeto, crie um arquivo chamado `.env`:

```env
API_PORT=8080
DB_URL=jdbc:postgresql://SEU_HOST:SUA_PORTA/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=SUA_SENHA
```

O arquivo `.env` contém credenciais reais e não deve ser enviado para o GitHub.

Adicione ao `.gitignore`:

```gitignore
/.env
```

O arquivo `.env.example` deve possuir as mesmas variáveis, mas somente com valores fictícios:

```env
# Porta HTTP da API
API_PORT=8080

# Conexão PostgreSQL/Aiven
DB_URL=jdbc:postgresql://SEU_HOST:SUA_PORTA/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=SUA_SENHA
```

> O `.env.example` pode ser enviado ao GitHub, pois não deve conter nenhuma credencial real.

## Configuração da aplicação

O arquivo `src/main/resources/application.yaml` deve estar configurado assim:

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

No Aiven, a URL deve começar com:

```text
jdbc:postgresql://
```

E normalmente precisa conter:

```text
?sslmode=require
```

## Estrutura dos arquivos

```text
kairos-api-postgres/
├── src/
│   └── main/
│       └── resources/
│           └── application.yaml
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

Caso o projeto não possua Maven Wrapper:

```bash
mvn spring-boot:run
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
