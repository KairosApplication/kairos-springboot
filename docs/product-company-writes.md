# Gravação de produtos por empresa

Todas as gravações de produtos exigem gerente autenticado e CSRF. A empresa é resolvida pelo funcionário vinculado ao e-mail da sessão, usando `RestockingAccess`. Conta sem funcionário/empresa é recusada com 403.

| Método | Rota recomendada | Sucesso |
| --- | --- | --- |
| POST | `/api/v1/products/company/registration` | 201 |
| PATCH | `/api/v1/products/company/update/{id}` | 200 |
| DELETE | `/api/v1/products/company/delete/{id}` | 204 |

Os aliases antigos (`/registration`, `/update/{id}` e `/delete/{id}`) permanecem compatíveis e recebem as mesmas proteções. As novas rotas permitem que clientes dependam explicitamente desta correção: versões anteriores não expõem esses caminhos e não executam a gravação por engano.

Cadastro recebe `name`, `brand`, `categoryId` e `price`. A empresa é atribuída no servidor; parâmetros extras de empresa não escolhem o vínculo. Edição mantém a empresa existente e valida a autorização antes de alterar campos. Exclusão valida a empresa antes de excluir. Produtos de outra empresa ou sem empresa retornam 403, sem adoção automática.

As validações dos DTOs, unicidade global de nome e restrições de integridade do banco continuam aplicáveis. Produto com vínculo em gôndolas, compras ou inventário pode não ser excluído. Não há migração de produtos legados nesta entrega. Endpoints antigos de leitura global permanecem fora deste escopo; o frontend deve consumir o catálogo por empresa de `/api/v1/store-map`, entregue separadamente pela PR #37.

`ProductCompanyWritesIntegrationTest` verifica os novos caminhos e os antigos com H2: atribuição de empresa, edição/exclusão própria, rejeição entre empresas e para produtos órfãos, conta sem vínculo, CSRF e dados inválidos. Os testes são transacionais e não acessam o banco de produção.
