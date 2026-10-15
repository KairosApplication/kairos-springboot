# Swagger e autenticação

Abra /swagger-ui/index.html com a API em execução; o JSON está em /v3/api-docs.

1. Execute GET /api/v1/auth/login. O navegador guarda o cookie CSRF; copie o
   token da resposta para X-CSRF-TOKEN no login.
2. Execute POST /api/v1/auth/login como formulário email/password com CSRF.
   A resposta 200 contém o JWT; refresh fica exclusivamente no cookie HttpOnly.
3. Use Authorize → bearerAuth e informe apenas o access token, sem o prefixo Bearer.
4. Obtenha novo CSRF com GET /api/v1/auth/login após o login. Use esse header
   em POST /api/v1/auth/refresh e POST /api/v1/auth/logout, com cookies do navegador.
5. Após refresh, substitua o JWT no Authorize. Remova Bearer expirado antes de
   renovar: um header inválido é rejeitado antes de processar o cookie.

Cookies Secure exigem HTTPS; em HTTP local configure AUTH_COOKIE_SECURE=false.
Login, cadastro público, refresh e logout exigem CSRF. Não faça refresh em paralelo.
Logout revoga novas renovações; access JWT anterior continua válido até expirar.
Veja o [contrato e exemplos](authentication.md).
