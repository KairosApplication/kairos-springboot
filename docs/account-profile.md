# Perfil da própria conta

`GET /api/v1/auth/profile` exige autenticação e retorna somente o perfil
vinculado à sessão. Não recebe ID de usuário ou empresa para selecionar outra conta.

Campos: `userId`, `email`, `name`, `lastName`, `birthDate` (ISO), `cpf`,
`employeeId`, `customerId`, `companyId`, `companyName` e `position`.
Dados opcionais ausentes são `null`; senha, tokens e credenciais não são retornados.
A resposta usa `Cache-Control: no-store`. O Android mascara o CPF.

Cliente recebe seu vínculo de cliente/mercado; funcionário ou gerente recebe
seu vínculo de funcionário/mercado. `/auth/me` e `/auth/chat-context` mantêm
seu contrato mínimo e não passam a expor CPF ou nascimento ao serviço de IA.

Esta alteração é somente leitura. Não adiciona edição de CPF/nascimento,
recuperação de conta, migração de banco ou alteração de credenciais.

Após integrar e publicar a API, o Android atualizado carrega os dados no login
e na restauração da sessão, preenchendo saudação, Perfil e Segurança nos dois fluxos.
