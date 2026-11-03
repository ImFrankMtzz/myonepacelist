# MyOnePaceList — tracker de One Pace para Android

## Contexto

Frank quiere una app Android minimalista, sin login, estilo "MyAnimeList" pero solo para **One Pace** (re-edición fan de One Piece, onepace.net). Debe: guardar en qué capítulo vas, abrir el capítulo directamente y elegir **doblaje latino** o **subtitulado**, verse muy bien, y quedar lista para un repositorio público en GitHub.

Decisiones tomadas con el usuario:
- **Distribución:** uso personal + GitHub (repo público, APK en GitHub Releases). Sin Google Play.
- **Reproducción:** reproductor **dentro de la app** (Media3/ExoPlayer) que recuerda el minuto exacto, con plan B automático a pestaña de navegador.
- **Catálogo:** JSON incluido en la app + auto-actualización generada por GitHub Action.
- **Extras:** "Continuar" + estadísticas, respaldo/exportar progreso, widget de pantalla de inicio, aviso de episodios nuevos.

Hallazgos de la investigación (onepace.net/es-419/watch):
- 14+ arcos; cada episodio tiene ancla (`#romance-dawn-01`, `#loguetown-01.5`), sinopsis, capítulos de manga/anime, duración e imagen.
- Por episodio hay enlaces Pixeldrain separados **"Subtítulos en español"** y **"Doblaje en español"** en 480/720/1080p (`pixeldrain.net/u/{id}`). Algunos marcados "obsoleto", "versión extendida" o sin doblaje.
- No hay API pública → el catálogo se obtiene leyendo el HTML.
- Pixeldrain: `GET /api/file/{id}` sirve el archivo con soporte de *byte ranges* (permite streaming y saltar). **Riesgo:** a usuarios gratuitos puede pedir captcha (`ip_download_limited_captcha_required`, `hotlink_detected`, límites de transferencia). Por eso el plan B (abrir `pixeldrain.net/u/{id}` en Custom Tab).

Entorno: JDK 21 instalado, `ANDROID_HOME` existe pero solo tiene `cmdline-tools` (faltan platform/build-tools). Proyecto previo `Code/Aniapp` usa Kotlin + Compose + Hilt + Room → mismo stack, patrones conocidos. Teléfono de pruebas: Pixel 8 Pro. `gh` no está instalado.

Nombre: **MyOnePaceList** (paquete `com.myonepacelist`, enlace profundo `myonepacelist://`).

## Diseño

### Stack
Kotlin + Jetpack Compose (Material 3), un solo módulo `app`, MVVM. Hilt (DI), Room (progreso), DataStore (preferencias), kotlinx.serialization (catálogo), Media3 ExoPlayer + UI (reproductor), Coil 3 (imágenes), WorkManager (actualización/avisos), Glance (widget), Navigation Compose con rutas tipadas, Custom Tabs (plan B). minSdk 26, target/compileSdk 35. Versiones estables fijadas en `gradle/libs.versions.toml` (tomar como base el de `Code/Aniapp/gradle/libs.versions.toml` y actualizar).

### Ubicación del repo
Repositorio git local (rama `main`), publicado en GitHub como `ImFrankMtzz/myonepacelist`.

### Catálogo (datos)
- `tools/catalog/` — script Node (`cheerio`) que lee `https://onepace.net/es-419/watch` y genera `catalog.json`:
  ```
  { schema:1, version:<hash>, generatedAt, arcs:[{ slug, title, description, mangaChapters, animeEpisodes, image,
      episodes:[{ id:"romance-dawn-01", arcSlug, number:"01", title, synopsis, mangaChapters, animeEpisodes,
                  durationMin, image, extended:bool,
                  sources:{ sub:{ "480":id, "720":id, "1080":id }, dub:{...} | null } }] }] }
  ```
  Se ignoran enlaces "obsoleto"; las "versiones extendidas" se guardan como variante del episodio.
- Copia incluida en `app/src/main/assets/catalog.json` (funciona sin internet).
- `.github/workflows/catalog.yml`: cron semanal + manual → corre el script, valida el JSON (esquema + nº de episodios ≥ anterior) y hace commit solo si cambió. Así se lee onepace.net **una vez por semana desde GitHub**, no desde cada teléfono.
- App: `CatalogRepository` carga assets → reemplaza con el JSON remoto cacheado (`raw.githubusercontent.com/<user>/MyOnePaceList/main/catalog.json`) si es válido y más nuevo.

### Progreso (sin login)
- Room `episode_progress(episodeId PK, status NONE|IN_PROGRESS|WATCHED, positionMs, durationMs, language SUB|DUB, updatedAt, watchedAt)`.
- DataStore: idioma preferido (default DUB), calidad (default 720p), tema (sistema/oscuro/claro), avisos on/off, versión de catálogo conocida.
- Regla: se marca VISTO al pasar 90 % o manualmente. "Marcar hasta aquí" = marca todos los anteriores como vistos (clave para decir dónde te quedaste).

### Lógica de dominio (funciones puras, testeables)
- `nextEpisode(catalog, progress)`: episodio en progreso más reciente; si no hay, el primero no visto después del último visto.
- `stats(...)`: episodios vistos/total, % global y por arco, horas vistas, tiempo restante.
- `markUpTo(episodeId)`, `catalogDiff(old,new)` → episodios nuevos.
- `Backup` export/import JSON versionado (`{schema, exportedAt, progress[], prefs}`), merge por `updatedAt`.

### Pantallas (navegación inferior de 3 pestañas + ajustes en la barra superior)
1. **Bienvenida (solo primera vez):** "¿Dónde te quedaste?" → elegir arco → episodio → idioma preferido. Opción "Empiezo desde cero".
2. **Inicio:** tarjeta grande **Continuar** (imagen, arco, nº, título, barra de minuto, botones `Doblaje` / `Subtítulos`); progreso del arco actual; vistos recientemente.
3. **Arcos:** lista de arcos con imagen y anillo de progreso → **Detalle de arco**: cabecera (transición compartida desde la tarjeta) + lista de episodios con check, duración, estado y menú (marcar visto, marcar hasta aquí, reiniciar).
4. **Detalle de episodio (hoja inferior):** sinopsis, capítulos manga/anime, selector de idioma (chips con texto + ícono, no solo color; deshabilitado con "Sin doblaje aún" si no existe), calidad, **Reproducir** y "Abrir en navegador".
5. **Reproductor:** pantalla completa horizontal, doble toque ±10 s, guardar posición cada 5 s y al pausar/salir, "¿Reanudar en 12:34?", al terminar "Siguiente episodio" con cuenta regresiva cancelable. Error de Pixeldrain (HTTP 403/429 o captcha) → hoja explicativa + botón que abre `pixeldrain.net/u/{id}` en Custom Tab.
6. **Estadísticas:** totales, horas, restante, barras por arco (con números, no solo color).
7. **Ajustes:** idioma/calidad por defecto, tema, avisos, exportar/importar progreso (selector de archivos del sistema), actualizar catálogo ahora, acerca de + aviso legal.

Enlace profundo `myonepacelist://episode/{id}?lang=dub|sub` (usado por widget y notificaciones).

### Segundo plano
- `CatalogSyncWorker` (WorkManager, cada 24 h, requiere red): descarga el catálogo remoto, calcula diff y lanza notificación "Nuevo episodio: Egghead 12" (canal propio; pide `POST_NOTIFICATIONS` en Android 13+ solo al activar avisos).
- **Widget Glance** (2×2 y 4×2): siguiente episodio, arco, barra de progreso del arco; toque → reproductor en idioma preferido. Se actualiza al cambiar el progreso.

### Dirección visual (ui-ux-pro-max: minimalismo Swiss, variance 3, density 4)
- **Oscuro primero** ("altamar de noche"): fondo `#0E1116`, superficie `#161B22`, superficie-2 `#1E2530`, texto `#E8ECF1`, secundario `#9AA4B2` (≥4.5:1), acento **ámbar sombrero de paja** `#F4B740` (texto encima `#1A1205`), error `#F2555A`.
- **Claro** ("pergamino"): fondo `#FAF8F3`, superficie `#FFFFFF`, texto `#14181F`, secundario `#5A6472`, acento `#9A6700` (para contraste AA).
- Tokens semánticos en `ui/theme` (nunca hex en pantallas); sin color dinámico, para mantener identidad.
- Tipografía: **Space Grotesk** (títulos, números) + **Inter** (cuerpo), incluidas en `res/font` (OFL); cifras tabulares en contadores/tiempos; escala M3 y soporte de tamaño de texto del sistema.
- Íconos: Material Symbols Rounded, un solo estilo; sin emojis. Objetivos táctiles ≥48 dp, espaciado 4/8 dp.
- Movimiento: transición compartida arco→detalle, barras animadas, check con háptica; respetar "quitar animaciones" del sistema. Esqueletos (shimmer) mientras cargan imágenes.
- Antes de programar las pantallas: **maqueta HTML navegable (Artifact)** de Inicio, Arcos, Detalle y Reproductor para que el usuario la apruebe.

### Legal / README
Proyecto fan no afiliado a One Pace, Toei ni Shueisha; no aloja video, solo enlaza los archivos públicos que publica onepace.net; créditos a One Pace. Sin analítica ni rastreo.

## Plan de implementación (fases)

**Fase 0 — Verificación (spike, desechable).** `curl -I -H "Range: bytes=0-1023" https://pixeldrain.net/api/file/<id>` para un episodio doblado y uno subtitulado: ver `Content-Type`, `206`, errores de hotlink/captcha y si los subtítulos vienen quemados (mp4) o como pista (mkv/ASS). Si el streaming directo se bloquea siempre → el reproductor queda como opción y el Custom Tab pasa a ser el camino principal (avisar al usuario).

**Fase 1 — Base.** Crear repo, escribir este diseño en `docs/superpowers/specs/2026-11-03-myonepacelist-design.md` y hacer commit. Instalar con `sdkmanager` `platforms;android-35`, `build-tools;35.0.0`, `platform-tools` (con permiso). Esqueleto Gradle (wrapper, version catalog, Hilt, Compose), tema + tipografías, navegación vacía, `.gitignore` (local.properties, keystores).

**Fase 2 — Catálogo.** Script `tools/catalog` + fixture HTML guardado + tests del parser; `catalog.json` inicial; modelos Kotlin + `CatalogRepository` (assets + remoto) + tests.

**Fase 3 — Progreso y dominio (TDD).** Room + DAO, DataStore, `nextEpisode`, `stats`, `markUpTo`, `catalogDiff`, backup round-trip.

**Fase 4 — Maqueta visual.** Artifact HTML de las pantallas clave → aprobación del usuario.

**Fase 5 — Pantallas.** Bienvenida, Inicio, Arcos, Detalle de arco, hoja de episodio, Estadísticas, Ajustes (export/import).

**Fase 6 — Reproductor.** Media3 con fuente `api/file/{id}`, guardado de posición, reanudar, siguiente episodio, plan B a Custom Tab.

**Fase 7 — Segundo plano.** `CatalogSyncWorker` + notificaciones, widget Glance, enlaces profundos.

**Fase 8 — Publicación.** README (capturas, instalación, aviso legal), LICENSE (MIT), workflow `catalog.yml`, workflow `release.yml` (al crear un tag `v*` compila un APK firmado y lo sube a Releases; keystore en *secrets*, nunca en el repo). Crear el repo en GitHub y hacer push **solo tras confirmación** (instalar `gh` o que el usuario cree el repo).

Ejecución recomendada: subagentes por fase con revisión entre fases (superpowers:subagent-driven-development); el plan detallado tarea por tarea se genera con writing-plans al aprobar.

## Archivos clave
- `app/src/main/java/.../data/catalog/` (CatalogRepository, modelos), `data/progress/` (Room, DAO), `data/prefs/`
- `app/src/main/java/.../domain/` (NextEpisode, Stats, MarkUpTo, CatalogDiff, Backup)
- `app/src/main/java/.../ui/{theme,home,arcs,episode,player,stats,settings,onboarding}/`
- `app/src/main/java/.../work/CatalogSyncWorker.kt`, `widget/NextEpisodeWidget.kt`
- `app/src/main/assets/catalog.json`, `tools/catalog/`, `.github/workflows/{catalog,release}.yml`

## Verificación
- `tools/catalog`: `npm test` (parser contra fixture) + correr contra el sitio real y revisar nº de arcos/episodios y enlaces dub/sub.
- `./gradlew testDebugUnitTest` (dominio, parser JSON, backup, diff) y `./gradlew assembleDebug lint`.
- Instalar en el Pixel 8 Pro con `adb install`; probar a mano: bienvenida "me quedé en Arlong Park 03", Continuar, reproducir doblaje y subtítulos, salir/reanudar en el minuto correcto, marcar visto al 90 %, plan B forzando un ID inválido, exportar → borrar datos → importar, widget, y aviso forzando el worker (`adb shell cmd jobscheduler run` / quitar un episodio del catálogo local).
- Revisión con la checklist de ui-ux-pro-max: contraste en ambos temas, objetivos ≥48 dp, texto grande del sistema, animaciones reducidas, TalkBack en controles de ícono.
