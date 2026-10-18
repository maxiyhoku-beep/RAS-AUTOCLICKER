# RAS// AUTO CLICKER

Auto clicker para Smoq Games 27 (Android).

## Cómo compilar el APK desde GitHub (todo desde el teléfono)

1. Creá un repositorio **público** en github.com, por ejemplo `ras-autoclicker`.
   (Público = minutos de GitHub Actions gratis e ilimitados.)
2. Descomprimí este ZIP con ZArchiver.
3. En el repo: **Add file → Upload files**.
   **Subí el CONTENIDO de la carpeta, no la carpeta.** O sea, al repo tienen que
   llegar `build.gradle`, `settings.gradle`, `app/` y `.github/` en la raíz.
   Verificá: en la página del repo tenés que ver esos archivos directamente.
4. **Commit changes.**
5. Andá a la pestaña **Actions**. Se dispara sola la ejecución "Build APK".
   Tarda 3 a 5 minutos.
6. Entrá a esa ejecución → sección **Artifacts** → descargá `app-debug`.
7. Extraelo con ZArchiver: adentro está **`app-debug.apk`**.
8. Instalalo (aceptá "instalar apps de origen desconocido" si te lo pide).

## Primer uso en el teléfono

1. **ACTIVAR ACCESIBILIDAD** → busca "RAS// AUTO CLICKER" en la lista y activalo.
   Esto es obligatorio para que la app pueda hacer los toques.
2. **PERMISO DE SUPERPOSICIÓN** → activalo, es para el panel flotante.
3. **QUITAR RESTRICCIONES DE BATERÍA** → en Samsung poné la app en **"Sin
   restricciones"**. Este paso es EL que evita que el clicker se frene solo.
4. Elegí idioma con las banderas (🇺🇸 English / 🇦🇷 Español).

## El panel

- **En reposo:** INICIAR · SELL ALL · SELL MULTI · OCULTAR PUNTEROS · CERRAR PANEL
- **Trabajando:** PAUSA · OCULTAR PUNTEROS · CERRAR PANEL + contador de tiempo
  y de sobres abiertos.

SELL ALL y SELL MULTI son excluyentes: al activar uno se apaga el otro.
Si ninguno está activo, el puntero de acción cae sobre **SAVE ALL**.

Cerrar el panel o cerrar la app **no quita ningún permiso**: el servicio de
accesibilidad queda activo a nivel sistema.

## Los punteros

| Puntero | Dónde | Dispara cuando |
|---|---|---|
| Acción (SAVE ALL / SELL ALL / SELL MULTI) | Pantalla de cartas | Detecta la forma de un botón claro |
| NEXT PACK | Pantalla del sobre | Detecta la forma del botón |
| BONUS | Cada cierta cantidad de sobres | Toca un botón; si el panel sigue ahí, toca el otro |

Ningún puntero lee texto: todos miran **forma y posición**, así que funciona
igual en cualquier idioma.

## Si en tu teléfono no detecta bien los botones

Las posiciones se guardan como fracción de la pantalla en
`ClickerPrefs.kt` (valores por defecto calibrados sobre una pantalla
1080x2340). Si tu teléfono tiene otra relación de aspecto, ajustá ahí los
valores `saveAllX/Y`, `sellAllX/Y`, `sellMultiX/Y`, `nextPackX/Y` y
`bonusLeftX/bonusRightX/bonusY`.

## Notas

- Requiere Android 8.0 (API 26) o superior.
- La detección por forma necesita permiso de captura de pantalla. Si no lo
  das (o si el juego lo bloquea), el clicker sigue funcionando por tiempos.
- **BÚSQUEDA ESPECÍFICA** permite cargar hasta 3 capturas y avisa con la
  notificación "HAZ CONSEGUIDO TU CARTA ESPECÍFICA".
