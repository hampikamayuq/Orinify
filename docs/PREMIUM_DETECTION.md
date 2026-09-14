# Detecção de capacidade Premium

## Princípio

Orinify não modifica entitlement. O detector classifica a capacidade observada na resposta do
servidor, não o plano comercial que o usuário acredita possuir.

```kotlin
enum class PremiumCapability {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE,
    AUTH_REQUIRED,
}
```

## Entradas

- sessão autenticada ou anônima;
- client usado na requisição;
- status de playability;
- lista completa de formatos audio-only;
- `audioQuality`, bitrate, sample rate, channels, MIME e itag;
- opcionalmente, baseline anônimo da mesma faixa e janela temporal.

## Regras conservadoras

- sem autenticação quando o teste exige conta: `AUTH_REQUIRED`;
- resposta inválida, vazia, parcial ou client desconhecido: `UNKNOWN`;
- evidência explícita e consistente de formato superior autenticado: `AVAILABLE`;
- `UNAVAILABLE` só após resposta autenticada válida e comparação suficiente, nunca por uma única
  ausência causada por faixa, região, client ou erro transitório;
- itag e bitrate isolados são evidência técnica, não prova universal de Premium;
- o texto da UI deve dizer “capacidade observada”, não garantir o plano da conta.

## Teste A/B

Para a mesma faixa:

1. obter resposta anônima;
2. obter resposta autenticada;
3. manter client e contexto comparáveis;
4. normalizar formatos com `AudioFormatInfo`;
5. comparar conjuntos por codec, bitrate, sample rate, `audioQuality` e tamanho;
6. registrar somente metadados sanitizados;
7. repetir com mais de uma faixa antes de concluir indisponibilidade.

O relatório deve mostrar valores reais, por exemplo `251.6 kbps`, e não rótulos promocionais. Se a
resposta autenticada não melhorar, o resultado permanece inconclusivo até separar possíveis causas:
autenticação, perfil do client, catálogo, região, resposta parcial ou mudança no YouTube.

## Privacidade

O relatório não contém:

- cookie ou nomes de cookie;
- `Authorization`;
- OAuth token;
- URL do stream ou query string;
- `visitorData`;
- e-mail ou nome da conta;
- identificador de dispositivo.

`isAuthenticated` é apenas um booleano. Se no futuro for necessário correlacionar cache à conta,
usar um identificador local aleatório ou hash com salt local, nunca um valor de autenticação.
