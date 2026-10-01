# Análisis de la copia local — cyberpunk-fabric / Mantis

Fecha: 30 de septiembre de 2026.

## Alcance y procedencia

Se solicitó revisar `maaurissio/cyberpunk-fabric`, rama `feat/mantis-blade`. El acceso mediante GitHub devolvió 404 tanto al consultar el repositorio como al pedir su README. No se puede confirmar el estado actual de esa rama ni un SHA remoto. Esta revisión se basa en el ZIP previamente aportado y la revisión local `mantis-modular-review/cyberpunk-fabric`. El inventario y las huellas de los modelos usados se incluyen en `source-provenance.json`.

## Hallazgos comprobados en el código local

| Fuente | Hallazgo | Consecuencia para la mesa |
| --- | --- | --- |
| `src/main/java/dev/apogee/registry/ApogeeItems.java` | Registra tres Mantis y Sandevistan. Los tres componentes T5 se construyen como `Item`. | Mapear los IDs de las Mantis; no inventar niveles de implante a partir de componentes de fabricación. |
| `src/main/java/dev/apogee/mantis/MantisVariant.java` | Variantes STANDARD, THERMAL, ELECTRIC. | STANDARD se representa con el nombre de asset `normal`. |
| `src/main/java/dev/apogee/mantis/MantisManager.java` | `equippedVariant` consulta objetos `MantisBladesItem` equipados con Trinkets; `toggle` gestiona por separado el despliegue. | La anatomía debe reflejar lo equipado, no depender de que esté desplegado. |
| `src/main/resources/data/trinkets/slots/cyberware/arms.json` | Una ranura Brazos, validada por etiqueta. | Un objeto Mantis equipa el par de brazos, no una ranura por mano. |
| `src/main/java/dev/apogee/client/mantis/MantisArmGeoModel.java` | Geometría por variante/lado, animaciones por lado y textura por variante; usa `entityCutoutNoCull`. | Reutilizar identidad visual; separar renderer de diagnóstico y material de piel translúcida. |
| `src/main/java/dev/apogee/client/model/SandevistanModel.java` | Existe un modelo Java para otro implante. | Se podrá adaptar como módulo de columna en un trabajo posterior. No está adaptado en esta entrega. |

## Correspondencia exacta de esta entrega

| Item | Ranura | Modelo bilateral de rayos X | Cubos |
| --- | --- | --- | ---: |
| `apogee:mantis_blades` | `cyberware/arms` | `mantis_xray_normal_pair.bbmodel` | 660 |
| `apogee:mantis_blades_thermal` | `cyberware/arms` | `mantis_xray_thermal_pair.bbmodel` | 804 |
| `apogee:mantis_blades_electric` | `cyberware/arms` | `mantis_xray_electric_pair.bbmodel` | 808 |

Los grupos de mecanismo extraídos de los modelos completos son housing, link_lower, link_upper, blade, slide_mid, slide_tip, blade_slider, cutting_edge y lift_carriage. Se conserva la geometría de cada grupo disponible y sus UV; las cubiertas y la mano externas no se usan como otra capa de piel. El avatar de diagnóstico proporciona su propia envoltura.

Los archivos completos de `combat-source/` se copiaron byte a byte desde la revisión local. No se editaron sus animaciones. Los nuevos módulos de diagnóstico no tienen controladores de despliegue ni sustituyen los archivos `.geo.json` del juego.

## Assets existentes identificados

En `src/main/resources/assets/apogee/` hay seis geometrías Mantis (tres variantes por dos lados), dos archivos de animaciones (uno por lado), tres texturas Mantis y tres máscaras de brillo, iconos de ítems y slots y texturas del Sandevistan. `art/mantis-kit/blockbench/` contiene los modelos fuente editables utilizados. `art/mantis-kit/modular/` conserva la separación de hoja y riel de la revisión anterior.

## Integración pendiente

La mesa aún necesita su bloque, menú/pantalla Java, slots enlazados al equipo real y sincronización validada por servidor. Después, un renderer del avatar debe cargar la skin real y resolver los módulos a partir del equipo. Los nuevos `.bbmodel` son fuentes de edición: requieren exportación o conversión antes de usarlos en el renderer elegido. Mantener las animaciones de combate actuales independientes del estado de la pantalla.

No se generó un JAR ni se realizó un commit o push.
