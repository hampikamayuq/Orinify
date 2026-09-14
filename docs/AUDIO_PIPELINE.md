# Pipeline de áudio

## Estado do upstream na base `bfba5ecb`

1. `YouTube.player(videoId)` tenta `ANDROID_MUSIC` quando há cookie.
2. Em falha, tenta `IOS`.
3. Em nova falha, usa `TVHTML5` e substitui URLs por resultados de uma instância Piped.
4. `MusicService.createDataSourceFactory()` escolhe o formato de streaming.
5. `DownloadUtil` repete uma versão quase idêntica da seleção para downloads.
6. O formato selecionado é salvo em `FormatEntity`, cuja chave primária é somente o `videoId`.
7. `DetailsDialog` já mostra itag, MIME, codec, bitrate, sample rate, loudness e tamanho.

## Dívidas confirmadas

### Seleção duplicada

Streaming e download calculam qualidade separadamente. Ambos somam um bônus fixo de 10.240 ao
bitrate de WebM para preferir Opus. A regra não representa perfis explícitos e pode divergir ao
evoluir.

### Reutilização rígida de itag

Quando existe `FormatEntity`, o player tenta reutilizar o mesmo itag. Isso pode impedir upgrade de
qualidade depois do login ou de uma mudança de perfil.

### Chave de cache insuficiente

O `DataSpec.key` atual é o `mediaId`. Cache e download não distinguem codec, tier ou perfil de
autenticação.

### Expiração de URL

O cache de URLs salva `expiresInSeconds * 1000`, que é uma duração, mas compara o valor diretamente
com `System.currentTimeMillis()`, que é timestamp absoluto. A condição também seleciona o item
quando o valor é menor que o relógio atual. A implementação futura deve salvar
`expiresAtEpochMs = fetchedAt + ttlMs` e reutilizar somente enquanto `now < expiresAtEpochMs`.

### Proveniência perdida

`YouTube.player()` retorna apenas `PlayerResponse`; o chamador não sabe se a resposta veio de
`ANDROID_MUSIC`, `IOS`, `TVHTML5` ou Piped. Diagnóstico e fallback determinístico exigem transportar
essa informação explicitamente.

## Pipeline alvo

```text
PlaybackRequest(videoId, networkProfile, codecPreference)
    ↓
StreamUrlManager consulta cache de URL com expiração absoluta
    ↓ miss/expirado
PlayerResponseStrategy tenta clients em ordem declarada
    ↓
PlayerResponseEnvelope(response, sourceClient, authenticated, fetchedAt)
    ↓
AudioFormatMapper produz somente metadados sanitizados
    ↓
PremiumDetector avalia evidências da resposta
    ↓
FormatResolver filtra e ordena formatos deterministicamente
    ↓
ResolvedAudioFormat(format, info, capability, rationale)
    ↓
QualityCachePolicy define a cache key
    ↓
Media3 ou DownloadManager
```

## Invariantes do resolver

- apenas formatos audio-only e com URL utilizável;
- nenhum bitrate sintetizado;
- desempate total e estável, terminando por itag;
- preferência de codec nunca escolhe um formato proibido ou sem áudio;
- `MAX_AVAILABLE` significa o melhor item presente na resposta;
- o resultado inclui a justificativa técnica da escolha;
- download e streaming recebem a mesma entrada e política.

## Ordem de integração

1. observar todos os formatos sem mudar a escolha;
2. transportar proveniência do client;
3. comparar escolha legada e nova em modo sombra;
4. registrar divergências localmente;
5. ativar o resolver por perfil, com rollback simples;
6. somente depois alterar chaves e upgrades de cache/download.
