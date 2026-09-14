# Sincronização com o upstream

## Preparação

```bash
git status --short
git remote -v
git fetch upstream --prune
git log --oneline --left-right main...upstream/dev
```

Não sincronizar com mudanças locais não registradas. Criar backup recuperável antes de resolver
conflitos extensos.

## Merge

```bash
git checkout main
git merge upstream/dev
```

Merge, em vez de rebase público, preserva a relação auditável com o fork. Rebase só deve ocorrer em
branches locais ainda não compartilhadas.

## Ordem para conflitos

1. preservar correções de segurança e schema do upstream;
2. preservar o application ID e assinatura do Orinify;
3. reaplicar adaptadores pequenos em `MusicService`, `DownloadUtil` e `YouTube`;
4. manter políticas no pacote `dev.diego.orinify.*`;
5. não resolver arquivos de migration “escolhendo um lado” sem revisar schema e testes;
6. registrar cada decisão relevante em `CHANGELOG-Q.md`.

## Gate pós-merge

```bash
./gradlew assembleFossDebug lintFossDebug testFossDebugUnitTest
```

Depois:

- smoke test de login e logout;
- uma faixa em streaming e uma em download;
- seek e próxima faixa;
- reprodução offline;
- URL refresh simulado;
- Android Auto básico;
- Samsung com tela desligada;
- upgrade sobre a última release Q.

## Auditoria de divergência

```bash
git diff --stat upstream/dev...main
git log --oneline upstream/dev..main
```

Se uma mudança Q puder ser implementada por composição ou adaptador, evitar editar um arquivo
central. Quando uma alteração central for inevitável, manter o hunk pequeno e coberto por teste.

## Atualização de versão

`versionName` segue `<upstream>-qN`. `versionCode` deve crescer monotonamente. Ao mudar a base de
`0.5.10` para `0.5.11`, reiniciar somente o sufixo visível (`q1`), nunca diminuir `versionCode`.
