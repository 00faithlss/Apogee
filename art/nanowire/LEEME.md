# Nanocable rojo — revisión v0.3

Minecraft Java 1.21.1 / Fabric / GeckoLib 4. Modelos originales editables, animaciones y lógica de selección de ataques. Recursos preparados para ir **dentro del mod**, sin resource pack adicional. Aún no es un mod instalado ni un JAR compilado.

## Cambios pedidos

- Se eliminó la inspección, su clip y cualquier propuesta de tecla I. También se retiró la guardia: el botón derecho queda destinado al golpe doble.
- Primer click izquierdo aceptado: brazo **derecho**. Segundo: **izquierdo**. Después alterna derecha/izquierda.
- Click derecho: golpe de **ambos cables separados**. No cambia cuál será el próximo brazo del combo izquierdo.
- Implantes en el **costado interno de cada antebrazo**, mirando al torso. Montaje espejado ±90° respecto de la cara superior usada antes.
- Sin placas, agarres ni piezas de cyberware en manos o dedos. Los brazos de muestra siguen incluyendo manos normales para revisar proporciones.
- Carcasas negras, juntas, tornillos de óxido negro, ventilación, carrete rebajado, rodillos, dos indicadores rojos y boquilla con apertura geométrica.
- Cable rojo puro **#FF0000**. Cada cable pertenece a su propio brazo. En reposo las dos mitades forman un hilo sujeto entre ambas manos; se separan al atacar y regresan al agarre al terminar.

## Animaciones revisadas en v0.3

Se volvió a revisar el tramo final del MP4 aportado. El golpe doble ahora tiene dos cambios de curvatura que dibujan una S, sin convertir las estelas del video en cables extra. Los golpes de click izquierdo recorren sentidos opuestos y conservan una curvatura de arrastre. Los tres clips de ataque parten y regresan al cable sostenido entre manos. No se añadieron placas ni dedos al cyberware: el contacto se representa sobre la mano normal de Minecraft.

La discretización fija uniones en la guía y el punto de agarre. Esto evita que los segmentos cambien abruptamente de dirección al desplazarse una subdivisión por el borde de la mano. Los tramos ocultos conservan la orientación de su trayectoria para salir sin giros repentinos.

## Archivos

| Archivo | Contenido |
| --- | --- |
| `blockbench/nanowire_arms.bbmodel` | Rig editable de combate: 262 cubos, 141 huesos, siete clips. Sin brazos de muestra. |
| `blockbench/nanowire_arms_with_reference.bbmodel` | 268 cubos, 143 huesos; incluye brazos de referencia no exportables. |
| `blockbench/nanowire_item.bbmodel` | Representación 3D del implante transportado, 174 cubos. |
| `src/main/resources/assets/apogee/geo/` | Geometrías exportadas para GeckoLib. |
| `src/main/resources/assets/apogee/animations/` | Siete animaciones exportadas. |
| `src/main/resources/assets/apogee/textures/nanowire/` | Atlas 64×64 y máscara emisiva roja. |
| `integration/java/dev/apogee/nanowire/NanowireInputState.java` | Máquina de estados Java independiente, con alternancia y control de disponibilidad. |
| `integration/java/dev/apogee/nanowire/NanowireInputStateTest.java` | Prueba ejecutable de la selección de ataques. |
| `integration.json`, `animation-events.json` | Rutas, montaje y ventanas visuales propuestas. No ejecutan daño. |
| `previews/` | Lámina, GIF, MP4 y montaje en tercera persona. |
| `source/`, `validation.json` | Generadores, renderer y resultados de verificación. |
| `INVESTIGACION.md` | Fuentes anteriores y observaciones del video adjunto de 4,13 segundos. |

## Preparación para Punchy

Se descompiló Punchy 2.8c y se rastrearon sus transformaciones de manos. Ver `COMPATIBILIDAD-PUNCHY.md` para el informe. Se añadieron `nanowire_left_attachment` y `nanowire_right_attachment` en Blockbench, geo y animaciones: cada uno está centrado en su propio brazo y no anima la pose de la skin. Así el renderer anfitrión puede determinar separación, locomoción y ajustes de las manos. Los clips de ataque del rig bilateral siguen siendo la referencia para la coreografía de brazos, que deberá integrarse con un único propietario de pose.

Estos exports no equivalen a un hook instalado ni a una prueba en Minecraft.

## Abrir en Blockbench

Abre `nanowire_arms_with_reference.bbmodel` y selecciona `animation.nanowire.idle` en Animate. Los segmentos del cable toman su posición desde las animaciones; la pose base sin un clip no muestra el cable desplegado correctamente.

Los editables están guardados como **Free Model**. Para reexportar desde Blockbench, instala GeckoLib Models & Animations, convierte el proyecto a GeckoLib Animated Model y exporta modelo, animaciones y textura por separado. Se incluyen los JSON ya generados. Los grupos `reference_*` tienen `export: false`.

En esta sesión los archivos se generaron y verificaron por código. El intento anterior de iniciar Blockbench web falló con WebGL; esta revisión no se validó dentro del editor ni con el plugin.

## Mecanismo de salida

Primero se abre la compuerta; después sale el cable por el agujero de la boquilla. La animación recorre progresivamente la longitud de la curva, en vez de agrandar toda la curva desde el centro. El grosor permanece constante salvo una transición corta de aparición. La carcasa y el brazo nunca se escalan.

Un tramo inicial guía el filamento junto al costado de la mano desnuda antes de permitir la curva del latigazo. El carrete gira según la longitud desplegada. Durante la retracción el filamento desaparece antes de cerrar la compuerta.

Cada cable tiene 64 segmentos. Los huesos `wire_left_*` dependen de `left_arm`; los `wire_right_*`, de `right_arm`. Las transformaciones del cable están calculadas en coordenadas locales de ese brazo. No dependen directamente de la raíz del modelo. En reposo sus extremos coinciden en un punto entre las manos, calculado a partir de ambos agarres.

## Clips y controles

Prefijo de todos los clips: `animation.nanowire.`.

| Clip | Duración | Uso |
| --- | ---: | --- |
| `stowed` | 0,10 s, bucle | Cable oculto; compuerta cerrada. |
| `deploy` | 1,10 s | Abre compuertas y alimenta ambos filamentos. |
| `idle` | 2,00 s, bucle | Hilo sujeto entre ambas manos normales, con tensión y movimiento ligero. |
| `attack_right` | 0,72 s | Primer click izquierdo: brazo derecho, barrido de derecha a izquierda. |
| `attack_left` | 0,72 s | Segundo click izquierdo: brazo izquierdo, barrido de izquierda a derecha. |
| `heavy` | 1,15 s | Click derecho: dos curvas en S, con ondulación y puntas separadas durante el golpe. |
| `retract` | 1,00 s | Recoge ambos cables y cierra compuertas. |

Los clips se adaptaron observando los fotogramas del video adjunto, especialmente el tramo 3,2–4,1 s para el barrido bilateral. No son una extracción de la animación original ni una réplica cuadro a cuadro. El color blanco del clip se sustituye por rojo por pedido del usuario.

### Lógica Java incluida

`NanowireInputState` está implementada y probada como clase independiente. Sus métodos no importan Minecraft, Fabric ni Punchy:

- `setActive(true, now)` inicia el despliegue y reinicia la alternancia en la derecha.
- `tick(now)` completa transiciones temporales.
- `press(Button.LEFT, now)` inicia el siguiente ataque de un brazo cuando se encuentra activo y en reposo.
- `press(Button.RIGHT, now)` inicia el doble en las mismas condiciones.
- `setActive(false, now)` retrae; `reset(now)` reinicia al morir/desconectar/quitar el implante.
- `revision()` permite detectar cambios y lanzar el clip una sola vez.

**Los clicks durante despliegue, ataque o retracción se ignoran y no cambian la alternancia.** No hay buffer ni ataque repetido por mantener pulsado. Alimentar `press` solo en un nuevo click físico; usar segundos de un reloj monótono. El modelo vuelve a reposo entre ataques. Si se pulsan ambos botones a la vez, el adaptador debe dar prioridad al derecho y enviar solo esa orden.

La clase se compiló y probó con el compilador Java 17 disponible. No utiliza características incompatibles con Java 21, pero eso no constituye una compilación del mod para Minecraft 1.21.1.

## Integración que falta en el mod y Punchy

1. Registrar el implante/ítem y copiar `assets/apogee/` dentro de los recursos del mod. Registrar GeoModel, animatable y renderers; el modelo de inventario necesita su propio renderer de ítem.
2. Conectar eventos de ataque/uso a la máquina de estados solo cuando el nanocable esté activo y no haya una pantalla abierta. Consumir el ataque/uso vanilla correspondiente únicamente en ese estado. Vincular cada nueva revisión al clip GeckoLib y sincronizar la orden con el servidor.
3. El servidor debe autorizar estado, alcance, cooldown y daño. No confiar en el cliente ni en los cubos de render como hitboxes. Las ventanas de `animation-events.json` son propuestas de sincronización visual.
4. Adaptar el montaje a la pose real del brazo que produce Punchy. Conservar pivotes y transformación local del implante. Aplicar la pose del brazo una sola vez: no acumular sin más la pose completa de este rig y la pose completa de Punchy. El renderer debe decidir qué sistema dirige los brazos durante un ataque y mantener la locomoción/skin del jugador.
5. Con el brazo ya transformado, el mecanismo y su cable lo siguen juntos por su jerarquía. Si se sustituyen las poses de ataque por otras de Punchy, habrá que revisar de nuevo alcance visual y colisiones: las comprobaciones incluidas se hicieron con las poses entregadas.
6. Cargar la máscara `_glowmask.png` con `AutoGlowingGeoLayer` si se quiere emisión fullbright. No implica iluminación dinámica ni bloom.
7. Probar en primera/tercera persona, skin clásica/slim, diferentes FOV, movimientos de Punchy y estados de equipamiento. El montaje actual usa un brazo de cuatro unidades de ancho; slim requiere ajustar el offset lateral.

No se implementó todavía el adaptador Fabric/Punchy, registro del arma, red, daño ni renderer dentro del repositorio. La clase de entrada está separada en `integration/java/` para que no se confunda con código ya conectado al mod. Sin commits ni push.

## Verificación

Se verifican jerarquías, UV, valores de animación, color rojo exacto, vuelta de ataques a reposo, anclaje de cada cable a su boquilla, continuidad de segmentos y ausencia de penetración de puntos muestreados en las cajas de referencia de manos/antebrazos. No es una prueba exhaustiva de colisión entre superficies o contra torso/mundo. Los resultados actuales están en `validation.json`.

Las muestras se renderizan leyendo la geometría y las animaciones exportadas. No son capturas de Minecraft. La prueba Java comprueba inactividad, despliegue, derecha/izquierda, clicks durante ataque, doble, retracción y reinicio.

## Regenerar

Python 3, NumPy, Pillow y FFmpeg para MP4:

```sh
python3 source/build.py
python3 source/render.py
python3 source/third_person.py
python3 source/validate.py
ffmpeg -i previews/nanowire-movimiento.gif -pix_fmt yuv420p -c:v libx264 -movflags +faststart previews/nanowire-movimiento.mp4
```

### Reposo con Punchy: dependencia de las dos manos

La pose de reposo horneada coincide en el rig de muestra. Al integrar los attachments con Punchy, sus brazos pueden moverse de forma independiente; por eso el tramo sujeto entre manos debe reconstruirse cada fotograma desde **los dos puntos de agarre reales**. Usar solo los dos clips locales horneados haría que las mitades se separasen cuando cambie la pose de Punchy. Hay que mezclar esa unión dinámica hacia las curvas libres durante el ataque y devolverla al recuperar el agarre. Esa conexión dinámica todavía no está implementada en el renderer Java.
