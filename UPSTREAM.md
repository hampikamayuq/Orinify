# Upstream

Orinify é derivado do [InnerTune](https://github.com/z-huang/InnerTune), distribuído sob GPL-3.0.
O histórico Git, a licença e as atribuições do projeto original devem ser preservados.

## Base inicial

- branch: `upstream/dev`
- commit: `bfba5ecb` (`Translations from Weblate (#1855)`)
- importado em: 2026-09-13

## Remotes

```text
origin    https://github.com/hampikamayuq/Orinify.git
upstream  https://github.com/z-huang/InnerTune.git
```

## Política de divergência

- `namespace = com.zionhuang.music` e os pacotes legados permanecem intactos sempre que possível.
- `applicationId = dev.diego.orinify` permite instalação lado a lado.
- funcionalidades específicas ficam preferencialmente em `dev.diego.orinify.*`.
- mudanças em `MusicService`, `DownloadUtil` e `YouTube` devem ser adaptadores pequenos.
- migrations existentes nunca são reescritas.

## Sincronização

```bash
git fetch upstream
git checkout main
git merge upstream/dev
./gradlew assembleFossDebug lintFossDebug testFossDebugUnitTest
```

Todo conflito deve ser descrito no `CHANGELOG-Q.md`, incluindo a decisão adotada e o teste de
regressão correspondente. O procedimento completo está em `docs/UPSTREAM_SYNC.md`.
