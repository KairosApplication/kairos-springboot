# Review da Kairos API

Responda em português e priorize problemas concretos introduzidos pela PR. Explique o impacto e indique o trecho relevante. Evite sugestões puramente cosméticas.

- Java 21, Spring Boot 4 e Maven Wrapper. Verifique com `bash ./mvnw -B -ntp clean verify` (Windows: `./mvnw.cmd -B -ntp clean verify`).
- Preserve controller → service → repository, com DTOs e mappers nos contratos HTTP.
- Confira validação Jakarta, semântica de PATCH, normalização, duplicidades, registros inexistentes e integridade referencial.
- Verifique códigos HTTP e o formato de ApiErrorResponse no GlobalExceptionHandler.
- Não exponha senhas, hashes, CPF ou credenciais em respostas/logs; nunca inclua `.env` ou segredos reais.
- Revise Spring Security e CORS; não torne endpoints públicos nem amplie origens sem necessidade explícita.
- Confira transações e constraints; verificações de existência não substituem constraints contra concorrência.
- Mudanças comportamentais precisam de testes de regressão, incluindo entradas inválidas e erros. Os testes usam H2 em modo PostgreSQL e não devem acessar Aiven.
- Revise workflows quanto a permissões mínimas. Não use pull_request_target para executar código não confiável.
- A revisão automática complementa os mantenedores. Não sugira desativar a proteção da main para resolver falhas de CI.
