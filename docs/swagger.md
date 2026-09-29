# Swagger / OpenAPI

Com a API em execução, acesse:

- Interface interativa: http://localhost:8080/swagger-ui/index.html
- Especificação OpenAPI em JSON: http://localhost:8080/v3/api-docs

Se `API_PORT` for diferente de `8080`, substitua a porta nos endereços. As rotas
do Swagger são públicas. A especificação é gerada pelo Springdoc a partir dos
controllers e complementada em `OpenApiConfig`, incluindo o POST de login que é
processado pelo Spring Security.

## Autenticação no Swagger UI

A API usa uma sessão HTTP (`JSESSIONID`) e proteção CSRF. No navegador, o cookie
é administrado automaticamente. Para testar uma operação que altera dados:

1. Execute `GET /api/v1/auth/login` e copie `token` da resposta. O valor de
   `headerName` indica o nome do header, normalmente `X-CSRF-TOKEN`.
2. Execute `POST /api/v1/auth/login` com `email` e `password` no corpo
   `application/x-www-form-urlencoded` e informe o token no header CSRF.
   O sucesso retorna `204`; credenciais inválidas retornam `401`.
3. Execute novamente `GET /api/v1/auth/login` para obter o token renovado após
   a autenticação. Use esse token no header das próximas operações
   `POST`, `PATCH` e `DELETE`.
4. Confira a sessão em `GET /api/v1/auth/me`. Para sair, execute
   `POST /api/v1/auth/logout` com o token CSRF.

O botão **Authorize** mostra o esquema de cookie para descrever a segurança
da API. O navegador não permite definir o header `Cookie` manualmente no
Swagger UI; o login acima estabelece a sessão. Para chamadas feitas fora do
navegador, envie o mesmo cookie nas requisições seguintes.

O cadastro `POST /api/v1/users/registration` é público, mas exige CSRF. As
operações de leitura de produtos exigem role `CUSTOMER`; funcionários e leitura
de compras exigem `MANAGER`. As rotas de usuários, clientes, categorias e setores
exigem uma sessão autenticada. As operações de escrita de produtos aparecem na
especificação porque existem nos controllers, mas estão bloqueadas pelas regras
atuais de segurança. Para detalhes, consulte [autenticação](authentication.md).

## Respostas

O Swagger mostra os códigos de sucesso declarados pelos controllers (`200`,
`201` ou `204`) e os erros relevantes. Erros de validação, registros ausentes e
conflitos retornam o esquema `ApiErrorResponse` quando tratados pelo
`GlobalExceptionHandler`. Falhas de autenticação, autorização ou CSRF retornam
`401` ou `403` sem esse esquema de erro. A consulta de recomendações aceita
`limit` de 1 a 100, com padrão 10; um cliente só acessa as próprias recomendações,
enquanto um gerente pode consultar qualquer cliente.
