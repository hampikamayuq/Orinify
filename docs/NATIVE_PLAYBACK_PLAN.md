# Plano de recuperação do player nativo

Data: 14/09/2026. Estado: planejado; reprodução nativa ainda não resolvida.
Branch de trabalho: `fix/playback-review`.

## Objetivo e evidências

Restabelecer reprodução e downloads pelo player nativo, inclusive em segundo plano,
preservando a conta, os metadados e as preferências de qualidade do usuário.

O usuário confirmou que músicas tocam dentro da página do YouTube Music, mas não no
player nativo. As capturas mostram HTTP 400 INVALID_ARGUMENT ao enviar a sessão web a
clientes Android e LOGIN_REQUIRED nas tentativas sem assinatura. Isso demonstra a falha
nesse caminho de requisições; não comprova qual requisito adicional resolverá o player web.

Já implementado nesta branch:

- validação explícita do login antes de persistir a sessão;
- restrição de cookies aos clientes web e remoção da escada de credenciais;
- rejeição de respostas OK sem URL de áudio utilizável;
- rejeição de SAPISID vazio e correção do diagnóstico de autenticação do Piped;
- alternativa visível de reprodução web, limitada ao primeiro plano;
- testes de regressão para as correções e para validação das URLs/origens.

A alternativa web não encerra o incidente. Os testes locais não demonstram reprodução
real no aparelho. Última execução: 85 testes aprovados e 13 testes previamente ignorados;
APK FossDebug compilado e assinatura verificada.

## Referências auditadas

1. [InnerTune #1789 — Player fixes](https://github.com/z-huang/InnerTune/pull/1789),
   head `d0655c7034a45f46e98dbd42b8f663ce66773947`: integração NewPipeExtractor,
   signatureTimestamp, decodificação de signatureCipher e parâmetro n, validação das URLs,
   fallback e separação entre metadados e stream.
2. [Limitação declarada pelo autor](https://github.com/z-huang/InnerTune/pull/1789#issuecomment-2708588457):
   faltava suporte a PO tokens; o trabalho continuou no OuterTune.
3. [OuterTune #294](https://github.com/OuterTune/OuterTune/pull/294): suporte a PO tokens,
   incorporado em 09/02/2025.
4. [OuterTune #326](https://github.com/OuterTune/OuterTune/pull/326): ciclo de vida dos tokens,
   troca de conta, visitor data e logout; incorporado em 23/02/2025.
5. [InnerTune #1871](https://github.com/z-huang/InnerTune/pull/1871): atualizações de clientes e
   pesquisa; não contém a integração completa de extração/token necessária para esta proposta.
6. [InnerTune #1869](https://github.com/z-huang/InnerTune/pull/1869): estabilidade e cache;
   não resolve por si só a rejeição de credenciais.

Esses PRs são referências de arquitetura, não garantia de funcionamento em setembro de
2026. Não aplicar os diffs integralmente nem copiar versões antigas de dependências.

## Etapa 1 — Selecionar uma implementação atual e reproduzir o contrato

- Comparar a implementação atual do extrator e do suporte a tokens com os PRs acima.
- Fixar versões/commits, registrar licença, API utilizada e requisitos Android/JVM.
- Verificar compatibilidade com minSdk 24, Java 17, Kotlin 2.0.10, Ktor 2.3.12 e AGP 8.6.0.
  O OuterTune #326 elevou minSdk para 26; não importar essa mudança implicitamente.
- Medir requisições anônimas de referência, registrando somente client/versão, status,
  presença de formatos e tipo de URL. Não confundir um status OK com áudio utilizável.
- Para sessão real, executar medições no aparelho sem exportar cookies, tokens ou URLs.

Entrega: nota de compatibilidade e dependências fixadas; decisão explícita sobre suporte
Android antes de alterar requisitos do aplicativo.

## Etapa 2 — Implementar resolução de URLs web

- Introduzir um adaptador de extração em `innertube` com downloader que respeite o proxy,
  timeouts e cancelamento. Fechar respostas e limitar tentativas.
- Modelar `signatureCipher` como opcional na resposta, sem quebrar URLs diretas.
- Obter o signatureTimestamp do player e incluí-lo no contexto de reprodução pertinente.
- Resolver assinatura e parâmetro n por implementação mantida, sem tabelas fixas copiadas
  de um JavaScript antigo. Manter resultados de transporte somente em memória.
- Resolver os formatos antes de aplicar a regra que rejeita respostas sem URLs utilizáveis.
- Classificar falhas de extração separadamente de HTTP, login e ausência de formatos.

Entrega: URLs diretas e ofuscadas passam pelo mesmo contrato de resolução; falhas não
são apresentadas como sucesso nem interrompem indevidamente o fallback.

## Etapa 3 — Integrar contexto de sessão e PO tokens

- Adaptar a implementação mantida de PO tokens, incluindo os contextos exigidos pelo
  player e pelo servidor de mídia. Não presumir que um mesmo token serve a todos.
- Vincular tokens à sessão/identidade pertinente; invalidar em login, logout, troca de
  conta e expiração. Evitar visitor data padrão emprestada de outra sessão.
- Invalidar também requisições em andamento: uma resolução da sessão anterior não pode
  repovoar o cache depois de logout ou troca de conta.
- Preservar desafios/interações exigidos pelo site; não tratar recusa do servidor como
  autorização para remover controles de autenticação.
- Não registrar credenciais, scripts com dados de sessão ou URLs assinadas em logs,
  exceções de interface ou relatórios de testes.

Entrega: sessão e tokens coerentes durante toda a resolução, com testes de expiração,
troca de conta, cancelamento e isolamento entre sessões.

## Etapa 4 — Integrar reprodução, downloads e cache

- Compartilhar o resolvedor entre MusicService e DownloadUtil.
- Preservar metadados musicais do cliente principal; identificar separadamente o cliente
  que forneceu o áudio. Não inferir Premium somente pela existência de cookie.
- Respeitar seleção de qualidade/itag sem forçar formatos indisponíveis para a conta.
- Validar o stream com uma operação limitada apropriada ao endpoint; não aceitar apenas
  o status OK do JSON. Fechar conexões e não baixar a faixa inteira como teste de URL.
- Em URL expirada/403, invalidar o cache e permitir no máximo uma nova resolução adequada;
  impedir loops. Usar o cache com prazo absoluto e margem já existente.
- Preservar filas, seek, normalização e reprodução em segundo plano.

Entrega: o mesmo contrato de stream atende reprodução e download, com erros diagnosticáveis.

## Etapa 5 — Validação e APK de teste

Testes locais necessários:

- fixtures sintéticas de URL direta, signatureCipher, parâmetro n e campos ausentes;
- requisição autenticada somente nos clientes compatíveis;
- URL recusada seguida de fallback, cancelamento e limite de retries;
- expiração de URL/token, logout e troca de conta durante uma resolução;
- ausência de dados sensíveis nos diagnósticos serializados.

Executar os testes de `innertube`, testes FossDebug do aplicativo e lint pertinente,
compilar o APK e verificar assinatura. Usar a mesma chave debug para permitir atualização
sobre as versões já instaladas. Só então fornecer um APK claramente identificado.

Matriz no aparelho, com a conta e a rede do usuário:

- faixas que falharam nas capturas e outras faixas comuns;
- início, pausa, seek, próxima faixa e retorno à fila;
- reprodução por pelo menos dez minutos com tela apagada e app em segundo plano;
- download completo e reprodução offline;
- Wi-Fi e rede móvel; logout/login e troca de conta quando disponível;
- qualidade Premium apenas se houver assinatura legítima e formatos disponíveis.

## Critério de conclusão

O incidente só será encerrado quando o APK corrigido tocar no player nativo do aparelho,
com tela apagada, e completar um download reproduzível offline. Testes unitários, login
validado ou reprodução dentro da página web não substituem esses critérios.

Até lá, manter a alternativa web identificada como temporária. Cada etapa deve resultar
em um commit revisável; preservar as mudanças do fork e não publicar credenciais, APKs,
keystores ou dados de conta no Git. O push desta branch não faz merge na main nem publica release.
