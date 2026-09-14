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

### Pendente antes da primeira release

- keystore pessoal criada fora do repositório e secrets configurados;
- smoke tests de login, playback, download e Android Auto;
- implementação da tela de diagnóstico sem alterar ainda a seleção de stream.
