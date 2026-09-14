# Changelog do Orinify

Alterações específicas do fork são registradas aqui. Mudanças herdadas continuam documentadas no
histórico e nas releases do InnerTune.

## 0.5.10-q1 — em desenvolvimento

### Fundação

- importada a base `upstream/dev` no commit `bfba5ecb`;
- definido `dev.diego.orinify` como application ID, preservando o namespace do upstream;
- alterados nome, atalhos e paleta do launcher para instalação lado a lado;
- adicionada configuração de assinatura release somente por variáveis de ambiente;
- protegidos `*.jks`, `*.keystore` e `keystore.properties` no `.gitignore`;
- CI de push/PR alterada para o flavor FOSS sem Firebase;
- pipeline de tag preparado para verificar assinatura e publicar SHA-256.

### Observabilidade

- criado o modelo sanitizado `AudioFormatInfo`;
- criado o mapper de formatos do player response;
- adicionados testes do parser de codec e da ausência de URL no modelo de diagnóstico.

### Validação local

- build FOSS de debug concluído com Android SDK 35;
- testes unitários e lint concluídos sem erros;
- APK verificado com `apksigner`;
- package `dev.diego.orinify.debug`, versão `0.5.10-q1`, confirmado no artefato.

### Correções da Fase 0

- URLs de stream passam a expirar de verdade: `StreamUrlCache` converte o tempo de vida informado
  pelo player em um instante absoluto, com margem de 60 s e relógio injetável. Antes uma duração era
  comparada com o relógio, a condição era sempre verdadeira e a URL expirada era reutilizada até o
  processo morrer;
- reutilização de itag deixou de derrubar a faixa: quando o client que respondeu não oferece o itag
  já tocado, a seleção normal é usada em vez de `ERROR_CODE_NO_STREAM`;
- removidos os pontos de crash na resolução de formato: `contentLength!!` e
  `split("codecs=")[1]`; formatos sem URL utilizável são descartados antes da escolha;
- download sem `contentLength` informado não usa mais um teto inventado de 10 MB, que truncava
  faixas longas;
- caches de URL de playback e download são separados e limpos quando a conta muda;
- `isAuthenticated` passa a exigir SAPISID, alinhado ao ADR-003 e ao que a UI já considerava login;
- verificador de atualização compara a tag da release com o `versionName`, eliminando o aviso
  permanente de atualização;
- diretório do DataStore excluído de backup e transferência, para o cookie de sessão não sair do
  aparelho;
- `Timber.DebugTree` plantado somente em builds de debug.

### Fase 1 — gate e manutenção

- workflows de push e pull request unificados, com `concurrency` por branch, eliminando o build
  duplicado de uma branch com PR aberto;
- `:innertube:test`, `:kugou:test` e `:lrclib:test` entraram no gate; os testes que chamam serviços
  reais deixaram de ser desativados e passam a ser pulados salvo `ORINIFY_NETWORK_TESTS=1`;
- removida a flag `-DskipFormatKtlint`, que nenhum plugin lia, e adicionado ktlint de verdade,
  restrito aos fontes do fork para não gerar conflito de formatação com o upstream;
- builds de release preservam `SourceFile` e `LineNumberTable`, e a release publica o `mapping.txt`
  ao lado do APK, falhando se ele não existir;
- Dependabot passa a propor atualizações mensais e agrupadas de actions e Gradle;
- Hilt migrado de kapt para KSP e `android.enableJetifier` desligado;
- metadados apontam para o fork: templates de issue atualizados, `FUNDING.yml` e `crowdin.yml`
  removidos;
- `orinify_strings.xml` permanece em inglês e português do Brasil, sem plataforma de tradução.

### Fase 2 — resolver único em modo sombra

- `FormatResolver` determinístico: ordem total e estável por bitrate, família de codec e itag, sem
  inventar bitrate e sem deixar a preferência de codec promover um formato que o servidor avaliou
  abaixo;
- `LegacyFormatPolicy` guarda a regra herdada, incluindo o bônus fixo de 10.240 para WebM, para que
  a comparação em sombra use a regra real e não uma cópia;
- a expressão de escolha saiu de `MusicService` e `DownloadUtil`; os dois passam a chamar a mesma
  política, primeiro passo concreto do ADR-004;
- `ResolverShadow` conta as comparações e guarda as últimas divergências, sem alterar o que toca;
- estratégia de clients declarada, com resultado de cada tentativa (`OK`, `NOT_PLAYABLE`,
  `LOGIN_REQUIRED`, `TRANSPORT_ERROR`, `DISABLED`) carregado no envelope;
- falha de transporte em um client deixou de encerrar a tentativa: cada client é tratado
  separadamente e a exceção original é relançada se todos falharem;
- fallback via instância pública do Piped passou a ser opcional e desligado por padrão, com
  interruptor em Configurações do player.

### Correção de clients do player

- `ANDROID_MUSIC` passou de `5.01` para `7.27.52` e `ANDROID_VR` substituiu o `IOS` na cadeia do
  player: as versões herdadas foram aposentadas no servidor e respondiam HTTP 400
  FAILED_PRECONDITION, o que derrubava toda faixa com "erro desconhecido";
- a chave de API legada só é enviada por clients que ainda a declaram;
- falha total do player deixou de ser "erro desconhecido": `PlayerUnavailableException` carrega o
  que cada client respondeu e a mensagem mostra, por exemplo, `ANDROID_MUSIC=TRANSPORT_ERROR (HTTP
  400)`, preservando a cadeia de causas para que rede e timeout mantenham mensagem própria;
- resposta não tocável sem motivo do servidor mostra o mesmo resumo em vez de diálogo em branco;
- consequência: o fallback por Piped ficou inócuo, porque o client `TVHTML5` que ele usa também é
  recusado.

### Diagnóstico da falha total de playback

Medições contra o endpoint real, com o corpo e os cabeçalhos que o app envia:

| Client | Versão | Resposta anônima |
| --- | --- | --- |
| `ANDROID_VR` | 1.60.19 | `OK`, 27 formatos, URLs diretas, zero `signatureCipher` |
| `ANDROID_MUSIC` | 7.27.52 | `LOGIN_REQUIRED` |
| `WEB_REMIX` | 1.20220606.03.00 | `UNPLAYABLE` |
| `ANDROID` | 17.13.3 | HTTP 400 |
| `TVHTML5` | 2.0 | `ERROR` |

Conclusões: `ANDROID_VR` é hoje o único client que toca sem sessão, o que confirma a cadeia
escolhida; `ANDROID` continua aposentado; e o fallback por Piped segue inócuo, porque o client que
ele usa responde `ERROR`.

Também ficou medido que um `hl` malformado — vazio, `und` ou com subtags de extensão como
`pt-BR-u-ca-gregory` — faz o servidor responder `ERROR` sem nenhum formato, em qualquer client. O
app já filtra o idioma por uma lista conhecida antes de montar o contexto, então não é a causa da
falha relatada, mas é o motivo de o `hl` não poder passar a ser derivado direto de
`Locale.toLanguageTag()`.

### Relato de falha utilizável

- cada tentativa passa a ser identificada por nome **e versão** do client, por exemplo
  `ANDROID_VR/1.60.19`. Uma versão aposentada no servidor era indistinguível de uma versão atual
  que foi recusada, e a mensagem não dizia qual build a produziu;
- o motivo da falha deixa de depender de o corpo da resposta ainda estar legível: quando ele já foi
  consumido, o token é extraído da mensagem da exceção, onde o Ktor o guarda. Continua saindo dali
  apenas um token limitado (`[A-Z_]+` ou uma razão curta), nunca o texto bruto, que carrega a URL da
  requisição;
- resposta de erro sem nenhum detalhe passa a ser reportada como `HTTP 400 no detail`, para separar
  "o servidor não explicou" de "não conseguimos ler a explicação";
- coberto por testes com `MockEngine`, incluindo o caso em que o corpo traz um segredo junto do
  token e só o token sai.

### Pendente antes da primeira release

- keystore pessoal criada fora do repositório e secrets configurados;
- smoke tests de login, playback, download e Android Auto;
- implementação da tela de diagnóstico sem alterar ainda a seleção de stream.
