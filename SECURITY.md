# Política de seguridad

MyOnePaceList es un proyecto personal y de código abierto. No tiene cuentas, anuncios ni rastreo, y todo se guarda en tu teléfono.

## Versiones con soporte

Solo la **última versión publicada** en [Releases](../../releases). Las versiones anteriores no reciben correcciones: actualiza a la más reciente.

## Cómo reportar una vulnerabilidad

**Por favor no abras un *issue* público.** Usa el reporte privado de GitHub:

1. Ve a la pestaña **Security** de este repositorio.
2. Pulsa **Report a vulnerability**.
3. Describe qué pasa, cómo reproducirlo y qué impacto tiene.

Solo lo ve quien mantiene el proyecto. Es un proyecto de una sola persona: respondo en cuanto puedo, normalmente en unos días, y avisaré cuando haya corrección.

## Qué interesa especialmente

Lo que entra a la app desde fuera es lo más delicado, y ya se valida, pero un hallazgo ahí es bienvenido:

- El **catálogo** que se descarga de este repositorio (IDs de Pixeldrain, rutas de imagen, nombres).
- Los **respaldos** de progreso que se importan desde un archivo.
- Lo que devuelve un **reproductor externo** (VLC, MX Player, mpv).
- El **APK de los Releases**: cada uno trae un `SHA256SUMS.txt` y está firmado con el certificado `CN=MyOnePaceList`. Si un APK no coincide con su suma, avísame.

## Fuera de alcance

- Los servicios de terceros: `onepace.net` y Pixeldrain. Reporta esos problemas a sus responsables.
- Problemas que exijan un teléfono ya comprometido o con el modo desarrollador y la depuración USB abiertos.
- La versión *debug* (`com.myonepacelist.debug`): es solo para desarrollo.

## Si instalas el APK

Descárgalo solo desde [Releases](../../releases) de este repositorio. Las versiones de otros sitios no son mías y no puedo asegurar su contenido.
