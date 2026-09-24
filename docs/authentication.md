# Autenticação por sessão

A API autentica e-mail e senha usando `CustomUserDetailsService` e o
`PasswordEncoder` existente. As roles são carregadas dos vínculos no banco:
`CUSTOMER`, `MANAGER` ou `EMPLOYEE`. Um usuário com mais de um vínculo recebe
as respectivas roles. Contas sem vínculo não conseguem entrar.

O login usa o filtro de formulário do Spring Security. Ele cria a autenticação,
renova o identificador da sessão e preserva o contexto nas próximas requisições.
HTTP Basic foi desabilitado. Não há JWT neste fluxo.

## Endpoints

| Método e rota | Entrada / resposta | Acesso |
| --- | --- | --- |
| `GET /api/v1/auth/login` | `200`, JSON com `headerName` e `token` CSRF | Público, prepara o login |
| `POST /api/v1/auth/login` | Formulário com `email` e `password`; `204` no sucesso, `401` na falha | Público, exige CSRF |
| `GET /api/v1/auth/me` | `200`, JSON com `email` e `roles` | Autenticado |
| `POST /api/v1/auth/logout` | `204`, invalida a sessão e remove `JSESSIONID` | Autenticado, exige CSRF |

O POST de login usa `application/x-www-form-urlencoded`, **não JSON**.
Os erros de login não informam se o e-mail existe. As respostas de autenticação
não retornam senha ou hash. Erros de autenticação/autorização usam status HTTP,
sem redirecionamento HTML.

A URL de login é a única URL pública de negócio: o GET nela apenas prepara o
token CSRF. Não autentica o usuário nem retorna seus dados. Acesso a rotas
protegidas sem sessão retorna `401`; uma sessão sem permissão retorna `403`.
Métodos que alteram estado sem CSRF válido retornam `403`, inclusive o login.

## Fluxo no frontend

Use `credentials: "include"` em todas as chamadas. O navegador administra o
cookie de sessão; não é necessário guardar senha ou token de autenticação em
localStorage. O token CSRF pode ficar em memória e deve ser renovado após login.

```javascript
const api = "http://localhost:8080"; // Ajuste para o endereço da API.
let csrf;

async function refreshCsrf() {
  const response = await fetch(`${api}/api/v1/auth/login`, {
    credentials: "include",
  });
  if (!response.ok) throw new Error("Não foi possível preparar a sessão");
  csrf = await response.json();
}

async function login(email, password) {
  await refreshCsrf();
  const response = await fetch(`${api}/api/v1/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      [csrf.headerName]: csrf.token,
    },
    body: new URLSearchParams({ email, password }),
  });
  if (!response.ok) throw new Error(`Falha no login: ${response.status}`);

  // O Spring troca o token CSRF após autenticar.
  await refreshCsrf();
  const me = await fetch(`${api}/api/v1/auth/me`, {
    credentials: "include",
  });
  if (!me.ok) throw new Error("Sessão indisponível");
  return me.json();
}

async function logout() {
  await refreshCsrf();
  const response = await fetch(`${api}/api/v1/auth/logout`, {
    method: "POST",
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
  });
  if (!response.ok) throw new Error(`Falha no logout: ${response.status}`);
  csrf = undefined;
}
```

Nos demais POST/PATCH/DELETE autorizados, envie o mesmo header CSRF junto do
cookie. Após recarregar a página, obtenha novamente o token pelo GET de login.
O CSRF padrão fica vinculado à sessão; invalidar a sessão também o descarta.

O CORS permite `http://localhost:5173`, credenciais e `X-CSRF-TOKEN`, incluindo
as rotas de autenticação. Em produção, configure a origem real e HTTPS,
`SERVER_SERVLET_SESSION_COOKIE_SECURE=true` e
`SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY=true`. Para front e API em sites
distintos, revise também SameSite e a política de cookies do navegador.

## Limites atuais

- As permissões existentes continuam: clientes consultam produtos e gerentes
  cadastram funcionários. Outros endpoints de negócio continuam bloqueados.
- O cadastro de funcionários ainda recebe um `userId` existente. Criar usuário
  e funcionário numa única transação e impedir cadastro de `MANAGER` pela API
  são mudanças de negócio separadas, ainda não implementadas.
- As roles são carregadas no login. Alterações de cargo/vínculo precisam de
  novo login ou de uma futura estratégia de invalidação das sessões afetadas.
- A suíte focada desta mudança é `AuthControllerIntegrationTest`; os testes
  antigos ainda precisam ser adaptados às permissões atuais.
