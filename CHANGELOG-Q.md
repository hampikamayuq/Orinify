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

### Pendente antes da primeira release

- keystore pessoal criada fora do repositório e secrets configurados;
- smoke tests de login, playback, download e Android Auto;
- implementação da tela de diagnóstico sem alterar ainda a seleção de stream.
