# Mesa de matasanos en Apogee

Paquete `mesa-matasanos-edgerunner-assets`, aportado por el autor del proyecto: diseño de la pantalla, maqueta interactiva y módulos de rayos X de las Cuchillas Mantis. Esta carpeta guarda las fuentes livianas.

## Qué usa el mod

La pantalla se armó primero con `textures/mesa_matasanos_gui.png` (480 × 356). El autor pidió después que funcionara como un cofre, con el inventario normal abajo, así que hoy se dibuja con la textura de cofre de Minecraft y un panel con los colores, las esquinas cian y los botones del diseño. De `layout.json` se toman las diez categorías y cuántas ranuras tiene cada una: cada categoría es una ranura de Trinkets `cyberware/<id>`. Los íconos `textures/item-*.png` se usan, en gris, como fondo de las ranuras vacías. La textura y las maquetas quedan como referencia.

La camilla del bloque no viene de este paquete: es la camilla del conjunto `ripperdoc` de Project: Edgerunner (ver `LICENSE_ASSETS.md`).

## Rayos X

El botón Rayos X del visor vuelve translúcida la piel del jugador (opacidad de un tercio, teñida de cian, sin escribir profundidad) y deja solo las capas de implantes, así que se ven los implantes instalados a través del cuerpo con sus propios modelos. Los módulos de `modules/` (anatomía y Mantis en rayos X) quedan como referencia: no se usan todavía.

## Pendiente

- Anatomía en rayos X (huesos, órganos) con los módulos de `modules/`.
- Implantes para las demás categorías (corteza, esqueleto, etc.): las ranuras ya existen y aceptan lo que entre en su etiqueta `trinkets:cyberware/<id>`.

## Qué no se incluyó

- `mesa-matasanos.bbmodel` (composición de referencia, 1,4 MB), `states/` (avatares ensamblados, se regeneran con `source/build_modules.py`) y `combat-source/` (copia de los modelos de combate que ya están en `art/mantis-kit/`).
- `mesa-matasanos-demo.html`: versión anterior de la maqueta; queda `mesa-matasanos-edgerunner-demo.html`.
