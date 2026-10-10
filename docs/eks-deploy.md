# Deploy da API mobile no EKS

Todo o fluxo de testes, build, publicacao ECR e deploy EKS executa no
[kairos-infra](https://github.com/KairosApplication/kairos-infra).
A API fornece apenas seu codigo; nao precisa de workflow de deploy,
Secrets AWS ou runner proprio. A CI deste repo continua verificando as alteracoes.

Credenciais de PostgreSQL e Redis ficam no AWS Secrets Manager.
O Redis deve ser compartilhado entre replicas para preservar sessoes.

## Disponibilidade

A configuracao de producao no infra usa duas replicas em nos separados,
maxUnavailable=0, readiness, espera antes de encerrar e shutdown graceful.
PostgresAuditInitializer usa lock transacional PostgreSQL para serializar seu
DDL entre replicas. Commit/rollback liberam o lock automaticamente, inclusive
em conexoes reutilizadas. Testes de concorrencia executam no perfil postgres-tests.

O lock evita conflitos de inicializacao; ainda planejar migracoes versionadas,
compativeis com as versoes antiga e nova. Rollback de imagem nao reverte banco.

Para verificar/criar/atualizar: no kairos-infra, Actions > Deploy connected services.
A verificacao periodica busca a main desta API. Configuracao e ativacao:
[kairos-infra/docs/automation.md](https://github.com/KairosApplication/kairos-infra/blob/main/docs/automation.md).
