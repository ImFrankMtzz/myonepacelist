# CLAUDE.md — MyOnePaceList

App Android (Kotlin + Jetpack Compose) para llevar el avance de **One Pace** y verlo con doblaje latino o subtítulos. Sin login: todo vive en el teléfono. El usuario escribe en español, así que la UI y las respuestas van en español. Diseño aprobado: `docs/superpowers/specs/2026-11-03-myonepacelist-design.md`. Estado y siguientes pasos: `HANDOFF.md`.

## Comandos

```bash
./gradlew testDebugUnitTest            # pruebas de dominio, respaldo y catálogo incluido
./gradlew lintDebug                    # CI lo corre: debe quedar en 0 errores
./gradlew assembleDebug assembleRelease
./gradlew installDebug                 # paquete debug: com.myonepacelist.debug
cd tools/catalog && npm test           # pruebas del lector de onepace.net
cd tools/catalog && npm run sync       # regenera catalog.json (raíz) y app/src/main/assets/catalog.json
```

Toolchain que ya funciona en esta PC: AGP 9.4.1 (Kotlin integrado: no se aplica `kotlin-android`), Kotlin 2.4.21, Gradle 9.8.1, compileSdk/targetSdk 37, minSdk 26, JDK 21. SDK en `%LOCALAPPDATA%\Android\Sdk`.

## Arquitectura

```
app/src/main/java/com/myonepacelist/
  MyOnePaceApp.kt       AppContainer (DI manual), ImageLoader de Coil, observador que refresca el widget
  data/catalog/         Catalog/Arc/Episode (+ OnePaceLinks), CatalogRepository (assets → caché → remoto)
  data/progress/        AppState (progreso + ajustes) en UN archivo JSON vía DataStore (StateStore)
  domain/               lógica pura con pruebas: nextEpisode, markUpTo, recordPosition, stats, catalogDiff, BackupCodec
  ui/                   AppViewModel + MainScreen (nav tipada, hoja de episodio) + pantallas + theme/
  player/               PlayerActivity/PlayerViewModel/PlayerScreen (Media3 sobre Pixeldrain)
  work/                 CatalogSyncWorker (diario) + notificación
  widget/               NextEpisodeWidget (Glance)
tools/catalog/          Node sin dependencias: parse.mjs (payload RSC de Next.js) y sync.mjs
.github/workflows/      catalog.yml (diario), ci.yml, release.yml (tag v* → APK firmado)
```

Sin Hilt ni Room a propósito: no hay KSP (frágil con Kotlin 2.4), el proyecto es pequeño y el progreso son ~500 entradas.

## Reglas del proyecto

- **Colores:** solo tokens de `ui/theme/Theme.kt` (`Tokens.accent`, etc.), nunca hex en pantallas. Hay dos paletas (oscura "altamar" y clara "pergamino"). Revisa el contraste en ambas.
- **Tipografía:** Space Grotesk para títulos y números, Inter para el cuerpo. Fuentes variables en `res/font`.
- **Lógica nueva:** va en `domain/` como función pura y con prueba en `app/src/test`.
- **No subir a GitHub, no crear tags ni releases** sin confirmación explícita del usuario. La clave de firma (`signing/`, `*.jks`) nunca entra al repo.
- **Legal:** la app no aloja ni distribuye video; solo reproduce los enlaces públicos que One Pace publica. No agregar fuentes de video de terceros.

## Datos de One Pace y Pixeldrain (verificado)

- Los datos salen de `https://onepace.net/es-419/watch`, en el payload `self.__next_f.push` → `data.timeline.segments[].parts[].fileGroups[]`. `sub:"es-419", dub:"ja"` = subtitulado; `dub:"es-419"` = doblaje latino. `cut` puede ser `extended` o `alternate_g8` (este último se ignora).
- `part` puede ser decimal (`loguetown-01.5`): es `Double` y `Episode.number` lo formatea.
- El doblaje latino solo existe en ~36 de 477 episodios, y 69 no tienen ningún archivo en español. Toda la UI debe aguantar `dub == null` y `sub == null`.
- 13 archivos solo existen en 480p: usa siempre `Episode.resolveQuality()` para la calidad mostrada y reproducida.
- Imágenes: `OnePaceLinks.image(src, w)` usa `/_next/image`, que solo acepta `q=75` (otro valor da 400) y anchos 640/750/828/1080/1200/1920.
- Streaming: `https://pixeldrain.net/api/file/{id}` responde 206 `video/mp4` con rangos. Los subtítulos vienen quemados en el video. Plan B si falla: visor `pixeldrain.net/u/{id}` en Custom Tab.

## Gotchas aprendidos

- **Glance:** lo que va antes de `provideContent` solo corre al iniciar la sesión del widget. Lee el estado dentro de la composición (`collectAsState`/`produceState`). RemoteViews solo admite ciertas vistas: un `<View>` suelto rompe la vista previa (usa `ImageView`). Los bitmaps deben ser software (`allowHardware(false)`).
- **Persistencia del reproductor:** usa `container.scope` (no `viewModelScope`) para que se guarde la posición en `onCleared`.
- **Botón play/pausa:** refleja `playWhenReady`, no `isPlaying`, para no parpadear mientras carga.
- **Catálogo remoto:** un 4xx devuelve `null` (repo aún no publicado), no reintenta.
- **Edge-to-edge:** las barras del sistema siguen el tema de la app (`SystemBars` en `MainScreen.kt`).

## Probar en el teléfono (Pixel 8 Pro, Android 17)

- adb: `"$ANDROID_HOME/platform-tools/adb.exe"`. En Git Bash usa `export MSYS_NO_PATHCONV=1` antes de rutas como `/sdcard/...`.
- Mantén `adb shell svc power stayon usb` activo (el usuario lo pidió; no lo reviertas).
- Capturas: `adb exec-out screencap -p`, reducidas a 480 px de ancho con PIL. Coordenadas reales = coordenadas en la captura × 2.8.
- Los controles del reproductor se ocultan a los 3 s: el primer toque solo los muestra.
- En la pantalla de inicio del usuario hay widgets personales (calendario): **verifica con una captura en qué página estás antes de tocar**.
- **Probar avisos** (solo debug): sirve un `catalog.json` modificado con `python -I -m http.server 8765 --bind 127.0.0.1`, corre `adb reverse tcp:8765 tcp:8765`, compila con `-PcatalogUrl=http://127.0.0.1:8765/catalog.json` y dispara el worker con `adb shell am start -n com.myonepacelist.debug/com.myonepacelist.MainActivity --ez qa_sync true` (repite si la Activity se recreó). Al terminar: borra `files/catalog.json` con `run-as`, quita el reverse y reinstala sin `catalogUrl`.
