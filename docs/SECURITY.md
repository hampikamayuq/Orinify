# Segurança e privacidade

## Dados proibidos em logs e diagnósticos

- cookies completos ou parciais, inclusive SID e SAPISID;
- cabeçalho `Authorization`;
- OAuth access/refresh tokens;
- URLs de mídia assinadas e suas query strings;
- `visitorData`;
- e-mail, nome ou ID remoto da conta;
- conteúdo de `google-services.json`;
- senhas, aliases privados ou bytes da keystore.

## Sanitização

A primeira defesa é estrutural: `AudioFormatInfo` não possui campos de transporte ou credenciais.
Para texto vindo de exceções, o exportador deverá:

1. remover cabeçalhos sensíveis sem diferenciar maiúsculas/minúsculas;
2. substituir pares de cookie por `<REDACTED>`;
3. remover query e fragmento de URLs;
4. limitar tamanho e profundidade de mensagens;
5. aplicar uma allowlist ao JSON final;
6. testar novamente o arquivo serializado antes de compartilhá-lo.

Filtros de blacklist isolados não são suficientes.

## Keystore

- nome local sugerido: `orinify-release.jks`;
- nunca versionar keystore ou `keystore.properties`;
- manter backup offline da chave e senhas;
- sem a chave original não existe atualização in-place do APK;
- CI recebe `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD` e `STORE_PASSWORD` como secrets;
- secrets não são disponibilizados a workflows de pull requests externos.

## Rede e fallback

- TLS não deve ser desativado;
- certificados não devem ser aceitos de forma permissiva;
- retries têm limite, jitter e classificação de erro;
- 401/403 não autorizam remover controles de autenticação;
- fallback não pode enviar cookies a endpoints/clientes que não foram aprovados para autenticação;
- logout limpa URLs e identificadores locais associados à sessão.

## Dependências e supply chain

- manter Gradle wrapper versionado;
- preferir actions oficiais e versões fixas/major revisadas;
- revisar mudanças em repositórios adicionais como JitPack;
- executar lint, testes, assinatura e SHA-256 em releases;
- publicar código-fonte correspondente ao APK distribuído, conforme GPL-3.0.

## Relato de falhas

Não abrir issue pública com diagnóstico bruto. Remover segredos, reproduzir com conta de teste quando
possível e compartilhar somente o menor trecho sanitizado necessário.

## Threat model inicial

Protegemos contra vazamento acidental por logs/exportação, reutilização de cache entre perfis,
keystore no Git e fallback que perde contexto de autenticação. Comprometimento do dispositivo,
servidor malicioso e engenharia reversa do APK exigem controles adicionais e não são resolvidos
apenas por ofuscação.
