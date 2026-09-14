# Plano de melhorias do Orinify

Data da análise: 2026-09-14. Base analisada: `main` em `5b1a8d7` (fork de `upstream/dev` em
`bfba5ecb` mais três commits do Orinify).

## Resumo executivo

O fork está bem fundamentado: a documentação de arquitetura, segurança e testes é clara, a CI
FOSS está verde, a assinatura de release é feita apenas por variáveis de ambiente e o primeiro
passo do pipeline de observabilidade (`AudioFormatInfo` e `PlayerResponseEnvelope`) já existe.

A distância entre o que os docs prometem e o que o código faz ainda é grande. Os problemas com
maior impacto no usuário hoje são:

1. o cache de URL de stream nunca expira, em `MusicService` e em `DownloadUtil`;
2. a reutilização rígida de itag derruba a faixa com "sem stream" quando o client de fallback não
   oferece aquele itag;
3. duas asserções `!!` e um `split` frágil no caminho de playback derrubam o app com formatos sem
   `contentLength` ou sem `codecs`;
4. o diagnóstico de stream é perdido no pré-carregamento da próxima faixa e em cache hit, então a
   tela mostra dados antigos do `FormatEntity`;
5. o verificador de atualização compara o título da release (`v0.5.10-q1`) com o `versionName`
   (`0.5.10-q1`) e vai anunciar atualização perpétua após a primeira release;
6. o cookie de sessão fica em DataStore sem exclusão de backup na nuvem.

O plano abaixo organiza o trabalho em cinco fases. As fases 0 e 1 cabem em uma semana e não
alteram a política de seleção de formato. As fases 2 a 4 implementam o pipeline alvo descrito em
`AUDIO_PIPELINE.md` na ordem de integração já documentada.

## O que está bom e deve ser preservado

- ADRs curtos e verificáveis em `ARCHITECTURE-Q.md`; a regra "resolver único" é a correta.
- Modelo de diagnóstico sanitizado por construção, com teste que prova a ausência da URL.
- Separação `applicationId` novo + namespace legado, que reduz custo de merge.
- Release só por tag `v*-q*`, com verificação `apksigner` e SHA-256 publicado.
- CI de PR com `PULL_REQUEST=true` desativando plugins Firebase.
- Política de conflitos com upstream escrita antes do primeiro merge.

## Diagnóstico por área

### A. Correção de playback (bugs confirmados)

| # | Achado | Onde | Efeito |
|---|---|---|---|
| A1 | TTL da URL armazena `expiresInSeconds * 1000` (duração) e compara com `currentTimeMillis()` (instante). A condição `duration < now` é sempre verdadeira. | `MusicService.kt:637` e `:706`; `DownloadUtil.kt:71` e `:113` | URL expirada é reutilizada até o processo morrer. Resultado típico: 403 no meio da faixa após ~6 h de app aberto, sem refresh. |
| A2 | Se existe `FormatEntity`, o código exige o mesmo itag na nova resposta e, se não encontrar, lança `ERROR_CODE_NO_STREAM` mesmo havendo outros formatos de áudio. | `MusicService.kt:666-681`; `DownloadUtil.kt:79-83` | Faixa tocada via `ANDROID_MUSIC` e depois servida por `IOS` ou `TVHTML5/PIPED` falha. Também bloqueia upgrade de qualidade após login. |
| A3 | `format.contentLength!!` e `mimeType.split("codecs=")[1]` sem proteção. | `MusicService.kt:696,699`; `DownloadUtil.kt:103,106` | `NullPointerException` ou `IndexOutOfBounds` dentro do resolver do ExoPlayer; o erro chega ao usuário como "erro desconhecido". |
| A4 | Range fixo `range=0-contentLength` no download usa `10000000` como fallback quando o tamanho é nulo. | `DownloadUtil.kt:93` | Download truncado silenciosamente para faixas longas sem `contentLength`. |
| A5 | `isAuthenticated = cookie != null`, mas a UI considera logado apenas quando há `SAPISID`. | `YouTube.kt:445` | Diagnóstico pode dizer "autenticado" com cookie vazio ou inválido, contrariando ADR-003. |
| A6 | Dependência hardcoded de `pipedapi.kavin.rocks` no fallback final. | `InnerTube.kt:147-150` | Instância pública sem SLA; envia o `videoId` a um terceiro; se cair, o terceiro nível de fallback vira erro. |

### B. Diagnóstico e observabilidade

| # | Achado | Onde | Efeito |
|---|---|---|---|
| B1 | `currentAudioFormatInfo` só é preenchido quando `mediaId` é o item atual. O ExoPlayer resolve a próxima faixa antes da transição, e `onMediaItemTransition` zera o estado. | `MusicService.kt:534,683-688` | Ao avançar de faixa, o diálogo cai no `FormatEntity` antigo. Em cache hit nada é preenchido. |
| B2 | Não há evento estruturado de resolução (client tentado, motivo da escolha, duração, fallback usado). | `YouTube.kt:441-490` | Impossível comparar escolha legada e nova "em modo sombra", que é o passo 3 da ordem de integração. |
| B3 | `Timber.DebugTree` plantado incondicionalmente, inclusive em release. | `App.kt:46` | Logs de release no logcat; qualquer log futuro com URL viola `SECURITY.md`. |
| B4 | ProGuard sem `LineNumberTable` e sem publicação de `mapping.txt`. | `proguard-rules.pro`; `release.yml` | Stack trace de release inutilizável em um fork sem Crashlytics. |

### C. Segurança e privacidade

| # | Achado | Onde | Efeito |
|---|---|---|---|
| C1 | `backup_rules.xml` e `data_extraction_rules.xml` excluem cache e downloads, mas não o diretório `datastore/`, onde vive `innerTubeCookie`. | `res/xml/backup_rules.xml` | Cookie de sessão sobe para o backup do Google e para transferência entre aparelhos. |
| C2 | Cookie em DataStore Preferences em texto claro. | `PreferenceKeys.kt:148` | Aceitável no modelo de ameaça atual, mas o logout precisa garantir limpeza de `songUrlCache` e do `PlayerResponseEnvelope` em memória, o que hoje não acontece. |
| C3 | Sem Dependabot ou Renovate, sem `dependency-review`. | `.github/` | Contraria "revisar mudanças em repositórios adicionais como JitPack" de `SECURITY.md`. |

### D. Build, CI e dependências

| # | Achado | Onde | Efeito |
|---|---|---|---|
| D1 | `-DskipFormatKtlint` na CI não corresponde a nenhum plugin; não há ktlint nem detekt. | `build.yml`, `build_pr.yml` | Flag morta; nenhum gate de estilo ou de código morto. |
| D2 | `build.yml` roda em push de qualquer branch e `build_pr.yml` roda em PR. Uma branch com PR aberto builda duas vezes. Sem `concurrency`. | `.github/workflows` | Minutos de CI dobrados; runs obsoletos não são cancelados. |
| D3 | Hilt via `kapt`; Room já usa KSP. | `app/build.gradle.kts:15,172` | kapt é o passo mais lento do build e está em modo de manutenção. Hilt suporta KSP desde 2.48. |
| D4 | `material3 1.3.0-rc01` e `navigation 2.8.0-rc01` em produção; `enableJetifier=true` provavelmente sem necessidade. | `libs.versions.toml`; `gradle.properties` | Versões RC em release; Jetifier atrasa o build. |
| D5 | Único teste no `app` cobre o mapper. Teste do `innertube` está `@Ignore` e depende de rede. O gate `testFossDebugUnitTest` não executa testes dos outros módulos. | `app/src/test`, `innertube/src/test` | `TEST_PLAN.md` lista oito componentes com testes obrigatórios; nenhum existe ainda. |
| D6 | `Updater` compara `release.name` com `BuildConfig.VERSION_NAME`. O workflow cria a release com `--title "$GITHUB_REF_NAME"`, ou seja, `v0.5.10-q1`. | `Updater.kt:15-17`; `SettingsScreen.kt:86`; `release.yml` | Após a primeira release, todo usuário verá "atualização disponível" para sempre. |

### E. Identidade do fork e metadados

| # | Achado | Onde |
|---|---|---|
| E1 | `bug_report.yml` aponta para a build de debug do InnerTune; `FUNDING.yml` aponta para o mantenedor do upstream. | `.github/ISSUE_TEMPLATE`, `.github/FUNDING.yml` |
| E2 | `fastlane/metadata` descreve o InnerTune; `crowdin.yml` ainda aponta ao projeto upstream de tradução. | `fastlane/`, `crowdin.yml` |
| E3 | Strings do Orinify existem só em `values` e `values-pt-rBR`; sem processo de tradução definido para `orinify_strings.xml`. | `res/values*/orinify_strings.xml` |

### F. Débitos herdados que valem menção

- 14 usos de `runBlocking` e 58 asserções `!!` no módulo `app`; os que importam estão no resolver
  de `DataSource`, listados em A3.
- `MaxSongCacheSizeKey` é lido uma vez na criação do `SimpleCache`; mudar o limite exige
  reiniciar o app.
- `GlobalScope` no `App.kt` para observar cookie e `visitorData`.
- `MusicService.kt` com 821 linhas mistura serviço Media3, política de qualidade, persistência de
  fila e Discord RPC. O fork não deve refatorar esse arquivo, mas deve parar de crescer nele.

## Plano por fases

### Fase 0 — Correções imediatas (sem mudar a política de seleção)

Objetivo: remover falhas que afetam o usuário hoje, com hunks pequenos e cobertos por teste.

1. **Expiração de URL** (A1). Criar `dev.diego.orinify.network.StreamUrlCache` com
   `expiresAtEpochMs = fetchedAt + (expiresInSeconds - margem) * 1000`, margem de 60 s, relógio
   injetável. `MusicService` e `DownloadUtil` passam a usar a mesma instância. Limpar no logout.
   Teste: TTL, margem, relógio avançado, entrada ausente.
2. **Fallback quando o itag não existe** (A2). Se o itag salvo não estiver na resposta, cair na
   seleção normal em vez de lançar `ERROR_CODE_NO_STREAM`. Registrar a divergência no
   `FormatEntity`.
3. **Endurecer o parse de formato** (A3, A4). Reaproveitar `codecFromMimeType` do mapper no lugar
   do `split`; tratar `contentLength` nulo como desconhecido; no download, omitir o `range` quando
   o tamanho é nulo em vez de truncar em 10 MB.
4. **`isAuthenticated` significa `SAPISID` presente** (A5). Expor `YouTube.isLoggedIn` calculado
   pelo mesmo `parseCookieString` que a UI usa.
5. **Verificador de atualização** (D6). Comparar `tag_name` sem o prefixo `v`, ou alinhar o título
   da release com `versionName`. Teste unitário do parser.
6. **Excluir `datastore/` do backup** (C1) nas duas regras XML.
7. **Logs só em debug** (B3): plantar `DebugTree` apenas quando `BuildConfig.DEBUG`.

Critério de aceite: CI verde, teste novo para cada item 1 a 5, smoke test de playback por mais de
seis horas com tela desligada sem 403.

**Status: implementada.** Os sete itens estão na branch `claude/analise-plano-melhorias-4h776p`.
`assembleFossDebug`, `lintFossDebug` e `testFossDebugUnitTest` passam na CI. O `StreamUrlCache` e a
normalização de versão têm 18 testes unitários. Falta apenas o smoke test em aparelho real, que não
pode ser feito por build automatizado e é o último item do critério de aceite. O teste de
`hasAuthenticatedSession` existe mas só entra no gate com a Fase 1, porque o gate atual não executa
os testes do módulo `innertube`.

### Fase 1 — Fundação de qualidade e CI

Objetivo: fazer o gate automatizado corresponder ao `TEST_PLAN.md`.

1. Remover `-DskipFormatKtlint`; adicionar ktlint (ou detekt) apenas para `dev.diego.orinify.*`
   e para arquivos tocados pelo fork, para não gerar diff massivo contra o upstream.
2. Unificar `build.yml` e `build_pr.yml` em um workflow com `on: [push, pull_request]`,
   `concurrency` por ref e `cancel-in-progress: true`.
3. Adicionar `./gradlew :innertube:test :kugou:test :lrclib:test` ao gate, com os testes de rede
   marcados por categoria e desligados por padrão.
4. Publicar `mapping.txt` como asset da release e ativar `-keepattributes SourceFile,LineNumberTable`
   com `-renamesourcefileattribute SourceFile` (B4).
5. Dependabot para GitHub Actions e Gradle, com agrupamento por ecossistema e revisão manual (C3).
6. Migrar Hilt de kapt para KSP e remover o plugin kapt (D3). Testar `enableJetifier=false` (D4).
7. Atualizar `bug_report.yml`, `FUNDING.yml`, `crowdin.yml` e `fastlane/metadata` para o Orinify
   ou removê-los (E1, E2). Decidir se `orinify_strings.xml` entra no Weblate ou fica em duas
   línguas (E3).

Critério de aceite: um único workflow de build, tempo de CI menor que o atual, Dependabot abrindo
PRs, release com `mapping.txt`.

### Fase 2 — Resolver único e proveniência

Objetivo: cumprir ADR-004 sem alterar ainda o comportamento observável.

1. Criar `dev.diego.orinify.audio.FormatResolver` puro (sem Android), recebendo
   `List<AudioFormatInfo>`, `NetworkProfile` e `CodecPreference`, retornando
   `ResolvedAudioFormat(format, rationale)`. Desempate total terminando por itag.
2. Portar a regra legada (bitrate com sinal e bônus WebM de 10.240) como `LegacyPolicy`, para que
   a fase de sombra compare exatamente o mesmo cálculo.
3. Introduzir `PlayerResponseStrategy` no `innertube`, com ordem de clients declarada, retorno do
   client realmente usado e classificação de erro (terminal, retry permitido, auth exigida).
   Tornar o fallback Piped opcional e configurável, desligado por padrão (A6).
4. Modo sombra: `MusicService` continua usando a regra legada, mas registra localmente quando
   `FormatResolver` escolheria outro formato. Contador e último exemplo visíveis em uma tela de
   diagnóstico.
5. `DownloadUtil` passa a chamar o mesmo resolver.

Testes obrigatórios: lista vazia, formatos sem URL, apenas vídeo, desempate, perfis Wi-Fi e
móvel, preferência Opus versus AAC, ordem de clients, erro terminal versus retry.

Critério de aceite: divergência entre legado e novo igual a zero nos cenários de teste; sombra
ativa em pelo menos uma release Q antes da fase 3.

### Fase 3 — Diagnóstico confiável e detecção de capacidade

1. Substituir `currentAudioFormatInfo` por um `LruCache<String, AudioFormatInfo>` de tamanho 32
   populado no resolver, e derivar o valor atual de `currentMediaMetadata` (B1). Em cache hit,
   preencher a partir do `FormatEntity` e marcar a origem como `CACHE`.
2. Estender `FormatEntity` com `sourceClient`, `isAuthenticated`, `audioQuality`, `channels` e
   `resolvedAtEpochMs`, via `AutoMigration` 12 → 13, sem reescrever migrations anteriores.
3. Evento `ResolutionEvent` sanitizado (B2): clients tentados, client usado, formato escolhido,
   justificativa, duração, tipo de erro. Guardado em ring buffer em memória e exportável como
   JSON pelo `DiagnosticSanitizer` descrito em `SECURITY.md`, com teste que falha se qualquer
   campo contiver `http`, `cookie`, `SAPISID` ou `Authorization`.
4. `PremiumDetector` com os quatro estados de `PREMIUM_DETECTION.md`, alimentado só por
   `AudioFormatInfo`. UI usa o texto "capacidade observada".
5. Tela de diagnóstico em `dev.diego.orinify.ui`: formato atual, últimos eventos, contadores da
   sombra, botão de exportar.

Critério de aceite: diálogo de detalhes correto após avançar faixa e após cache hit; export
inspecionado por segredos com teste automatizado.

### Fase 4 — Cache com identidade de qualidade

Só depois de duas releases com o resolver ativo.

1. `CacheKey` versionada `v1:videoId:codecFamily:qualityTier:authProfile` com escaping e testes
   de colisão, conforme `CACHE_POLICY.md`.
2. `DataSpec.key` do player passa a usar a chave; entradas antigas viram `LEGACY_UNKNOWN` e
   continuam tocando.
3. Upgrade em background com troca atômica e preservação da cópia anterior em falha.
4. `FormatEntity` deixa de ter `videoId` como chave única e passa a permitir um registro por
   `CacheKey`.

Critério de aceite: playback offline de downloads antigos intacto após upgrade de APK; matriz
Samsung do `TEST_PLAN.md` executada.

### Fase 5 — Manutenção contínua

- Sincronização mensal com `upstream/dev` seguindo `UPSTREAM_SYNC.md`, com registro de conflitos em
  `CHANGELOG-Q.md`.
- Atualizar `media3`, `compose` e `kotlin` apenas em ciclo próprio, após a sincronização, e nunca
  na mesma release que altera cache.
- Manter `MusicService`, `DownloadUtil` e `YouTube` como adaptadores; qualquer nova regra vai para
  `dev.diego.orinify.*`.

## Priorização

| Item | Impacto | Esforço | Fase |
|---|---|---|---|
| A1 expiração de URL | Alto: falha após horas de uso | Baixo | 0 |
| A2 itag rígido | Alto: faixa "sem stream" | Baixo | 0 |
| A3/A4 parse frágil | Alto: crash | Baixo | 0 |
| D6 updater | Alto: alerta perpétuo | Baixo | 0 |
| C1 backup do cookie | Alto: privacidade | Baixo | 0 |
| B1 diagnóstico perdido | Médio: feature do fork não confiável | Médio | 3 |
| D2/D1 CI | Médio | Baixo | 1 |
| D3 Hilt KSP | Médio: tempo de build | Médio | 1 |
| Fase 2 resolver | Alto: objetivo central do fork | Alto | 2 |
| Fase 4 cache key | Alto, mas arriscado | Alto | 4 |
| A6 Piped | Médio | Médio | 2 |
| E1/E2 metadados | Baixo | Baixo | 1 |

## Riscos

- **Mudar cache key antes do resolver estar estável** invalida cache dos usuários duas vezes. Por
  isso a fase 4 vem por último.
- **Refatorar `MusicService`** para "limpar" o arquivo tornaria cada merge do upstream um conflito
  grande. O plano evita isso deliberadamente.
- **Fallback Piped removido sem substituto** pode reduzir faixas reproduzíveis em regiões com
  bloqueio. Manter opcional e medir pelo `ResolutionEvent` antes de decidir.
- **Testes JVM não provam playback**: cada fase exige o smoke test físico listado em
  `TEST_PLAN.md` antes da tag.

## Próximo passo sugerido

A Fase 0 está implementada e verde na CI. Restam dois passos, nesta ordem:

1. **Smoke test em aparelho real**, o único critério de aceite da Fase 0 que build automatizado não
   cobre: reprodução por mais de seis horas com tela desligada sem 403, troca de rede, download e
   reprodução offline, login e logout. Sem isso a Fase 0 não pode ser declarada PASS, conforme a
   Definition of Done do `TEST_PLAN.md`.
2. **Fase 1**, que faz o gate automatizado corresponder ao `TEST_PLAN.md`. Convém começar pela
   unificação dos workflows com `concurrency` e pela inclusão dos testes de `innertube`, `kugou` e
   `lrclib`, que já hoje deixam código testado fora do gate.
