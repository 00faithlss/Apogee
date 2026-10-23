# Trauma Team — prototipo equipable

Conjunto de cuatro piezas para Minecraft Java 1.21.1, Fabric y GeckoLib 4.9.3, revisado y exportado en **Blockbench web** con GeckoLib Models & Animations 4.2.5. Geometría, texturas e iconos originales; las imágenes de Cyberpunk se conservan solo como referencias.

## Uso

Casco, chaleco, pantalones y botas están en la pestaña creativa de Apogee. IDs: `apogee:trauma_team_helmet`, `apogee:trauma_team_chestplate`, `apogee:trauma_team_leggings` y `apogee:trauma_team_boots`. Ocupan las cuatro ranuras vanilla.

**N** abre y cierra la carcasa frontal. Comparte el control de casco con MaxTac: la pieza equipada determina la acción y la tecla se puede reasignar en Controles. El casquete frontal, formado por una quilla central y dos alas separadas por líneas negras finas, gira hacia arriba **60°** con la bisagra en la corona y sube como una sola pieza. Cerrada, la placa se solapa con el respirador y oculta toda la cara, incluidos los ojos. La lámina oscura translúcida y el respirador permanecen delante del rostro al abrirla. La transición dura medio segundo y puede invertirse a mitad del recorrido sin saltos.

El servidor valida la acción y comunica el estado al portador y sus observadores. Reequipar el casco, morir o reconectar lo deja cerrado. Cada portador conserva su propia pose; no hay visión nocturna ni prestaciones médicas adicionales.

Los **cuatro indicadores de la máscara parpadean en rojo**: 0,4 segundos encendidos y 0,6 apagados. Su emisión solo aparece durante la fase encendida; la lámpara lateral blanca mantiene su brillo propio. No proyectan luz sobre los bloques.

El traje tiene verde/turquesa oscuro, chaleco y bolsas blancas, guantes rojos, rodilleras oscuras y refuerzos rojos en las suelas. Sigue las referencias `02`, `03` y `05`; `01` sirve de apoyo para el uniforme. La selección completa aportada por el usuario se conserva sin cambios en `referencias-seleccionadas.json`.

Las estadísticas son **netherita provisional**, con rareza épica y resistencia al fuego del objeto. Las recetas usan componentes propios (ver el README); no hay balance definitivo. El JAR de desarrollo se genera en `build/libs/`, conservando el JAR preexistente de la raíz.

## Archivos

| Archivo | Uso |
|---|---|
| `blockbench/trauma_team.bbmodel` | Proyecto principal Armor; geometría, atlas incrustado y animaciones de abrir/cerrar. |
| `blockbench/trauma_team_guia_abierto.bbmodel` | Guía de la posición abierta con cabeza de muestra; no exportar como geometría base. |
| `blockbench/trauma_team_guia_cerrado.bbmodel` | Guía cerrada con cabeza de muestra. |
| `export/trauma_team.geo.json` | Exportación real mediante **Export GeckoLib Model**. |
| `export/trauma_team.animation.json` | Apertura/cierre exportados con **Export Animations**. |
| `textures/trauma_team.png` y `trauma_team_off.png` | Fases encendida y apagada del parpadeo. |
| `textures/*_glowmask.png` | Máscaras de emisión para GeckoLib. |
| `previews/` | Capturas de Blockbench y Minecraft sin retoques. |
| `evidence/` | Registros de las pruebas ejecutadas. |
| `validacion.json` | Resultado estructural y evidencia de ejecución disponible. |

La cabeza de las guías es una muestra y está excluida de exportación. Su atlas ampliado no sustituye los PNG del juego. El conjunto tiene 454 cubos, 28 huesos (los 25 de la plantilla más la quilla y las dos alas del casquete) y los ocho grupos de equipamiento de la plantilla GeckoLib. Mangas, torso, pantalones y botas incluyen margen para las capas exteriores de las skins.

## Pruebas y límites

`runTraumaSmoke` abre Minecraft y comprueba N, la respuesta del servidor, los extremos de apertura/cierre, el rechazo sin casco y el reinicio al reequipar. Captura la máscara cerrada y abierta, el cristal interior, ambas fases del parpadeo en una habitación con luz de bloques y cielo a cero, y el ajuste con skins clásicas y delgadas y sus capas exteriores. El resultado se identifica por `TRAUMA_SMOKE_PASSED` en el registro, no solo por la salida de Gradle.

`runGametest` incluye tres pruebas Trauma Team: ranuras, necesidad del casco y reinicio al reequipar el mismo objeto. `check` incluye las comprobaciones de movimiento del Nanocable. Las cifras verificadas de cada ejecución están en `validacion.json` y los informes de `evidence/`.

Quedan pendientes todas las poses y extremos de giro, la convivencia visual con Mantis/Nanocable y otros implantes, mezcla con otras armaduras, primera persona y una sesión de dos clientes reales. **Essential queda fuera del alcance por indicación del usuario.**

## Edición

Abrir el proyecto principal en Blockbench y mantener **Model Type: Armor**. Guardar el `.bbmodel`, exportar geometría y animaciones a `export/`, ejecutar `python art/trauma-team/source/sync_runtime.py` y después `python art/trauma-team/source/validate_model.py`. Para reproducir las pruebas, usar Java 21 y ejecutar `gradlew.bat runTraumaSmoke`, `gradlew.bat runGametest` y `gradlew.bat check build`.

`source/build_model.py` sobrescribe los proyectos y atlas al regenerar: conservar primero las ediciones posteriores de Blockbench. La pose del juego se interpola por entidad para evitar que un singleton de objeto comparta la apertura de varios portadores; las animaciones exportadas conservan el recorrido editable como referencia.

Se continúa sobre `codex/maxtac-armor`, con la base de armaduras MaxTac aún sin integrar. Se conservan los cambios locales previos; no se hace commit ni push. Documentación primaria: [armaduras de GeckoLib 4](https://github.com/bernie-g/geckolib/wiki/Geckolib-Armor-%28Geckolib4%29) y [texturas emisivas](https://github.com/bernie-g/geckolib/wiki/Emissive-Textures-%28Geckolib4%29).
