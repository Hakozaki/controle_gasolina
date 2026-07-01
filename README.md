# Controle Gasolina

App Android simples para registrar abastecimentos de veiculos.

## Funcionalidades

- Cadastro e selecao de veiculo
- Registro de quilometragem atual opcional
- Registro de posto de combustivel
- Registro de litros abastecidos
- Selecao do tipo de combustivel
- Registro do valor total em R$
- Timestamp automatico no momento do cadastro
- Localizacao GPS quando o usuario concede permissao
- Historico local dos abastecimentos

## Como gerar o APK

```bash
./gradlew assembleDebug
```

O APK gerado fica em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Como instalar em um celular conectado

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Os dados ficam salvos localmente no aparelho usando `SharedPreferences`.
