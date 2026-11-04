# HANDOFF — MyOnePaceList

_Actualizado: 2026-11-03 (noche)._

## Objetivo

App Android minimalista, estilo MyAnimeList pero solo para One Pace: guardar en qué episodio vas, reproducirlo dentro de la app con doblaje latino o subtítulos y publicarla en un repo público de GitHub (APK en Releases, sin Google Play).

## Estado: completa y probada en el dispositivo

Todo lo del diseño está implementado (`docs/superpowers/specs/2026-11-03-myonepacelist-design.md`) y verificado en el Pixel 8 Pro del usuario (Android 17):

| Función | Resultado en el teléfono |
|---|---|
| Bienvenida "¿Dónde te quedaste?" | ✅ marca vistos los anteriores (Arlong Park 04 → 26 episodios) |
| Reproducir con doblaje latino | ✅ Pixeldrain transmite dentro de la app |
| Reproducir con subtítulos | ✅ subtítulos en español quemados en el video |
| Retomar en el minuto | ✅ 12:32 → "Reanudar en 12:32" → arranca en 12:32 |
| Fin de episodio | ✅ marca visto y pasa solo al siguiente a los 8 s |
| Progreso persistente | ✅ sobrevive a un cierre forzado |
| Arcos y detalle de arco | ✅ filtros, checks, "Quedaste en 0:31" |
| Estadísticas | ✅ cuadran |
| Tema oscuro y claro | ✅ incluidas las barras del sistema |
| Exportar, borrar e importar | ✅ 29 episodios recuperados; también desde la bienvenida |
| Aviso de episodios nuevos | ✅ con catálogo de prueba local; tocarlo abre el episodio |
| Widget | ✅ fondo con la imagen del arco actual, se actualiza en vivo y abre el reproductor |
| Reproductor externo (VLC) | ✅ selector de Android → VLC arranca en el minuto guardado y al cerrar devuelve la posición (24:14 guardado) |
| No reproducir con datos | ✅ con red medida pregunta antes; "Reproducir igual" abre el reproductor; si la red pasa a medida a media reproducción, pausa y ofrece "Seguir con datos"; al volver el Wi-Fi quita el aviso |

Verificación automática: `testDebugUnitTest` (32 pruebas) y `tools/catalog` (7 pruebas) pasan, `lintDebug` da 0 errores, y debug y release (R8, ~4 MB) compilan.

## Auditoría de seguridad (2026-11-03)

Corregido: el catálogo remoto se sanea (`CatalogGuard`) y se limita a 5 MB; el generador rechaza IDs, slugs o imágenes raros; los respaldos importados se limitan a 2 MB y se sanean; los resultados del reproductor externo se acotan; Media3 ya no sigue redirecciones https→http; las Actions están fijadas por SHA, CI corre con `contents: read`, `persist-credentials: false`, sin `${{ }}` dentro de `run`, y el release se compila sin caché, borra el keystore y publica `SHA256SUMS.txt`.
Sin hallazgos: secretos en el historial, claves en el repo, logs, WebView, componentes exportados de más, cleartext en release, PendingIntent mutables.
Los commits usan el correo noreply de GitHub del usuario (el Gmail se reescribió antes del primer push).

## Publicado (2026-11-03)

- **Repo público:** https://github.com/ImFrankMtzz/myonepacelist (`githubUser=ImFrankMtzz`; el nombre va en minúsculas, también en la URL del catálogo). CI en verde.
- **Release v0.1.0:** https://github.com/ImFrankMtzz/myonepacelist/releases/tag/v0.1.0. APK firmado (cert SHA-256 `a4753e76…786f`, `CN=MyOnePaceList`) + `SHA256SUMS.txt`, verificados tras descargarlos. Secrets `SIGNING_*` cargados en el repo.
- **Clave de firma:** solo en `signing/` (fuera de git; verificado que nunca entró al historial ni al remoto). Si se pierde no hay actualizaciones que se instalen encima.
- **LinkedIn:** post publicado con 4 capturas (inicio, ajustes, reproductor, arcos) y el enlace al repo.
- **README** con galería de capturas (`docs/screenshots/`).

## Pendiente

1. **Copiar `signing/` a un lugar seguro** (USB / gestor de contraseñas). Lo único irrecuperable.
2. **Catálogo automático (resuelto a medias):** `onepace.net/es-419/watch` responde **HTTP 403** a las IP de GitHub Actions (Cloudflare); desde la PC del usuario responde 200 con el mismo User-Agent (run de prueba: https://github.com/ImFrankMtzz/myonepacelist/actions/runs/41738757731). **No intentar evadir la protección** (User-Agent de navegador, proxies…).
   - Hecho: el workflow `catalog.yml` se **borró** (solo habría fallado con el 403; se recupera del historial de git si One Pace algún día permite a GitHub) y se añadió `tools/catalog/local-sync.ps1`, que en un clon dedicado hace `git pull`, corre las pruebas y `npm run sync`, y hace commit+push de `catalog.json` solo si cambió (se detiene si hay otros cambios sin guardar). Probado: lee onepace.net bien y responde "sin cambios" (38 arcos, 477 episodios); el camino commit+push aún no se ha ejercitado porque no hubo cambios (el simulacro `git push --dry-run` sí autentica).
   - Clon dedicado: `%USERPROFILE%\myonepacelist-catalog-sync` (identidad git = correo noreply de GitHub, no el Gmail). Log en `local-sync.log` dentro del clon.
   - ✅ **Tarea programada creada por el usuario** (2026-11-03): `MyOnePaceList catalogo`, diaria a las 10:00, solo con la sesión iniciada (`Interactive`, `Limited`, sin contraseña guardada), `StartWhenAvailable` y solo con red, límite de 10 min. Claude no pudo crearla (el modo automático bloquea crear tareas programadas), así que la creó el usuario con `register-task.ps1` (carpeta temporal de la sesión; si ya no está, es esa misma configuración con `powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File <clon>\tools\catalog\local-sync.ps1`). Probada: `Start-ScheduledTask` → resultado `0` y log "sin cambios". Quitarla: `Unregister-ScheduledTask -TaskName 'MyOnePaceList catalogo' -Confirm:$false`.
   - **Aviso de seguridad no resuelto:** la tarea ejecuta cada día el código que baja del repo (el script y `tools/catalog`, que no tiene dependencias npm) con las credenciales de git guardadas (Git Credential Manager, probablemente con acceso a todos sus repos). Solo el usuario puede escribir en el repo, pero no se pudo verificar si su cuenta de GitHub tiene 2FA (la API devolvió `null`): conviene activarlo. Mitigación pendiente: una *deploy key* limitada a este repo, o correr el script a mano.
   - Nota: en PowerShell el `!` no es un prefijo (solo lo es en el prompt de Claude Code). Y los perfiles de PowerShell del usuario no cargan porque la política de ejecución es la de fábrica: es inofensivo, solo hace que `claude` no pase por Headroom en una ventana normal (arreglo opcional: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`).
   - Solo funciona con la PC encendida y con sesión iniciada. Pendiente a más largo plazo: escribir al equipo de One Pace (Discord `discord.com/invite/pacing`, X `@OnePaceProject` o modmail de r/onepace; no hay correo publicado) para pedir permiso para GitHub Actions o una fuente oficial; el borrador del mensaje quedó en la conversación. Su feed RSS (`/en/releases/rss.xml`) es de torrents y no sirve de reemplazo.
3. **Difusión en comunidades en español** (aplazado por el usuario). Lo que se averiguó:
   - Reddit está **bloqueado** para el navegador de Claude (ni `navigate` ni `WebFetch`): el usuario publica a mano y Claude prepara el texto con las reglas que pegue.
   - r/onepace (en inglés) permite apps alternativas solo si son de código abierto y sus releases las compila un pipeline público: **la app cumple** (MIT + GitHub Actions + `SHA256SUMS.txt`). No enlazar sitios no oficiales (rentry, buymeacoffee…); título sin spoilers; las capturas de inicio y reproductor muestran Isla de Drum y a Vivi, mejor usar ajustes y arcos. El texto de las reglas que pegó el usuario salía cortado a mitad de la regla 7: faltaba el final y quizá más reglas.
   - Esa comunidad es en inglés y el catálogo solo trae español latino: el post tendría que aclararlo en la primera línea (borrador bilingüe en la conversación; si se retoma, rehacerlo).
   - Cuentas nuevas: muchas comunidades las filtran por karma/antigüedad; empezar comentando y no repetir el mismo post en varias a la vez.
4. Opcional: *attestation* de procedencia en `release.yml` (`actions/attest-build-provenance`) y un `v0.1.1`, para reforzar "cada build rastreable al código". Solo con confirmación.
5. Opcional: *topics* de GitHub (`one-piece`, `onepace`, `android`, `kotlin`) con `gh` (ya inició sesión como ImFrankMtzz).

## Estado del teléfono

- Reinstalado el 2026-11-03 18:07 (`adb install -r`, progreso y ajustes intactos) con la URL de catálogo correcta (`ImFrankMtzz/myonepacelist`); la instalación anterior apuntaba a `TU_USUARIO_GITHUB` y nunca bajaba el catálogo remoto.
- Instalado `com.myonepacelist.debug` con el **progreso real del usuario** (Isla de Drum 02, ~24 min) y los ajustes que él eligió (incluye "No reproducir con datos móviles" activado). No restaurar respaldos viejos encima: avanzó entre pruebas.
- `/sdcard/Download/myonepacelist-2026-11-03.json`: respaldo viejo de progreso de prueba; se puede borrar.
- El Release v0.1.0 (`com.myonepacelist`, sin `.debug`) **sí está instalado** desde el 2026-11-03 18:08 (APK descargado de GitHub, checksum y firma verificados). Es una app aparte y **sin progreso** (muestra la bienvenida): el progreso real sigue en la debug. Para migrarlo: exportar en la debug y "Restaurar un respaldo" en la release. Ojo: con las dos instaladas hay dos widgets y los avisos se duplican.
- Widget en una página nueva de la pantalla de inicio (a la derecha de la página de widgets).
- `stay_on_while_plugged_in = 2` (pedido por el usuario; se queda así).
- La carpeta de pruebas de adb (`scratchpad`) tiene respaldos del estado; no están en el repo.

## Ideas para después (no comprometidas)

- Picture-in-picture en el reproductor.
- Mostrar en el detalle del arco las versiones extendidas (`extended`, ya están en el catálogo).
- Usar en la UI la duración real del archivo cuando se conozca (el catálogo redondea; el 06 dice 25 min y dura 26:57).
- ✅ Capturas en `docs/screenshots/` y en el README (se tomaron en el Pixel con `adb screencap`, recortando la barra de estado).
