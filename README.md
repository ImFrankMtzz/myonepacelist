# MyOnePaceList

App Android minimalista para llevar tu avance de **[One Pace](https://onepace.net/es-419)**, la re-edición sin relleno de One Piece hecha por fans, y verla con **doblaje latino** o **subtítulos en español**.

Es como una lista estilo MyAnimeList, pero enfocada solo en One Pace: sin cuentas, sin anuncios y sin rastreo. Tu progreso vive en tu teléfono.

## Qué hace

- **¿Dónde te quedaste?**: en el primer uso eliges arco y episodio, y la app marca como vistos todos los anteriores.
- **Continuar**: un toque y retomas el episodio en el minuto exacto donde lo dejaste.
- **Doblaje latino o subtítulos** por episodio, en 480p, 720p o 1080p. Si un episodio aún no tiene doblaje, la app lo indica y usa los subtítulos.
- **Reproductor integrado** (Media3). Guarda tu posición cada 5 segundos, marca el episodio como visto al pasar el 90 % y pasa solo al siguiente. Si Pixeldrain pide verificación, ofrece abrir el episodio en el navegador.
- **Reproductor externo** (opcional): abre los episodios en VLC, MX Player, mpv u otra app de video. Si tu reproductor avisa en qué minuto te quedaste (VLC, MX Player y mpv lo hacen), la app lo guarda.
- **No reproducir con datos móviles** (opcional): con datos te pregunta antes de empezar, y si a media reproducción pasas del Wi-Fi a datos, pausa el video.
- **Arcos y episodios** con el avance de cada arco, filtros (pendientes, con doblaje) y "marcar visto hasta aquí".
- **Estadísticas**: porcentaje completado, horas vistas y por delante, y avance por arco.
- **Widget** de pantalla de inicio con tu siguiente episodio sobre la imagen del arco en curso; se añade desde Ajustes.
- **Avisos** cuando One Pace publica episodios nuevos o doblaje nuevo (opcional).
- **Respaldo**: exporta e importa tu progreso como archivo JSON para cambiar de teléfono.
- Tema oscuro y claro.

## Instalación

Descarga el APK más reciente desde [Releases](../../releases) e instálalo en tu Android (8.0 o superior). Android te pedirá permitir la instalación desde tu navegador o gestor de archivos.

## Cómo se mantiene al día

```
onepace.net ──(GitHub Action diaria)──> catalog.json en este repo ──(la app, 1 vez al día)──> tu teléfono
```

- `tools/catalog/` lee los datos de la página de One Pace (el payload de Next.js) y genera `catalog.json`: arcos, episodios, sinopsis y los IDs de Pixeldrain para subtítulos y doblaje en cada calidad.
- `.github/workflows/catalog.yml` lo corre cada día y hace commit solo si algo cambió. Así onepace.net recibe una visita al día, no una por cada teléfono.
- La app trae una copia del catálogo, así que funciona sin internet, y descarga la versión nueva desde este repositorio.

## Seguridad y privacidad

- Sin cuentas, sin anuncios, sin analíticas. Los únicos permisos son internet, estado de la red (para el ajuste de datos móviles) y notificaciones (opcional).
- El catálogo descargado se valida antes de usarse: IDs de Pixeldrain, rutas de imagen y slugs que no tengan el formato que publica One Pace se descartan, tanto en la Action que lo genera como en la app.
- Los respaldos importados se validan (tamaño, formato y valores) y lo que reporta un reproductor externo se acota a valores razonables.
- La versión release no permite tráfico sin cifrar. Las Actions están fijadas por SHA y el APK de cada Release lleva su `SHA256SUMS.txt`.

## Desarrollo

Requisitos: JDK 17+ (probado con 21), Android SDK y Node 20+ para el script del catálogo.

```bash
# App
./gradlew testDebugUnitTest assembleDebug     # pruebas + APK debug
./gradlew installDebug                        # instalar en un teléfono conectado por USB

# Catálogo
cd tools/catalog
npm test                                      # pruebas del lector
npm run sync                                  # regenerar catalog.json desde onepace.net
```

En `gradle.properties`, `githubUser` indica de qué repositorio descarga la app el catálogo. En CI se toma solo del dueño del repo.

### Estructura

```
app/src/main/java/com/myonepacelist/
├─ data/catalog/   modelos del catálogo y repositorio (incluido + remoto)
├─ data/progress/  progreso y ajustes (DataStore, un solo JSON)
├─ domain/         lógica pura: siguiente episodio, estadísticas, respaldo (con pruebas)
├─ ui/             pantallas Compose y tema
├─ player/         reproductor Media3
├─ work/           revisión diaria del catálogo y notificaciones
└─ widget/         widget Glance
```

Stack: Kotlin, Jetpack Compose (Material 3), Navigation, DataStore, kotlinx.serialization, Media3 ExoPlayer, Coil, WorkManager y Glance.

### Publicar una versión

1. Crea una clave de firma una sola vez y guárdala fuera del repo: `keytool -genkeypair -v -keystore release.jks -alias myonepacelist -keyalg RSA -keysize 4096 -validity 10000`
2. En *Settings → Secrets and variables → Actions* agrega `SIGNING_KEYSTORE_BASE64` (el `.jks` en base64), `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` y `SIGNING_KEY_PASSWORD`.
3. Sube `versionCode`/`versionName` en `app/build.gradle.kts`, crea un tag `vX.Y.Z` y súbelo. El workflow `release.yml` adjunta el APK al Release.

> Guarda bien esa clave: sin ella no podrás actualizar la app ya instalada.

## Aviso legal

MyOnePaceList es un proyecto de fans, sin afiliación con One Pace, Toei Animation ni Shueisha. No aloja ni distribuye video: reproduce los archivos que el equipo de One Pace publica en onepace.net y que sirve Pixeldrain. Todo el crédito de las ediciones es del [equipo de One Pace](https://onepace.net/es-419/about). One Piece © Eiichiro Oda / Shueisha, Toei Animation.

## Licencia

Código bajo licencia [MIT](LICENSE). Tipografías Inter y Space Grotesk bajo la [SIL Open Font License](licenses/).
