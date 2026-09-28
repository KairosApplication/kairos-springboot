# Dependabot

O Dependabot verifica versões de dependências usadas pelo repositório e abre
pull requests (PRs) com atualizações. A configuração versionada está em
[`.github/dependabot.yml`](../.github/dependabot.yml).

## Configuração deste repositório

- O ecossistema monitorado é `github-actions`: as Actions referenciadas nos
  workflows em `.github/workflows/` são verificadas semanalmente.
- As atualizações de Actions são agrupadas em uma PR pelo grupo
  `github-actions` (`patterns: ['*']`).
- O limite é de cinco PRs de atualização de versão abertas simultaneamente.
- As PRs são propostas para a branch padrão. A configuração não define
  auto-merge; cada atualização deve passar pelos checks e pela revisão exigida
  pelas regras da branch.

O Dependabot não está configurado para abrir PRs periódicas de atualização das
dependências Maven do `pom.xml`. Essas versões são atualizadas manualmente. O
workflow [Dependency check](../.github/workflows/dependency-check.yml) continua
analisando as dependências Maven em busca de vulnerabilidades; ele não atualiza
versões.

## Ao revisar uma PR do Dependabot

1. Confira quais Actions e versões foram alteradas no diff e consulte as notas
   de versão quando houver mudança de versão principal.
2. Verifique se os checks da PR passaram, especialmente os workflows afetados
   pelas Actions atualizadas.
3. Faça a revisão e o merge conforme as regras da branch. Uma PR agrupada pode
   conter várias atualizações de Actions.

Alertas de vulnerabilidade e PRs de correção de segurança do Dependabot são
recursos distintos das atualizações periódicas de versão descritas neste
arquivo. Sua disponibilidade depende das configurações de segurança do
repositório no GitHub, que não são definidas por este `dependabot.yml`.
