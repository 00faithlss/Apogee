# Nanocable rojo en Apogee

Paquete `nanocable-rojo-geckolib-assets` v0.3, aportado por el autor del proyecto. Esta carpeta guarda las fuentes livianas; el mod usa las exportaciones copiadas a `src/main/resources/assets/apogee/`.

## Qué se incluyó

- `LEEME.md`, `INVESTIGACION.md`, `integration.json`, `animation-events.json`, `validation.json`: documentación y datos del paquete, sin cambios.
- `source/`: generador (`build.py`), especificación del modelo (`model_spec.json`), renderer de revisión y validación. Regeneran todos los modelos, animaciones y proyectos de Blockbench.
- `integration/`: la máquina de estados de entrada del paquete, solo como referencia. El mod usa su propia lógica, autorizada por el servidor, compartida con las Cuchillas Mantis.
- `previews/`: lámina, GIF, MP4 y montaje en tercera persona del paquete.

En el mod:

- `geo/nanowire_{left,right}_attachment.geo.json` y sus animaciones: la carcasa de cada antebrazo (obturador, tapa de servicio y carrete), montada sobre el brazo del jugador. Se les quitó el cable con `tools/nanowire_strip_wire.py`: el paquete lo trae horneado como 64 segmentos rígidos a 60 fps (unos 8 MB de animación por brazo) y el mod lo dibuja por su cuenta.
- `geo/nanowire_item.geo.json`: el modelo del objeto, con el cable enrollado (hueso `stored_wire`) cambiado por un tramo recto y tenso entre las bocas de las dos carcasas, a pedido del autor. Regenerar el objeto desde el paquete devuelve el aro.
- `textures/nanowire/nanowire_red{,_glowmask}.png`: atlas y máscara emisiva, sin cambios.

Para regenerar las exportaciones sin cable desde el paquete:

```sh
python3 tools/nanowire_strip_wire.py <paquete>/src/main/resources/assets/apogee
```

## El cable

El cable se calcula en cada fotograma (`client/nanowire/NanowireWire` y `NanowireMoves`), en primera y en tercera persona:

- En reposo va tenso entre las dos manos, con un leve zumbido.
- Cada golpe sigue una curva por puntos clave (Catmull-Rom centrípeta) que se recorre por longitud de arco con una sola curva de velocidad, así la punta acelera y frena sin tirones.
- El cable tiene largo fijo y se desliza por el camino de su punta, por eso los lazos se forman solos.
- Al golpear, los dos cables se separan: cada uno sale como un látigo desde su propia mano y, al recogerse, se tensa y vuelve a unirse con el otro en el centro. El golpe doble se turna entre tres formas: una X, un tajo vertical con los cables en paralelo y una S con el cable derecho arriba y el izquierdo abajo.
- Las estelas son el mismo cable unos instantes antes.
- Se dibuja como cintas orientadas a la cámara: un núcleo rojo emisivo con la textura del paquete y un halo aditivo.

La coreografía (el latigazo que cruza la vista y cierra un lazo, el golpe doble en X, vertical o en S, con los dos brazos) se comparó fotograma a fotograma con un video del Monowire de Cyberpunk 2077 que aportó el autor. Del video solo se tomaron trayectorias y tiempos medidos; no va al repositorio ni al JAR.

## Qué no se incluyó

- `blockbench/*.bbmodel` (30 a 60 MB cada uno): se regeneran con `python3 source/build.py`.
- `nanowire_arms.geo.json` y `nanowire_arms.animation.json`: el rig de dos brazos del paquete, solo de referencia. La primera persona usa los brazos del jugador en la posición de Minecraft, con coreografía propia.
- `COMPATIBILIDAD-PUNCHY.md` e `integration/punchy-audit.json`: el paquete los preparó descompilando Punchy 2.8c, cuya licencia no lo permite. La compatibilidad con Punchy se mide desde fuera, como con las Cuchillas Mantis.
