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
- **LinkedIn:** post publicado con 4 capturas (inicio, ajustes, reproductor, arcos) y el enlace al repo. Se rechazó la promoción de pago.
- **README** con galería de capturas (`docs/screenshots/`).
- **Ajustes del repo:** descripción y 7 *topics*; wiki apagada; escaneo de secretos y su bloqueo activos; alertas de Dependabot y actualizaciones de seguridad activas; reporte privado de vulnerabilidades activo; `main` protegida solo contra *force push* y borrado (sin exigir PR, para que la tarea de la PC suba directo; si alguna vez hay que reescribir historia, hay que relajar esa regla un momento). Archivos: `SECURITY.md`, `.github/dependabot.yml` (semanal, Gradle y Actions, menores/parches agrupados), plantilla de issue y `config.yml`.
- **Dependabot PR #1** (abierto, **sin fusionar**): AGP 9.4.1→9.5.0 y Gradle 9.8.1→9.8.2, CI en verde. Es cambio de herramientas de compilación: revisarlo y decidir. Los cambios mayores (AGP, Kotlin) llegan aparte y pueden romper la compilación; CI es el filtro.

## Catálogo automático

`onepace.net/es-419/watch` responde **HTTP 403** a las IP de GitHub Actions (Cloudflare); desde la PC del usuario responde 200 con el mismo User-Agent. **No evadir la protección** (User-Agent de navegador, proxies…). Por eso no hay workflow de catálogo (se borró; se recupera del historial si One Pace algún día lo permite).

- `tools/catalog/local-sync.ps1` corre en un clon dedicado (`%USERPROFILE%\myonepacelist-catalog-sync`, identidad git = correo noreply de GitHub, no el Gmail): `git pull --ff-only`, pruebas, `npm run sync`, y commit+push de `catalog.json` solo si cambió. Se detiene si hay otros cambios sin guardar. Log en `local-sync.log` del clon. Si falla muestra un aviso de Windows (`Alert`); el camino de fallo se probó (log + código 1), pero no se pudo ver el globo (la sesión de Claude no es interactiva).
- Tarea de Windows `MyOnePaceList catalogo` (creada por el usuario con `register-task.ps1`, carpeta temporal de la sesión): diaria a las 10:00, solo con sesión iniciada (`Interactive`, `Limited`, sin contraseña guardada), `StartWhenAvailable`, solo con red, límite 10 min. Probada: resultado `0`, "sin cambios" (38 arcos, 477 episodios). Quitarla: `Unregister-ScheduledTask -TaskName 'MyOnePaceList catalogo' -Confirm:$false`. Claude no puede crear tareas programadas (el modo automático lo bloquea): las crea el usuario.
- **Sin ejercitar aún:** el camino commit+push con un cambio real (`git push --dry-run` sí autentica) y la descarga de un catálogo nuevo en la app (hoy remoto = incluido).
- Solo funciona con la PC encendida y con sesión iniciada.
- **Aviso de seguridad no resuelto:** la tarea ejecuta cada día el código que baja del repo (el script y `tools/catalog`, sin dependencias npm) con las credenciales de Git Credential Manager (probablemente con acceso a todos sus repos). Solo el usuario escribe en el repo, pero no se pudo verificar su 2FA (la API devolvió `null`). Mitigaciones: activar 2FA, una *deploy key* limitada a este repo, o correr el script a mano.
- Su feed RSS (`/en/releases/rss.xml`) es de torrents y no sirve de reemplazo.

## Pendiente (lo hace el usuario)

1. **Copiar `signing/` a un lugar seguro** (USB / gestor de contraseñas). Lo único irrecuperable.
2. Comprobar que su cuenta de GitHub tiene **2FA**.
3. **Escribir al equipo de One Pace** para pedir permiso a GitHub Actions o una fuente oficial: Discord `discord.com/invite/pacing`, X `@OnePaceProject` o modmail de r/onepace (no hay correo publicado). El borrador del mensaje (en inglés) quedó solo en la conversación.
4. Decidir qué app usa en el teléfono (ver abajo), el PR #1 de Dependabot y la *deploy key*.
5. Opcional: imagen social del repo (solo se sube desde la web de GitHub); *attestation* de procedencia en `release.yml` (`actions/attest-build-provenance`) y un `v0.1.1`.

## Difusión (aplazada por el usuario)

- Reddit está **bloqueado** para el navegador de Claude (ni `navigate` ni `WebFetch`): el usuario publica a mano y Claude prepara el texto con las reglas que pegue.
- r/onepace (en inglés) permite apps alternativas solo si son de código abierto y sus releases las compila un pipeline público: **la app cumple**. No enlazar sitios no oficiales (rentry, buymeacoffee…); título sin spoilers; las capturas de inicio y reproductor muestran Isla de Drum y a Vivi (mejor usar ajustes y arcos). El texto de reglas que pegó el usuario salía **cortado a mitad de la regla 7**: puede faltar el final y más reglas.
- Esa comunidad es en inglés y el catálogo solo trae español latino: el post tendría que aclararlo en la primera línea. No se encontró una comunidad en español verificable. Cuentas nuevas: empezar comentando y no repetir el mismo post en varias a la vez.

## Estado del teléfono

- Instalada solo la debug (`com.myonepacelist.debug`, URL de catálogo correcta). El usuario desinstaló todo y restauró su progreso: 49 episodios, ya en la pantalla principal. Se perdieron el widget y los permisos de notificaciones: hay que volver a añadir el widget y activar los avisos.
- Respaldo en el formato de la app (18:05, 50 episodios, Isla de Drum 02 en 23:58): `/sdcard/Download/myonepacelist-respaldo-2026-11-03.json`.
- **Rendimiento (medido con `dumpsys gfxinfo`, pantalla de 120 Hz):** la debug va con tirones; la release es unas 4 veces mejor. Misma ruta de deslizamientos: debug 3.45 % de fotogramas atrasados (74/2144, p99 34 ms, 73 "hilo de UI lento") frente a release 0.84 % (15/1783, p99 23 ms, 10). Es el costo normal de Compose en debug, no un fallo de la app. Recomendado: usar la release en el día a día. Para pasarse: exportar en la debug (o usar el respaldo de Descargas), instalar el APK del Release, "Restaurar un respaldo", y luego quitar la debug. La release es una app aparte (otra firma): no comparte datos.
- Una desinstalación borra el progreso: antes de desinstalar la debug, exportarlo (Ajustes → Exportar).
- `stay_on_while_plugged_in = 2` (pedido por el usuario; se queda así).

## Ideas para después (no comprometidas)

- Picture-in-picture en el reproductor.
- Mostrar en el detalle del arco las versiones extendidas (`extended`, ya están en el catálogo).
- Usar en la UI la duración real del archivo cuando se conozca (el catálogo redondea; el 06 dice 25 min y dura 26:57).
- ✅ Capturas en `docs/screenshots/` y en el README (se tomaron en el Pixel con `adb screencap`, recortando la barra de estado).
