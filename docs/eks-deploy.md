# Deploy da API mobile no EKS

O workflow `.github/workflows/deploy.yml` chama o pipeline compartilhado da
organizacao apos a CI aprovada em main, ou por execucao manual. O release valida
o SHA atual, executa os testes PostgreSQL, publica imagem imutavel no ECR e
atualiza o release Helm mobile-api com verificacao de readiness e rollback.

## Ativacao

Integrar e configurar kairos-infra e o workflow compartilhado da organizacao
antes de integrar/ativar o chamador desta API. O deploy fica desabilitado ate
a Variable KAIROS_DEPLOY_ENABLED ser definida como true.

Variables necessarias: AWS_REGION, AWS_ACCOUNT_ID, EKS_CLUSTER_NAME,
AWS_PUBLISHER_ROLE_ARN, AWS_DEPLOYER_ROLE_ARN, API_HOST e API_CERTIFICATE_ARN.
Criar o Environment production restrito a main e permitir o runner privado
da organizacao para este repositorio.

Opcionalmente, cadastrar AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY em Secrets,
com AWS_SESSION_TOKEN para credenciais temporarias. As chaves precisam de
permissao para assumir as roles publisher/deployer, e essas roles precisam
confiar no principal IAM. Sem chaves, o pipeline usa OIDC.

Credenciais de PostgreSQL e Redis ficam no AWS Secrets Manager, com as chaves
DB_URL, DB_USERNAME, DB_PASSWORD e REDIS_URL. Nao entram no Dockerfile ou Git.
O Redis deve ser compartilhado entre replicas para preservar as sessoes.

## Disponibilidade

A configuracao de producao no infra usa duas replicas em nos separados,
maxUnavailable=0, readiness, espera antes de encerrar e shutdown graceful.
PostgresAuditInitializer usa lock transacional PostgreSQL para serializar seu
DDL entre replicas. Commit/rollback liberam o lock automaticamente, inclusive
em conexoes reutilizadas. Testes de concorrencia executam no perfil postgres-tests.

O lock evita conflitos de inicializacao; ainda planejar migracoes versionadas,
compativeis com as versoes antiga e nova. Rollback de imagem nao reverte banco.

Para verificar/criar/atualizar manualmente: Actions > Deploy mobile API > Run workflow.
Uma verificacao central no infra solicita o mesmo workflow para os servicos habilitados.
O guia completo esta em
[kairos-infra/docs/automation.md](https://github.com/KairosApplication/kairos-infra/blob/main/docs/automation.md).
