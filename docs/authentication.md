# Autenticação JWT, Redis e renovação automática

A API autentica com JWT HS256 e controla as sessões no Redis. PostgreSQL mantém
usuários, senhas e perfis. Cada login cria uma sessão independente por dispositivo.

## Configuração

Copie `.env.example` para `.env` e configure PostgreSQL e `AUTH_SIGNING_KEY`.
A chave deve conter pelo menos 32 bytes aleatórios em Base64, sem valor padrão.
Todas as instâncias devem compartilhar a chave, issuer e audience.

| Variável | Padrão |
| --- | --- |
| AUTH_ISSUER / AUTH_AUDIENCE | kairos-api / kairos-clients |
| AUTH_ACCESS_TTL / AUTH_SESSION_TTL | 15m / 7d absolutos |
| AUTH_COOKIE_SECURE / AUTH_COOKIE_SAME_SITE | true / Lax |
| REDIS_URL | redis://localhost:6379 |
| REDIS_CONNECT_TIMEOUT / REDIS_COMMAND_TIMEOUT | 2s / 2s |
| CORS_ALLOWED_ORIGINS | http://localhost:5173; lista explícita separada por vírgulas |

Use `AUTH_COOKIE_SECURE=false` somente para HTTP local. `SameSite=None` exige
Secure e HTTPS. Cookies cross-site também dependem das políticas do navegador.
Não use o ambiente de produção para testes: Flyway executa migrações ao iniciar.

## Contrato

| Método e rota | Comportamento |
| --- | --- |
| GET /api/v1/auth/login | Obtém JSON `{headerName, token}` e cookie CSRF |
| POST /api/v1/auth/login | Formulário email/password + CSRF; retorna JWT no JSON e cookies |
| GET /api/v1/auth/me | Retorna `{email, roles}` com Bearer ou cookies |
| POST /api/v1/auth/refresh | Renovação explícita: refresh cookie + CSRF; retorna novos tokens |
| POST /api/v1/auth/logout | Revoga a sessão no Redis e apaga os cookies; 204 idempotente |

Login e refresh retornam:

```json
{"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":900}
```

`KAIROS_ACCESS` contém o JWT e `KAIROS_REFRESH` contém o refresh opaco. Ambos são
HttpOnly, Secure por padrão, SameSite configurável e Path=/api/v1. O refresh
nunca aparece no JSON. Cookies legados de refresh em /api/v1/auth são removidos.
`KAIROS_CSRF` também é HttpOnly; obtenha o valor mascarado para o header pelo
GET /api/v1/auth/login. Não armazene tokens em localStorage.

O cadastro público mantém id/name/email, acrescenta os campos de access token e
faz login do visitante. Se já estiver autenticado, preserva sua identidade e
retorna null nos campos de token. Uma falha no Redis após o cadastro concluir no
PostgreSQL pode exigir fazer login após recuperar a infraestrutura.

## Renovação na própria API

O navegador envia os cookies com `credentials: 'include'`. Quando o access cookie
expira ou está ausente e há um refresh válido, o filtro da API renova os tokens,
atualiza os cookies e continua a requisição original. Não é necessário tratar 401,
chamar refresh nem repetir a operação no frontend. Não há tarefa em background:
a renovação ocorre na próxima requisição, dentro dos sete dias da sessão.

Bearer continua disponível. Um Bearer expirado pode ser renovado quando houver
refresh cookie da MESMA sessão; se já houver access cookie atual da mesma sessão,
a API o utiliza sem outra rotação. Bearer inválido, assinatura adulterada ou
sessões incompatíveis são rejeitados. Sem cookie de refresh, um Bearer expirado
retorna 401. Login e refresh explícito processam suas próprias credenciais.

Requisições simultâneas da renovação automática compartilham o mesmo sucessor
por uma janela de **3 segundos**. O sucessor é derivado por HMAC com separação de
domínio, usando a chave secreta do servidor; Redis guarda apenas hashes e o
prazo de sobreposição, sem o refresh em texto puro. A operação é atômica em Lua
e funciona entre instâncias. Depois da janela, ou se o sucessor já foi consumido,
o reuso revoga a sessão. Durante esses três segundos não é possível distinguir
um reenvio legítimo de um replay do mesmo token; ambos recebem o mesmo sucessor.
A renovação explícita permanece estrita: serialize chamadas a /auth/refresh.

A expiração absoluta de sete dias não muda na rotação. Ao atingir esse prazo,
é necessário fazer login novamente. Usuário e perfis são recarregados do
PostgreSQL ao renovar; alterações de perfil aparecem no próximo JWT.

## Logout imediato e indisponibilidade

Toda requisição autenticada verifica assinatura e claims do JWT e consulta no
Redis se a sessão existe, pertence ao usuário, está ativa e não expirou. Não há
consulta ao PostgreSQL apenas para autenticar um JWT ainda válido.

O logout e a detecção de reuso invalidam também JWTs já emitidos: sua próxima
requisição retorna 401, mesmo antes de expirar. Uma requisição já autorizada e
em execução pode concluir; a revogação não desfaz operações iniciadas.

Se JWT e refresh cookie forem de sessões diferentes, o logout revoga somente a
sessão do JWT verificado. Sem JWT, o refresh prova a sessão. JWT assinado expirado
também permite logout. Chamadas repetidas continuam retornando 204. Não há logout
global de outros dispositivos. O cliente deve limpar eventual token mantido em memória.

Redis indisponível retorna 503 no login, refresh, logout e recursos autenticados;
não há fallback em memória nem aceitação de JWT sem conferir revogação. Se logout
falhar, os cookies são preservados para permitir nova tentativa. Credenciais
inválidas retornam 401; permissão insuficiente ou CSRF inválido retornam 403.

## Navegador

```javascript
const api = 'http://localhost:8080/api/v1';
async function csrf() {
  const response = await fetch(api + '/auth/login', { credentials: 'include' });
  if (!response.ok) throw new Error('CSRF: ' + response.status);
  return response.json();
}
let protection = await csrf();
const login = await fetch(api + '/auth/login', {
  method: 'POST', credentials: 'include',
  headers: { [protection.headerName]: protection.token },
  body: new URLSearchParams({ email: 'usuario@example.com', password: '<senha>' })
});
if (!login.ok) throw new Error('Login: ' + login.status);
protection = await csrf(); // login troca a proteção CSRF
const me = await fetch(api + '/auth/me', { credentials: 'include' });
// A API renova os cookies e atende esta mesma chamada quando necessário.
const logout = await fetch(api + '/auth/logout', {
  method: 'POST', credentials: 'include',
  headers: { [protection.headerName]: protection.token }
});
if (!logout.ok) throw new Error('Logout: ' + logout.status);
```

Login, cadastro, refresh e logout sempre exigem CSRF. As demais operações de
escrita também exigem CSRF quando houver cookies de autenticação, mesmo com
Bearer. Somente requisições exclusivamente Bearer dispensam CSRF. Após logout,
obtenha novo CSRF antes de fazer login novamente. O antigo JSESSIONID não autentica.

Veja [Redis, persistência e testes reais](security-redis.md).
Referências: [CSRF no Spring Security](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html)
e [rotação de refresh tokens](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14).
