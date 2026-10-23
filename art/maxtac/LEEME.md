# MaxTac — prototipo equipable

Conjunto original para Apogee, revisado, guardado y exportado en **Blockbench web** con GeckoLib Models & Animations 4.2.5. Las cuatro piezas están integradas en Minecraft Java 1.21.1 con Fabric y GeckoLib 4.9.3.

## Uso

En la pestaña creativa de Apogee aparecen el casco, chaleco, pantalones y botas MaxTac. También se obtienen con `/give @s apogee:maxtac_helmet`, `apogee:maxtac_chestplate`, `apogee:maxtac_leggings` y `apogee:maxtac_boots`. Ocupan las cuatro ranuras de armadura vanilla.

Existe además la variante `apogee:maxtac_helmet_closed`, con boca y mentón cubiertos (`blockbench/maxtac_casco_cerrado.bbmodel` y `export/maxtac_casco_cerrado.geo.json`).

Al equipar el casco se encienden las **dos ópticas principales**. **N** activa o apaga las cuatro auxiliares verticales; la tecla se puede reasignar en Controles. El servidor valida la solicitud y comunica el estado al portador y a sus observadores. Quitarse el casco, morir o reconectar reinicia el modo básico. Es un cambio visual: no concede visión nocturna ni otras habilidades.

Las bandas de las mangas son continuas, de color violeta `#BC78FF`, y tienen brillo propio, igual que las ópticas activas. Se ven sin iluminación ambiental; no proyectan luz sobre bloques. Las cuatro auxiliares permanecen oscuras en el modo básico.

Las estadísticas usan **netherita provisional**, rareza épica y resistencia al fuego del objeto. Las recetas usan componentes propios (ver el README); no hay balance definitivo. El JAR de desarrollo se genera en `build/libs/`; el JAR preexistente en la raíz se conserva.

## Archivos

| Archivo | Uso |
|---|---|
| `blockbench/maxtac.bbmodel` | Proyecto principal de tipo Armor, con atlas incrustado de 256 × 256. |
| `blockbench/maxtac_con_maniqui.bbmodel` | Guía con una cara de muestra bloqueada, excluida de la exportación. No es la skin del usuario. |
| `blockbench/maxtac_con_maniqui_reposo.bbmodel` | Guía equivalente con solo el par principal encendido. |
| `export/maxtac.geo.json` | Geometría exportada mediante **Export GeckoLib Model** de Blockbench. |
| `textures/maxtac.png` y `maxtac_standby.png` | Texturas de seis y dos ópticas activas. |
| `textures/*_glowmask.png` | Máscaras emisivas para AutoGlowingGeoLayer de GeckoLib. |
| `previews/` | Capturas de Blockbench y de Minecraft, sin retoques. |
| `evidence/` | Registro de la prueba de cliente e informe de GameTest. |
| `referencias-seleccionadas.json` | Copia sin cambios de la selección del usuario. |
| `validacion.json` | Comprobaciones estructurales y evidencia de ejecución disponible. |

## Modelado y ajuste

- Casco corregido según `03`, `09`, `10` y `11`: boca y mentón descubiertos, negro uniforme, nariz fina y seis ópticas circulares con biseles de dieciséis segmentos. La abertura muestra la skin; la armadura no dibuja una boca propia.
- Carcasa con espacio para la capa exterior de la cabeza, mangas ampliadas, traje que cubre el torso y pantalones que envuelven las piernas. Las bandas permanecen por fuera del tejido y enlazan sus tramos.
- Hombreras inclinadas diez grados; cada manga, pierna y bota sigue su hueso. El calzado es una reconstrucción provisional porque las referencias no lo describen por completo.
- 380 cubos (394 en la variante cerrada), 25 huesos y ocho grupos de equipamiento con los pivotes de la plantilla: `armorHead`, `armorBody`, `armorRightArm`, `armorLeftArm`, `armorRightLeg`, `armorLeftLeg`, `armorRightBoot` y `armorLeftBoot`.

## Validación

Essential queda fuera del alcance de validación de MaxTac por indicación del usuario.

`runMaxtacSmoke` abre Minecraft, equipa las cuatro piezas y comprueba el par principal, la pulsación N, la respuesta del servidor, la vuelta a dos ópticas, el rechazo sin casco y el reinicio al reequipar. Captura los dos modos en una cámara con **luz de bloques 0 y luz del cielo 0**. También fuerza skins vanilla reproducibles de brazos clásicos y delgados, con capas exteriores activas, para revisar cobertura frontal y posterior. El resultado correcto se identifica por **MAXTAC_SMOKE_PASSED** en el registro, no solo por el código de salida de Gradle.

`runGametest` incluye tres pruebas MaxTac para las cuatro ranuras, la exigencia del casco y el reinicio al reequipar el mismo objeto. El informe completo está en `build/gametest/report.xml`. `check` ejecuta las comprobaciones de Gradle y la prueba de movimiento del Nanocable. Los resultados conservados están en `validacion.json` y `evidence/`.

Quedan pendientes una sesión con dos clientes reales, todas las poses de movimiento y extremos de giro, encantamientos, primera persona, mezcla visual con otras armaduras y convivencia con Mantis, Nanocable, Bomba de sangre y Sandevistan. La prueba del visor en cliente y servidor integrado no equivale a esa sesión multijugador completa.

## Edición y reproducción

1. Abrir el proyecto principal en Blockbench con el complemento GeckoLib y mantener **Model Type: Armor**.
2. Guardar el `.bbmodel` y exportar con **Export GeckoLib Model** a `export/maxtac.geo.json`.
3. Ejecutar `python art/maxtac/source/sync_runtime.py` para copiar geometría, texturas e iconos al mod.
4. Ejecutar `python art/maxtac/source/validate_model.py`, `gradlew.bat check build`, `gradlew.bat runGametest` y `gradlew.bat runMaxtacSmoke`, con Java 21.

`source/build_model.py` conserva la construcción inicial y el atlas. **Al ejecutarlo sobrescribe los proyectos y texturas de arte**; conservar antes las ediciones posteriores de Blockbench. Las guías usan un atlas ampliado con una cara de muestra y no sustituyen la textura de entrega.

En esta máquina se utilizó un JDK 21 temporal y `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Administrator\AppData\Local\Temp\apogee-no-unix-sockets` solo en el proceso de prueba para evitar un fallo local de sockets Unix de Java. No se modificaron ajustes del sistema.

La rama es `codex/maxtac-armor`, basada en `develop` (`4ce6966`). Se conservaron los cambios locales de imágenes y el JAR preexistente; no se hizo commit ni push. La geometría, texturas e iconos son originales; las capturas de Cyberpunk solo se usan como referencias locales.

Documentación primaria: [armaduras de GeckoLib 4](https://github.com/bernie-g/geckolib/wiki/Geckolib-Armor-%28Geckolib4%29), [texturas emisivas de GeckoLib 4](https://github.com/bernie-g/geckolib/wiki/Emissive-Textures-%28Geckolib4%29).