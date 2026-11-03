# HANDOFF — MyOnePaceList

_Actualizado: 2026-11-03 (tarde)._

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

## Pendiente (requiere al usuario)

1. ✅ Usuario de GitHub: `githubUser=ImFrankMtzz`; repo público `github.com/ImFrankMtzz/myonepacelist` (en minúsculas, también en la URL del catálogo).
2. ✅ Repo creado por el usuario y push hecho (2026-11-03).
3. **Clave de firma para releases:** `keytool` (comando en el README), guardarla fuera del repo y cargar los 4 secrets (`SIGNING_*`). Luego subir la etiqueta `v0.1.0` (con confirmación).
4. Tras el primer push: correr a mano el workflow **Catálogo** para comprobar que hace commit del `catalog.json`.

## Estado del teléfono

- Instalado `com.myonepacelist.debug` con **progreso de prueba** (Arlong Park, siguiente: 07). El usuario puede borrarlo en Ajustes.
- `/sdcard/Download/myonepacelist-2026-11-03.json`: respaldo de ese progreso de prueba.
- Widget en una página nueva de la pantalla de inicio (a la derecha de la página de widgets).
- `stay_on_while_plugged_in = 2` (pedido por el usuario; se queda así).

## Ideas para después (no comprometidas)

- Picture-in-picture en el reproductor.
- Mostrar en el detalle del arco las versiones extendidas (`extended`, ya están en el catálogo).
- Usar en la UI la duración real del archivo cuando se conozca (el catálogo redondea; el 06 dice 25 min y dura 26:57).
- Capturas de pantalla para el README cuando el repo sea público.
