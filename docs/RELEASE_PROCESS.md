# Processo de release

## Uma vez por máquina

Crie a keystore fora do repositório e escolha senhas únicas. Exemplo interativo:

```bash
keytool -genkeypair \
  -keystore /caminho/seguro/orinify-release.jks \
  -alias orinify \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Não colocar senhas na linha de comando ou no histórico do shell. Fazer backup offline da keystore.

## Variáveis locais

```text
ORINIFY_KEYSTORE_FILE=/caminho/seguro/orinify-release.jks
ORINIFY_KEY_ALIAS=orinify
ORINIFY_KEY_PASSWORD=<secret>
ORINIFY_STORE_PASSWORD=<secret>
```

Build:

```bash
./gradlew assembleFossRelease lintFossRelease testFossReleaseUnitTest
```

## Secrets do GitHub

```text
KEYSTORE_BASE64
KEY_ALIAS
KEY_PASSWORD
STORE_PASSWORD
```

`KEYSTORE_BASE64` é a representação base64 do arquivo binário, sem quebras incompatíveis com o
secret. Nunca imprimir o valor durante o workflow.

## Versionamento

- `versionName`: versão upstream + sufixo Q, por exemplo `0.5.10-q1`;
- `versionCode`: inteiro estritamente crescente, inicialmente `2601`;
- tag: `v0.5.10-q1`;
- atualizar `CHANGELOG-Q.md` antes da tag.

## Gate de release

1. árvore limpa e sincronizada com o remoto desejado;
2. CI de push e PR verde;
3. smoke test em Samsung;
4. login, playback, download e offline;
5. troca Wi-Fi ↔ móvel;
6. Android Auto básico;
7. atualização sobre APK anterior;
8. diagnóstico exportado inspecionado por segredos;
9. tag anotada criada no commit validado.

## Verificação do artefato

```bash
apksigner verify --verbose Orinify.apk
sha256sum Orinify.apk
```

O workflow de tag `v*-q*` executa essas verificações, publica APK e arquivo `.sha256` e cria a
GitHub Release. O hash publicado deve ser comparado após download.

## Recuperação

Se a keystore for perdida, builds novas não atualizarão instalações existentes. Não gere uma nova
chave fingindo continuidade: publique com novo application ID ou instrua uma reinstalação com perda
de estado claramente documentada.
