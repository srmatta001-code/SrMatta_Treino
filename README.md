# SrMatta Treino

Aplicativo Android leve para registrar treinos de academia. Funciona 100% offline, sem login, sem anúncios.

- **Linguagem:** Kotlin, apenas o framework do Android (nenhuma biblioteca extra)
- **Compatibilidade:** Android 6.0+ (minSdk 23) — roda no Galaxy J7 Prime 2
- **Dados:** SQLite local no próprio aparelho
- **Permissão:** só `VIBRATE` (aviso de fim do descanso)

## O que o app faz

| Aba | Função |
| --- | --- |
| **Treino** | Inicia o treino, registra séries (exercício, carga, repetições), cronômetro de treino e de descanso com vibração ao terminar. A tela fica acesa durante o treino. |
| **Histórico** | Lista os treinos finalizados com duração, séries e volume (carga × repetições). Toque num treino para ver os detalhes ou excluir. |
| **Evolução** | Gráfico do peso corporal e gráfico da maior carga por treino, por exercício. |

## Como gerar o APK

### Opção A — GitHub (não precisa instalar nada)

1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta (inclusive `.github/`).
2. Abra a aba **Actions → Gerar APK → Run workflow** (ou faça um push na branch `main`).
3. Quando terminar (3–6 min), baixe `SrMattaTreino.apk` na aba **Releases** do repositório, direto pelo celular.

### Opção B — Android Studio

1. Abra esta pasta no Android Studio (Koala ou mais recente) e aguarde o Gradle sincronizar.
2. Menu **Build → Build APK(s)** para o debug, ou rode no terminal:
   ```
   ./gradlew assembleRelease
   ```
3. O arquivo sai em `app/build/outputs/apk/release/app-release.apk`.

O build `release` já vem com R8 (encolhe código e recursos) e é assinado com a chave de debug, então instala direto.

## Instalando no celular

1. Copie o `.apk` para o aparelho (ou baixe por ele).
2. Toque no arquivo; se pedir, permita **Instalar apps desconhecidos** para o navegador/gerenciador de arquivos usado.
3. Abra **SrMatta Treino**.

## Observações

- O aviso de fim do descanso (vibração) funciona com o app aberto. Por isso a tela fica acesa durante o treino.
- Os dados ficam só no aparelho. Desinstalar o app apaga o histórico.
- Se um dia quiser publicar na Play Store, troque a assinatura de debug por uma chave própria em `app/build.gradle.kts`.
