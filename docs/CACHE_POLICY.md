# Política de cache e download

## Problema atual

O upstream usa `videoId` como chave do Media3 e `FormatEntity.id`. Isso permite reutilizar bytes e
metadados de qualidade diferente como se fossem equivalentes.

## Identidade lógica alvo

```text
videoId : codecFamily : qualityTier : authProfile
```

Cada componente precisa de serialização canônica, separador escapado e versão de schema. A cache
key não inclui URL, token, cookie ou cabeçalho.

Exemplo conceitual:

```text
v1:dQw4w9WgXcQ:opus:premium:account-7f31
```

O sufixo de conta, se necessário, é um pseudônimo local com salt armazenado no dispositivo. Ele não
deve permitir recuperar e-mail, channel ID ou segredo da sessão.

## Metadados

```json
{
  "schemaVersion": 1,
  "videoId": "...",
  "codec": "opus",
  "bitrate": 251600,
  "sampleRate": 48000,
  "channels": 2,
  "itag": 251,
  "qualityTier": "premium",
  "createdAtEpochMs": 0,
  "accountProfile": "account-7f31"
}
```

## Regras

- `MAX_AVAILABLE` nunca aceita silenciosamente uma entrada inferior como definitiva;
- reprodução imediata pode usar cache inferior quando offline ou para reduzir startup, desde que a
  UI e as métricas indiquem a qualidade real;
- upgrade ocorre em background somente com rede permitida e espaço suficiente;
- substituição só acontece depois de download completo, validação e troca atômica;
- falha no upgrade preserva a cópia anterior;
- downloads offline são imutáveis durante playback;
- exclusão de conta remove a associação local, sem registrar credenciais.

## Migração

Entradas antigas baseadas em `videoId` são classificadas como `LEGACY_UNKNOWN`. Elas podem tocar,
mas não comprovam codec/tier. A migração deve ser preguiçosa e nunca apagar downloads antes de uma
substituição válida.

## URLs

URLs assinadas não fazem parte da identidade persistente. O cache de resolução guarda somente em
memória `url`, `fetchedAtEpochMs`, `expiresAtEpochMs` e client, e é limpo no logout. Diagnósticos
exportados omitem a URL inteira.
