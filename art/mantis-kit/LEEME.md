# Mantis — modelos detallados v0.4

Destino: Minecraft Java **1.21.1 / Fabric / GeckoLib 4**. Esta revisión transforma un brazo cerrado de tamaño clásico en un implante abierto por arriba y por debajo. Incluye geometría detallada, texturas, animaciones y escenas de montaje lateral. **No es un mod instalable ni un JAR compilado.** Las vistas se calculan desde los recursos exportados; aún falta probarlos en Blockbench y Minecraft.

## Cambios de esta revisión

- Corrección del cruce señalado entre cubierta superior y soporte de la hoja. Se sustituyó la cubierta grande por dos piezas más cortas y se rehízo su trayectoria.
- Las bisagras se desplazan sobre guías: primero despejan el volumen que recorrerá la hoja, después vuelven a su posición de reposo abierto.
- Un elevador de 1.1 unidades, con cilindros y vástagos, da altura al soporte principal antes de extender el filo.
- Las paredes inferiores de piel se recogen detrás de las superiores. No acompañan al suelo formando una caja profunda.
- Dos placas inferiores finas, independientes, abren desde el codo y desde la muñeca; dejan una separación central. Conservan refuerzos, tendones, abrazaderas y conectores.
- Se mantienen las tres variantes, ambas Mantis en primera persona y el montaje lateral con brazos separados en tercera persona.
- El brazo cerrado conserva el volumen clásico **4 × 4 × 12**. La piel beige sigue siendo una referencia de material.

Por implante: normal **438 cuboides**, térmica **510**, eléctrica **512**, con **25 huesos exportados**. Los proyectos individuales agregan tres cubos y dos grupos de piel de referencia. Todos los cuboides y sus pivotes son editables.

## Archivos

| Archivo | Uso |
|---|---|
| `mantis-visor.html` | Visor WebGL local: variante, estado, giro, zoom y tercera persona |
| `previews/mantis-modelos.png` | Modelos completos, plegado, ambas hojas y montaje lateral |
| `previews/mantis-detalles.png` | Corte visual sin piel, cubierta inferior, hoja térmica y eléctrica |
| `previews/mantis-muneca.png` | Cuatro fases de apertura |
| `previews/mantis-movimiento.gif` | Movimiento continuo calculado desde la animación JSON |
| `blockbench/mantis_<variante>_<lado>.bbmodel` | Seis modelos individuales con texturas incrustadas |
| `blockbench/mantis_<variante>_montaje_lateral.bbmodel` | Dos escenas editables de tercera persona, térmica y eléctrica |
| `montaje-referencia.json` | Ejes y orden de transformaciones del montaje |
| `PROMPT-IMPLEMENTACION.md` | Prompt actualizado para integrar en el repositorio Java |

Abre los proyectos en Blockbench 5 o posterior. Son modelos genéricos con animaciones; la pose base es plegada. Selecciona `animation.mantis.idle` en el modo de animación para verlos desplegados, o `deploy` para la apertura. Para exportar mediante el flujo de GeckoLib, instala su plugin y convierte una copia. Ya se incluyen los JSON exportados.

Las escenas laterales son una referencia de montaje, no un ítem único listo para usar. Sus clips muestran ambos mecanismos moviéndose para inspeccionarlos; la alternancia de ataques se implementa por instancia en Java.

## Recursos del mod

Copia `resources/assets/mantis/` a `src/main/resources/assets/<tu_modid>/` y adapta el namespace.

| Tipo | Rutas |
|---|---|
| Geometría | `geo/mantis_<normal/thermal/electric>_<right/left>.geo.json` |
| Animación | `animations/mantis_<right/left>.animation.json` |
| Textura | `textures/item/mantis_<normal/thermal/electric>.png` |
| Máscara emisiva | El PNG correspondiente terminado en `_emissive.png` |

Ahora las variantes **no comparten un único JSON de geometría**: cambiar solo la textura no añade electrodos ni disipadores. Selecciona ambos recursos. El atlas mide 64×64 y contiene 16 materiales. La piel beige y la manga azul son referencias, no colores que deban imponerse a la skin real.

## Ejes y montaje lateral

En el editor, la mano apunta a **-Z**, el codo a **+Z** y la apertura del implante a **+Y**. El brazo de referencia ocupa X=-2…2, Y=-2…2 y Z=-6…6; la mecánica queda dentro de ese volumen cuando se cierra. Al abrirse, las cubiertas y el mecanismo salen de ese volumen.

Con vectores columna, las escenas usan:

`T(hombro) · Rz(separación) · T(0, -6, 0) · Rx(-90°) · Rz(giro lateral) · transformación animada`

La escena usa una pose de combate con separación de 20° por brazo para dejar espacio a la apertura inferior: derecho -20° e izquierdo +20° en Z, alrededor del hombro (±6, 24, 0). El vector dorsal queda (∓0.9397, +0.3420, 0): hacia fuera y ligeramente arriba, sin componente hacia delante. Los centros de la tabla son anteriores a esa separación.

| Lado anatómico | Centro en el maniquí | Giro lateral Z | Apertura |
|---|---|---|---|
| Derecho | (-6, 18, 0) | +90° | -X, hacia fuera y arriba |
| Izquierdo | (+6, 18, 0) | -90° | +X, hacia fuera y arriba |

El maniquí mira hacia -Z. Su derecha anatómica aparece a la izquierda de la imagen frontal. El mecanismo y la hoja quedan al costado externo; en reposo la punta sigue la dirección longitudinal del brazo. El brazo completo orienta el corte al atacar.

Las escenas separan `*_shoulder_pose`, `*_mount_rx`, `*_mount_rz` y `*_arm_root`: el montaje no se mezcla con la animación. En Java, reemplaza las posiciones del maniquí por las transformaciones de los brazos del jugador. Evita aplicar dos veces el movimiento vanilla.

Primera persona necesita matrices de cámara propias. No reutilices las posiciones absolutas del maniquí. El exportador refleja X y cambia el signo de rotación X/Y entre Blockbench y el JSON de GeckoLib. `montaje-referencia.json` usa ejes del editor; **Minecraft no carga automáticamente ese archivo**.

## Huesos y pivotes del modelo derecho

| Hueso | Padre | Pivote del editor | Función |
|---|---|---|---|
| `arm_root` | `—` | (0, 0, 0) | Montaje y ataque |
| `housing` | `arm_root` | (0, -0.65, 0) | Carcasa y cables |
| `wrist` | `arm_root` | (0, 0, -3.6) | Mano y piel distal |
| `link_lower` | `housing` | (0, 0.02, -2.1) | Soporte principal y elevación |
| `link_upper` | `link_lower` | (0, 0.02, 0.49) | Segundo soporte |
| `blade` | `link_upper` | (0, 0.594, 2.744) | Hoja y contrarrotación |
| `slide_mid` | `blade` | (0, 0.594, 2.744) | Tramo intermedio |
| `slide_tip` | `slide_mid` | (0, 0.594, 2.744) | Tramo final |
| `dorsal_slide` | `housing` | (0, 0, 0) | Carro de bisagra anterior |
| `rear_slide` | `housing` | (0, 0, 0) | Carro de bisagra posterior |
| `dorsal_lid` | `dorsal_slide` | (0, 1.75, -2.7) | Cubierta superior anterior |
| `dorsal_rear` | `rear_slide` | (0, 1.75, 2.5) | Cubierta superior posterior |
| `proximal_lid` | `housing` | (0, 1.75, 4.6) | Tapa junto al codo |
| `wrist_flap` | `housing` | (0, 1.75, -3.6) | Tapa de muñeca |
| `ventral_root` | `housing` | (0, -1.6, 4.6) | Placa inferior: bisagra del codo |
| `ventral_tip` | `housing` | (0, -1.6, -3.6) | Placa inferior: bisagra de muñeca |
| `lift_carriage` | `housing` | (0, 0, 0) | Vástagos del elevador |
| `side_skin_wrist_left` | `wrist` | (-2, 0, -3.6) | Piel lateral inferior retráctil |
| `side_skin_wrist_right` | `wrist` | (1.9, 0, -3.6) | Piel lateral inferior retráctil |
| `side_skin_main_left` | `wrist` | (-2, 0, -2.7) | Piel lateral inferior retráctil |
| `side_skin_main_right` | `wrist` | (1.9, 0, -2.7) | Piel lateral inferior retráctil |
| `side_skin_upper_rear_left` | `housing` | (-2, 0, 0) | Piel lateral inferior retráctil |
| `side_skin_upper_rear_right` | `housing` | (1.9, 0, 0) | Piel lateral inferior retráctil |
| `side_skin_rear_left` | `housing` | (-2, 0, 2.5) | Piel lateral inferior retráctil |
| `side_skin_rear_right` | `housing` | (1.9, 0, 2.5) | Piel lateral inferior retráctil |

`reference_forearm` y `reference_hand` aparecen solo en el proyecto editable y en los renders; se excluyen del runtime. La mano de referencia hereda de `wrist`.

## Animaciones

Todos los nombres llevan el prefijo `animation.mantis.`.

| Clip | Duración | Resultado |
|---|---|---|
| `folded` | 1.0 s, pose constante en bucle | Plegada |
| `idle` | 1.0 s, pose constante en bucle | Desplegada |
| `deploy` | 1.0 s, mantiene el último fotograma | Desplegada |
| `retract` | 1.0 s, mantiene el último fotograma | Plegada |
| `attack_right` | 0.6 s | Regresa a desplegada |
| `attack_left` | 0.6 s | Regresa a desplegada |

El despliegue dura 1 segundo. Los pasos siguientes describen las claves entregadas, no reglas automáticas del motor:

| Intervalo | Movimiento |
|---|---|
| 0–0.16 s | Las láminas inferiores de piel suben 1.85 unidades y se recogen 0.18 hacia dentro detrás de las superiores. |
| 0.16–0.34 s | Apertura inferior y despeje superior: las cubiertas principales llegan a -120°/+120°; sus carros se desplazan -2.4/+1.5 en Z. |
| 0–0.38 s | La muñeca flexiona hasta -18°. |
| 0.38–0.68 s | Los soportes llegan a -43°/-25°; la hoja compensa +68°. El elevador sube 1.1 unidades. |
| 0.70–0.84 s | Con la hoja elevada, los carros vuelven y las cubiertas superiores se colocan a -43°/+60°. |
| 0.84–1.00 s | Cada tramo telescópico se desplaza (-0.371, 0, -4.25). |

La tapa proximal queda a +28° y la de muñeca a -32°. Las placas inferiores tienen bisagras **independientes**: `ventral_root` gira -24° desde el codo y `ventral_tip` gira +30° desde la muñeca; esta última ya no hereda de la placa posterior. Los tendones de cada placa terminan en sus conectores; los cinco cables principales siguen en el cuerpo del implante.

El repliegue recorre exactamente la misma secuencia a la inversa. La piel no se elimina ni se hace transparente en la animación. Las piezas conservan sus dimensiones. El lado izquierdo refleja posiciones y giros correspondientes. Los recorridos temporales amplios de las cubiertas son necesarios para evitar el cruce detectado en la versión anterior; no los sustituyas por interpolación directa de cerrada a reposo abierto.

La hoja telescópica resuelve el guardado dentro del implante. Es una interpretación visual, no una simulación mecánica de fabricación. Se revisaron renders y se añadió una comprobación de cajas orientadas (SAT/OBB) de la hoja y los soportes contra todas las piezas de piel, la mano de referencia y las cubiertas superiores. Consulta `HOLGURAS.txt` para los resultados y límites. No es una certificación de todos los contactos mecánicos del conjunto. Las interpolaciones son lineales. Los clips de ataque indican direcciones de corte: prueba la combinación anatómica al conectarlos y usa un controlador independiente por brazo. El golpe **no guarda automáticamente la hoja**.

## Piel, cavidad y muñeca: integración necesaria

1. Sustituye o enmascara el volumen vanilla que ocupa este brazo. Al cerrar, los paneles segmentados deben reconstruir su superficie; al abrir, deben dejar una cavidad real. Dejar un cubo sólido de piel debajo taparía los cables. La geometría entregada corresponde al brazo clásico de cuatro píxeles; Slim requiere su propia adaptación.
2. Dibuja la mano bajo la transformación de `wrist`; mover solo el collar dejaría rígida la mano vanilla.
3. Remapea los paneles del material `skin` a las zonas correspondientes de la skin real y su segunda capa. El atlas del paquete solo identifica materiales: **no contiene aún el UV final de una skin del jugador**. Para la mano y el anclaje del codo, reconstruye los tres cubos `reference_*` que se excluyen del runtime, aplicando la skin y el hueso adecuado. No dejes huecos en los extremos al cerrar.
4. Separa ligeramente los brazos del torso en la pose de combate para dar espacio a las cubiertas inferiores. La escena de montaje incluye una separación de 20° como referencia, pero deben revisarse las colisiones durante todas las poses del juego. Conserva el montaje lateral al caminar, agacharse y atacar; prueba brazos clásicos/Slim, armaduras y ambas perspectivas.
5. Evita duplicar la pista de ataque de `arm_root` si Java ya mueve el brazo entero. Piel e implante deben compartir transformación.

Copiar los JSON no implementa estas operaciones de renderizado.

## Emisión y efectos

Las máscaras `_emissive` necesitan una capa registrada en GeckoLib; su nombre no activa nada automáticamente. Los renders/visor muestran los acentos a brillo completo, sin simular bloom ni iluminación del juego. En Java, atenúa o desactiva la emisión al guardar el implante. Las brasas y los arcos eléctricos deben implementarse como efectos dinámicos; los electrodos del modelo son hardware real, no rayos congelados.

El servidor controla daño/cooldown y sincroniza variante y despliegue. No se incluyen sonidos, eventos de daño ni lógica de red.

## Regeneración y validación

Requiere Python, Pillow y NumPy:

```sh
python source/build_models.py
python source/build_mounting.py
python source/render_preview.py
python source/render_details.py
python source/finalize.py
python source/validate_models.py
python source/check_clearance.py
```

Los resultados se registran en `VALIDACION.txt` y `HOLGURAS.txt`: JSON, UV, referencias, dimensiones, imágenes, cierre dentro de 4 × 4 × 12, simetría y orientación lateral. **Pendientes:** interfaz de Blockbench, ejecución del visor en navegador real, integración y pruebas en Minecraft, rendimiento y animaciones con skins reales. No se ha compilado un mod.

Referencias: [Blockbench](https://www.blockbench.net/wiki/docs/bbmodel/), [GeckoLib 4: modelos](https://github.com/bernie-g/geckolib/wiki/Geo-Models-(Geckolib4)), [instalación](https://github.com/bernie-g/geckolib/wiki/Installation-(Geckolib4)), [geometría 1.12.0](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/visualreference/geometry.v1.12.0).

Diseño fan-made basado en las referencias de la conversación, sin afiliación con Cyberpunk o Minecraft.
