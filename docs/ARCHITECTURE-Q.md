# Arquitetura do Orinify

## Objetivo

Controlar e observar o caminho entre autenticação, player response, formato escolhido, cache,
download e Media3 sem transformar o projeto em um conjunto de patches difíceis de atualizar.

## Limites

```text
Compose / UI
    ↓
Playback (Media3)
    ↓
Orinify FormatResolver
    ↓
Orinify PremiumDetector
    ↓
InnerTube / YouTube
    ↓
YouTube Music

FormatResolver ↔ Quality-aware cache ↔ Downloads
```

O módulo `innertube` continua responsável pelo transporte e parse das respostas. O módulo `app`
contém Media3, persistência e UI. Código novo de domínio deve ficar em:

```text
dev.diego.orinify.audio
dev.diego.orinify.cache
dev.diego.orinify.network
dev.diego.orinify.diagnostics
dev.diego.orinify.settings
dev.diego.orinify.ui
```

## Decisões

### ADR-001 — application ID separado, namespace preservado

O application ID é `dev.diego.orinify`. O namespace e os pacotes do upstream permanecem
`com.zionhuang.music`. Isso permite instalação lado a lado sem uma renomeação massiva que tornaria
merges futuros muito caros.

### ADR-002 — FOSS como distribuição padrão

O flavor `foss` não aplica Google Services, Firebase Crashlytics ou Firebase Performance. Métricas
do Orinify serão locais, limitadas e exportáveis pelo usuário.

### ADR-003 — capabilities, não entitlement presumido

Premium é uma conclusão sobre formatos efetivamente recebidos. Login isolado, nome da conta, itag
fixo ou expectativa de bitrate não comprovam capacidade Premium.

### ADR-004 — resolver único

Streaming e download devem chamar a mesma política determinística. `MusicService` e `DownloadUtil`
serão consumidores, não implementações concorrentes da seleção.

### ADR-005 — diagnóstico sanitizado por construção

Modelos de diagnóstico não contêm URL, cookie, cabeçalho, token ou `visitorData`. A sanitização não
depende apenas de filtros de texto aplicados depois.

## Regras de dependência

- domínio de áudio não depende de Compose, Android Context ou banco;
- UI observa estados, mas não decide qualidade;
- cache não interpreta entitlement;
- transporte não registra credenciais;
- fallback retorna o client realmente usado junto à resposta;
- persistência de configurações Q usa DataStore;
- banco só muda quando metadados duráveis não puderem ser mantidos fora dele.
