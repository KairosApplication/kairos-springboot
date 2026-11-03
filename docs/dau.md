# DAU de atividade autenticada

Implementação: DailyActivityFilter → DailyActivityService → register_user_access, migration Flyway V4. Fonte do ID é exclusivamente o subject do JWT autenticado; empresas são resolvidas no PostgreSQL por Employees/Customers, com UNION. Parâmetros de cliente não selecionam identidades para métricas.

Conta respostas autenticadas bem-sucedidas de /api/v1, incluindo /auth/me. Exclui login/refresh/logout/cadastro, health/docs e User-Agents identificados como bot/crawler/spider/probe. Login bem-sucedido sozinho não conta: /auth/me ou uso comercial é o evento escolhido. A conta é registrada em todos os seus vínculos atuais; não representa seleção de loja feita pela UI nem detecção universal de robôs.

V4 é aditiva e foi testada com banco novo e com o SQL antigo de kairos-data preinstalado e contendo um registro. Objetos incompatíveis/duplicações/permissões requerem revisão, não limpeza. O papel Flyway deve ter propriedade/permissão sobre função/views preexistentes; runtime precisa SELECT nos vínculos e INSERT/UPDATE na tabela. Não se adicionou SECURITY DEFINER nem grants públicos.

SQL preserva first_seen_at e usa clock_timestamp/America/Sao_Paulo e GREATEST(last_seen_at) para concorrência. Registro em REQUIRES_NEW, com falha capturada para preservar resposta/negócio. Counters sem identidade: kairos.dau.record.success/failures; WARN sem tokens/PII. Sem fila/retry durável; perda de evento pode ser recuperada por próxima atividade do dia. Latência de pool/rede permanece limitação do registro síncrono.

Testes completos em Docker/Java 21, somente ambientes locais novos:

```powershell
$repo=(Get-Location).Path
docker run --rm -v "${repo}:/app" -v kairos-modelagem-audit-m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /app maven:3.9-eclipse-temurin-21 mvn -B -Ppostgres-tests '-Dintegration.redis=true' '-Dintegration.redis.container=true' verify
```

O socket permite Testcontainers criar PostgreSQL/Redis exclusivos. Não usar DSNs/credenciais de produção. A suíte injeta falha renomeando temporariamente a função somente em seu banco novo e a restaura no finally. Não executa migrations em Aiven. Os scripts/matriz/evidências de normalização e performance estão em kairos-data/docs/FINAL_MODELAGEM_REVIEW.md. Nenhuma entrega de BI do Daniel foi alterada.
