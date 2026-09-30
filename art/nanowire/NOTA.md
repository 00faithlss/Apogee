# Nanocable rojo en Apogee

Paquete `nanocable-rojo-geckolib-assets` v0.3, aportado por el autor del proyecto. Esta carpeta guarda las fuentes livianas; el mod usa las exportaciones copiadas a `src/main/resources/assets/apogee/`.

## Qué se incluyó

- `LEEME.md`, `INVESTIGACION.md`, `integration.json`, `animation-events.json`, `validation.json`: documentación y datos del paquete, sin cambios.
- `source/`: generador (`build.py`), especificación del modelo (`model_spec.json`), renderer de revisión y validación. Regeneran todos los modelos, animaciones y proyectos de Blockbench.
- `integration/`: la máquina de estados de entrada del paquete, solo como referencia. El mod usa su propia lógica, autorizada por el servidor, compartida con las Cuchillas Mantis.
- `previews/`: lámina, GIF, MP4 y montaje en tercera persona del paquete.

En el mod:

- `geo/nanowire_{left,right}_attachment.geo.json` y sus animaciones: la carcasa de cada antebrazo con su cable, montadas sobre el brazo del jugador.
- `geo/nanowire_item.geo.json`: el modelo del objeto.
- `textures/nanowire/nanowire_red{,_glowmask}.png`: atlas y máscara emisiva.
- `nanowire/arm_motion.json`: las pistas `left_arm` y `right_arm` de las siete animaciones del rig de dos brazos (`nanowire_arms.animation.json`), extraídas para mover los brazos del jugador en primera persona. El resto de ese archivo (17 MB) son los cables, que ya traen las exportaciones por brazo.

## Qué no se incluyó

- `blockbench/*.bbmodel` (30 a 60 MB cada uno): se regeneran con `python3 source/build.py`.
- `nanowire_arms.geo.json` y `nanowire_arms.animation.json` completos: referencia de coreografía; de ellos solo se usan las pistas de los brazos.
- `COMPATIBILIDAD-PUNCHY.md` e `integration/punchy-audit.json`: el paquete los preparó descompilando Punchy 2.8c, cuya licencia no lo permite. La compatibilidad con Punchy se mide desde fuera, como con las Cuchillas Mantis.
