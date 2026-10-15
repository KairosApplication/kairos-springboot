# Autenticação JWT e refresh tokens

Java 21, Spring Boot 4.0.7 e Spring Security gerenciado pelo Boot. PostgreSQL
continua responsável por usuários, senhas e perfis. O login reaproveita
AuthenticationManager e PasswordEncoder. Entidades e migrações permanecem iguais.

Preservamos o transporte por cookies. Não há código de cliente neste repositório
que exija refresh em JSON; adotamos navegador como padrão. Aplicativos nativos
com cookie jar podem manter esse contrato, guardando cookies em armazenamento
seguro do sistema operacional. Não foi criado frontend.

## Configuração e execução

Preencha .env a partir de .env.example. AUTH_SIGNING_KEY é obrigatória: pelo
menos 32 bytes aleatórios em Base64, sem chave padrão em produção. Gere uma
chave uma vez e guarde no ambiente/gerenciador de segredos; todas as instâncias
precisam compartilhar chave, issuer e audience. Uma troca de chave invalida JWTs.

Exemplo PowerShell (variável somente nesta sessão; não exibe a chave):

```powershell
$keyBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)
$env:AUTH_SIGNING_KEY = [Convert]::ToBase64String($keyBytes)
docker compose up -d redis
./mvnw.cmd spring-boot:run
```

Para executar API e Redis com Compose, configure a chave no .env e execute
`docker compose --profile app up -d --build`. Não teste contra produção:
a inicialização mantém as migrações Flyway existentes.

| Variável | Padrão/finalidade |
| --- | --- |
| AUTH_SIGNING_KEY | Obrigatória, Base64 de pelo menos 32 bytes aleatórios; HS256 |
| AUTH_ISSUER / AUTH_AUDIENCE | kairos-api / kairos-clients |
| AUTH_ACCESS_TTL / AUTH_SESSION_TTL | 15m / 7d (absoluto) |
| AUTH_COOKIE_SECURE | true; false somente em HTTP local |
| AUTH_COOKIE_SAME_SITE | Lax; None exige Secure e HTTPS |
| REDIS_URL | redis://localhost:6379; suporta credenciais e rediss:// |
| REDIS_CONNECT_TIMEOUT / REDIS_COMMAND_TIMEOUT | 2s / 2s |
| CORS_ALLOWED_ORIGINS | Origens explícitas separadas por vírgula, sem curingas |

## Contrato

| Método e rota | Entrada | Resposta |
| --- | --- | --- |
| GET /api/v1/auth/login | Pública | 200 {headerName, token} e cookie CSRF |
| POST /api/v1/auth/login | Formulário email/password, cookie e header CSRF | 200 access JSON e refresh cookie |
| POST /api/v1/auth/refresh | Refresh cookie, cookie e header CSRF | 200 novo access JSON e novo refresh cookie |
| GET /api/v1/auth/me | Authorization: Bearer accessToken | 200 {email, roles} |
| POST /api/v1/auth/logout | Refresh cookie ou JWT, cookie e header CSRF | 204 e remoção dos cookies |

Login/refresh retornam:
```json
{"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":900}
```

Refresh nunca aparece no JSON. KAIROS_REFRESH é HttpOnly, Path=/api/v1/auth,
SameSite configurável e Secure por padrão. Sua duração acompanha o restante da
sessão, sem prorrogar 7 dias. KAIROS_CSRF também é HttpOnly, Path=/api/v1;
obtenha o token a enviar pelo JSON de GET /api/v1/auth/login.

O cadastro público mantém id/name/email e acrescenta accessToken/tokenType/expiresIn
para visitantes, com refresh cookie. Se houver JWT autenticado, mantém a identidade
atual e retorna null nos campos de token. Se Redis falhar após o cadastro PostgreSQL
concluir, o usuário pode já existir: recupere a infraestrutura e faça login.

Login, cadastro público, refresh e logout sempre exigem CSRF, mesmo com Bearer.
Obtenha novo CSRF após login/cadastro e logout; refresh preserva o atual.
Nas demais rotas, autenticação exclusivamente por Bearer dispensa CSRF.
Cookie refresh e o antigo JSESSIONID não autenticam recursos protegidos.
Clientes que esperavam login 204 e JSESSIONID precisam migrar para JSON e Bearer.

## Exemplo no navegador

Execute de uma origem permitida. Mantenha access apenas em memória. Cookies são
gerenciados pelo navegador; não armazene refresh em localStorage.

```javascript
const api = 'http://localhost:8080/api/v1';
let accessToken, csrf;
async function loadCsrf() {
  const response = await fetch(api + '/auth/login', { credentials: 'include' });
  if (!response.ok) throw new Error('Falha no CSRF');
  csrf = await response.json();
}
await loadCsrf();
const login = await fetch(api + '/auth/login', {
  method: 'POST', credentials: 'include',
  headers: { [csrf.headerName]: csrf.token },
  body: new URLSearchParams({ email: 'usuario@example.com', password: '<senha>' })
});
if (!login.ok) throw new Error('Login: ' + login.status);
accessToken = (await login.json()).accessToken;
await loadCsrf();
await fetch(api + '/auth/me', {
  headers: { Authorization: 'Bearer ' + accessToken }
});

// Compartilhe a Promise entre chamadas; serialize também entre abas/processos.
let refreshing;
function refresh() {
  return refreshing ??= (async () => {
    const response = await fetch(api + '/auth/refresh', {
      method: 'POST', credentials: 'include',
      headers: { [csrf.headerName]: csrf.token }
    });
    if (!response.ok) throw new Error('Refresh: ' + response.status);
    accessToken = (await response.json()).accessToken;
  })().finally(() => { refreshing = undefined; });
}
await refresh();
const logout = await fetch(api + '/auth/logout', {
  method: 'POST', credentials: 'include',
  headers: { [csrf.headerName]: csrf.token }
});
if (!logout.ok) throw new Error('Logout: ' + logout.status);
accessToken = undefined;
```

Ao reabrir a aplicação, obtenha CSRF e use refresh. Não envie Bearer expirado
ao refresh/logout: o filtro rejeita o header antes de processar o cookie.
Para cross-site, configure HTTPS, SameSite=None, Secure e CORS explícito com
credenciais. Políticas do navegador sobre cookies de terceiros ainda se aplicam.

## TTL, atomicidade, reuso e logout

JWT contém sub (ID), email (preserva a identidade existente), sid, roles, issuer,
audience, iat, exp, jti e token_use=access. Assinatura HS256, algoritmo e claims
são validados localmente. Não há consulta a bancos apenas para validar JWT.
Consultas de negócio/cache continuam normais. Perfis mudam no próximo refresh;
access tokens emitidos mantêm os perfis anteriores até expirar.

Refresh tem locator aleatório e segredo de 256 bits (SecureRandom). O locator
sozinho não autoriza nada. Redis guarda SHA-256 do token completo e metadados
em uma chave kairos:auth:{sid}, num único hash slot para Redis Cluster.
A rotação Lua revalida o token, consome o hash anterior e insere o novo
atomicamente. Hashes consumidos permanecem até a expiração absoluta.
Usuário e perfis são recarregados do PostgreSQL antes de concluir a rotação.

Token desconhecido não revoga sessões. Reuso de hash consumido revoga somente
a família correspondente. Serialize refresh, inclusive entre abas: duas chamadas
com o mesmo token podem produzir 200 e 401, revogando também o novo refresh.
Logout e rotação compartilham a chave; revogação impede renovações posteriores.
Logout é idempotente, inclusive quando não existe cookie/sessão a revogar.
Se Bearer e cookie identificarem sessões diferentes, somente a sessão do JWT
verificado é revogada; sem Bearer, a prova é o refresh cookie. Mantenha os tokens
do cliente sincronizados, pois o cookie recebido é apagado no logout.

**Logout e detecção de reuso não invalidam imediatamente JWTs emitidos.**
Eles continuam válidos até expirar (15 minutos por padrão), pois não há uma
lista de revogação consultada por requisição.

Credenciais/token inválidos retornam 401; permissão insuficiente ou CSRF retorna
403; indisponibilidade de Redis retorna 503 sem emissão de tokens. Não há fallback
em memória. Logout retorna 503 se não conseguir confirmar revogação; tente novamente.
Não registre senhas, tokens completos ou Authorization em logs/proxies.

Veja [persistência, eviction e testes reais](security-redis.md).
Referências oficiais: [JWT no Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
e [CSRF e cookies](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).
