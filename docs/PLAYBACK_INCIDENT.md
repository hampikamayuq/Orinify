# Falha total de playback — relatório

Registro do diagnóstico da falha em que **toda** faixa parava em 0:00 com "Erro desconhecido".
Escrito enquanto o problema ainda está aberto: o que está fechado aparece como medido, o que é
hipótese aparece como hipótese, e as hipóteses que caíram continuam aqui, porque descartar um
suspeito é resultado.

Nenhum dado desta investigação viola `SECURITY.md`: não há cookie, `SAPISID`, `Authorization`,
`visitorData` real, URL assinada nem identificador de conta em nenhum trecho abaixo.

## Sintoma

Aparelho Samsung, Android, conta logada, 5G e Wi-Fi. Toda faixa falha em 0:00, de qualquer artista,
em qualquer rede. O container onde o desenvolvimento acontece nunca reproduziu a falha.

## Linha do tempo

| Commit | Mudança | Resultado no aparelho |
| --- | --- | --- |
| — | estado herdado do upstream | "Erro desconhecido", sem nenhuma informação |
| `dcdf079` | clients atualizados, `PlayerUnavailableException` com rastro | `HTTP 400` sem token |
| `81fc900` | sessão deixa de ir a clients sem conta; token do servidor no detalhe | `HTTP 400` |
| `fd45d9c` | chave de API legada removida; repetição anônima | `HTTP 400` |
| `b9ef54a` | versão do client no rastro; detalhe lido do corpo em cache | `HTTP 400 INVALID_ARGUMENT` |
| `8b60544` | `ANDROID_VR` passa a receber a sessão | mesmo 400, agora com o rastro completo |
| `25b2a9c` | modo `no-visitor` | mesmo 400 no modo novo |
| `7db9768` | escada de apresentações de credencial | aguardando |

## O que está medido

Medições feitas contra o endpoint real, com o corpo e os cabeçalhos que o app envia.

### Clients

| Client | Versão | Resposta anônima |
| --- | --- | --- |
| `ANDROID_VR` | 1.60.19 | `OK`, 27 formatos, URLs diretas, zero `signatureCipher` |
| `ANDROID_MUSIC` | 7.27.52 | `LOGIN_REQUIRED` |
| `WEB_REMIX` | 1.20220606.03.00 | `UNPLAYABLE` |
| `ANDROID` | 17.13.3 | HTTP 400 |
| `TVHTML5` | 2.0 | `ERROR` |

`ANDROID_VR` é o único client que toca sem sessão e o único que devolve URLs diretas, por isso
ancora a cadeia. `ANDROID` segue aposentado. O fallback por Piped é inócuo: o client que ele usa
responde `ERROR`.

As versões herdadas do upstream (`ANDROID_MUSIC` 5.01, `IOS` 19.29.1, `TVHTML5` 2.0) eram recusadas
pelo servidor. Essa era a causa do "Erro desconhecido" original, e está corrigida.

### Locale

`hl` malformado — vazio, `und`, ou com subtags de extensão como `pt-BR-u-ca-gregory` — faz o
servidor responder `ERROR` com zero formatos, em qualquer client. **Não** é a causa desta falha: o
app filtra o idioma por uma lista conhecida antes de montar o contexto. Fica registrado porque é o
motivo de `hl` nunca poder passar a sair direto de `Locale.toLanguageTag()`.

### Rastro do aparelho

Com a sessão presente, em `7db9768` e anteriores:

```
ANDROID_MUSIC/7.27.52             = TRANSPORT_ERROR (HTTP 400 INVALID_ARGUMENT)
ANDROID_MUSIC/7.27.52 no-visitor  = TRANSPORT_ERROR (HTTP 400 INVALID_ARGUMENT)
ANDROID_MUSIC/7.27.52 anon        = LOGIN_REQUIRED
ANDROID_VR/1.60.19                = TRANSPORT_ERROR (HTTP 400 INVALID_ARGUMENT)
ANDROID_VR/1.60.19 no-visitor     = TRANSPORT_ERROR (HTTP 400 INVALID_ARGUMENT)
ANDROID_VR/1.60.19 anon           = LOGIN_REQUIRED
```

O padrão não depende do client. Toda requisição **com** a sessão é recusada como argumento
inválido; toda requisição **sem** ela é respondida com uma exigência de login. As duas são idênticas
fora os cabeçalhos de credencial.

## Bugs reais corrigidos no caminho

Independentes da falha principal, todos com teste.

### `parseCookieString` truncava valores e podia lançar

```kotlin
val (key, value) = it.split("=")
```

Valores de cookie contêm `=` rotineiramente — o preenchimento base64 de `__Secure-3PSIDTS` já
garante — e a divisão em todo `=` truncava esses valores. Uma entrada sem nenhum `=` lançava
exceção dentro do setter de `cookie`, que atribui `field` antes de reconstruir `cookieMap`: o
resultado seria enviar o cookie **sem conseguir assiná-lo**. Corrigido para dividir só no primeiro
separador e ignorar entradas malformadas.

### URLs de stream não expiravam

Uma duração era comparada com o relógio, a condição era sempre verdadeira, e a URL expirada era
reutilizada até o processo morrer. Causa provável dos 403 após horas de reprodução.

### O relato de falha não era utilizável

Três capturas seguidas da mesma falha não continham informação capaz de estreitar nada. Foi preciso
corrigir isso antes de corrigir qualquer outra coisa:

- cada tentativa carrega **nome e versão** do client, o que distingue uma versão aposentada de uma
  versão atual recusada, e revela imediatamente um APK desatualizado;
- o token do servidor é lido do corpo ou, quando ele já foi consumido, do cache que o Ktor guarda na
  mensagem da exceção. Só um token limitado sai dali, nunca o texto bruto, que embute a URL;
- uma resposta sem explicação lê `HTTP 400 no detail`, separando "o servidor não explicou" de "não
  conseguimos ler a explicação";
- a mensagem mostra a razão do servidor **e** o rastro. Antes escolhia uma das duas, e a razão
  sozinha não diz qual client a ouviu.

## Hipóteses descartadas

Cada uma custou uma rodada. Ficam registradas para não voltarem.

| Hipótese | Como caiu |
| --- | --- |
| Versões de client aposentadas | Era causa do sintoma original, não deste 400 |
| Locale `BR`/`pt-BR` | 200 medido |
| `visitorData` vazia ou codificada | 200 medido |
| `androidSdkVersion`/`deviceMake` ausentes | 200 medido, sem diferença |
| Host, `x-origin`, `X-YouTube-Client-Name` numérico | 200 medido em todas as variantes |
| Chave de API legada junto de credenciais | Removida; o 400 permaneceu, só mudou de token |
| Sessão enviada a client sem conta | `ANDROID_VR` anônimo continua recebendo 400 quando autenticado |
| `visitorData` emprestada de outra sessão | Modo `no-visitor` recusado igual |
| `SAPISIDHASH` malformado | Formato verificado: hex minúsculo sobre `timestamp SAPISID origin` |

## Erro de método

A análise original leu o código e **nunca chamou a API**. Por isso não viu que as versões de client
já eram recusadas pelo servidor — o defeito estava num contrato externo, não no código, e nenhuma
leitura de código o encontraria.

O padrão se repetiu depois em escala menor: quatro rodadas de hipótese sem medição, cada uma
custando um ciclo de build, instalação e teste do usuário. O que quebrou o ciclo não foi uma
hipótese melhor, foi parar de adivinhar e fazer o app relatar o que acontece.

A lição generaliza: **um contrato com um serviço externo não se audita lendo código.**

## Estado atual

`7db9768` sobe uma escada de apresentações de credencial. Cada degrau muda exatamente um elemento
contra o primeiro, então o degrau que o servidor aceitar identifica sozinho o que faltava:

| Degrau | O que muda | Aparece no rastro como |
| --- | --- | --- |
| 1 | cookie + `SAPISIDHASH` | (sem sufixo) |
| 2 | mais a chave pública de API | `key` |
| 3 | mais `X-Goog-AuthUser: 0` | `authuser` |
| 4 | cookie sem `Authorization` | `unsigned` |
| 5 | sem sessão | `anon` |

Medido anonimamente, os cinco são inócuos: todos respondem 200. É uma sonda deliberada, não o
comportamento final — quando um degrau responder, a escada colapsa para ele.

### O que este container não consegue fazer

Reproduzir a falha exige uma sessão que o servidor valide. Sem isso, a metade autenticada de
qualquer hipótese só pode ser testada no aparelho. É por isso que a sonda existe.
