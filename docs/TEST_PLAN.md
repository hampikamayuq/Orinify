# Plano de testes

## Gate automatizado por push/PR

```bash
./gradlew assembleFossDebug lintFossDebug testFossDebugUnitTest
```

O flavor FOSS é o gate inicial porque não depende de Google Services nem inclui telemetria externa.

## Unitários obrigatórios

| Componente | Casos mínimos |
|---|---|
| AudioFormatMapper | MIME inesperado, codec ausente, nulls, sanitização |
| FormatResolver | filtros, perfis, desempate, Opus/AAC, lista vazia |
| PremiumDetector | quatro estados, resposta parcial, A/B sem diferença |
| CacheKey | determinismo, escaping, versão e colisões |
| StreamUrlManager | TTL, margem de expiração, relógio e 403 |
| ClientFallback | ordem, retry permitido e erro terminal |
| DiagnosticSanitizer | cookies, headers, URLs e variações de caixa |
| NetworkPolicy | Wi-Fi, móvel, roaming e offline |

## Integração

```text
login → busca → faixa → player response → resolver → playback → cache → seek → next
logout → login
online → offline → online
download → playback offline → remoção
URL válida → expirada → refresh
client primário falha → fallback
```

Testes que acessam YouTube não devem integrar o gate unitário normal: são instáveis, dependem de
rede/conta e podem causar rate limit. Devem ser opt-in e nunca receber credenciais em forks ou PRs.

## Matriz física Samsung / One UI

| Cenário | Esperado |
|---|---|
| Tela desligada por 30 min | reprodução contínua |
| Doze e app otimizado | erro acionável ou continuidade documentada |
| App sem restrição de bateria | reprodução contínua |
| Wi-Fi → 5G | recuperação automática sem trocar faixa |
| 5G → Wi-Fi | recuperação automática sem loop |
| Rede ausente 20 s / 2 min | retry limitado e retomada |
| Bluetooth conectar/desconectar | audio focus/noisy corretos |
| Chamada e Maps | duck/pause/retomada conforme foco |
| Pressão de RAM | sessão restaurável |
| Reboot | biblioteca e downloads íntegros |
| Android Auto | browse, play, pause, next e metadata |

## Sequência longa

- reproduzir 50 músicas consecutivas;
- registrar startup, rebuffer, fallback, erro e cache hit;
- confirmar que nenhuma métrica contém URL ou credencial;
- repetir em Wi-Fi e rede móvel;
- comparar regressões com o baseline da mesma versão.

## Upgrade e dados

1. instalar uma build anterior assinada pela mesma chave;
2. criar biblioteca, preferências, fila e downloads;
3. atualizar o APK sem desinstalar;
4. validar migrations, playback offline e login;
5. nunca testar upgrade entre assinaturas diferentes como se fosse cenário suportado.

## Definition of Done do MVP

O MVP só fecha após os itens da especificação passarem em build automatizado e dispositivo real.
Login/Premium, transições de rede, tela desligada, Bluetooth e Android Auto não podem ser declarados
PASS apenas por testes JVM.
