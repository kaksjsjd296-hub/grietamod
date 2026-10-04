# Grieta Mod (Forge 1.20.1)

Mod para **Minecraft 1.20.1 / Forge 47.x** con dos items (pestaña creativa **"Grieta Mod"**):

| Item | Id | Qué hace |
|---|---|---|
| **Grieta** | `grietamod:grieta` | Click derecho: el cielo se vuelve rojo con remolinos y se abre una **grieta gigante** con borde rosa y galaxia dentro. Todos los jugadores la ven. Click derecho otra vez: se cierra con animación. |
| **Grieta Configuradora** (con tuerca) | `grietamod:grieta_configuradora` | Click derecho: abre un menú para **subir tu imagen**; esa imagen se muestra dentro de la grieta. Solo configura, no activa nada. |

```
/give @s grietamod:grieta
/give @s grietamod:grieta_configuradora
```

## Cómo se usa
1. Ten el mod en el **servidor y en todos los clientes** (o en tu mundo si juegas solo/LAN).
2. Con la **Grieta Configuradora** en la mano, click derecho → **Elegir imagen...** (o arrastra un archivo a la ventana) → **Subir y aplicar**.
   Acepta PNG, JPG, GIF y BMP. Se reduce automáticamente a máx. 512 px.
3. Con la **Grieta**, click derecho → se abre. Click derecho otra vez → se cierra.

La imagen se recorta para llenar la grieta sin deformarse. Si quieres que se vea completa, usa una imagen con proporción ~3:5 (vertical).

## Configuración
`config/grietamod-common.toml` → `autoCloseSeconds` (0 = no se cierra sola).

## Cómo obtener el .jar en GitHub
1. Crea un repositorio nuevo en GitHub y sube **todo el contenido de esta carpeta** (incluida `.github/`).
2. Ve a la pestaña **Actions** → se ejecuta **Build Mod** (tarda ~5-10 min la primera vez).
3. Entra a la ejecución terminada → **Artifacts** → descarga `grietamod-jar` (dentro está el `.jar`).
4. Para publicarlo en **Releases**: crea una etiqueta, por ejemplo `v1.0.0`
   (`git tag v1.0.0 && git push origin v1.0.0`) y el .jar aparece en la sección *Releases*.

Coloca el `.jar` en la carpeta `mods/`.

## Compilar en tu PC (opcional)
Necesitas JDK 17 y Gradle 8.8: `gradle build` → el .jar queda en `build/libs/`.
