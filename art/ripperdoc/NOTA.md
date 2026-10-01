# Mesa de matasanos en Apogee

Paquete `mesa-matasanos-edgerunner-assets`, aportado por el autor del proyecto: diseño de la pantalla, maqueta interactiva y módulos de rayos X de las Cuchillas Mantis. Esta carpeta guarda las fuentes livianas.

## Qué usa el mod

- `textures/mesa_matasanos_gui.png` → `assets/apogee/textures/gui/ripperdoc.png`, con estos cambios pedidos por el autor:
  - sin los objetos de muestra (Mantis térmicas en Brazos y tres objetos en el inventario) ni el marco rosado de selección;
  - sin los textos «Mantis térmicas / Ambos brazos / Instaladas» ni «Capacidad por definir»: la lista de implantes instalados la dibuja el juego.
- `layout.json`: las posiciones de las ranuras de Sistema operativo y Brazos, del inventario y del visor del paciente.

La camilla del bloque no viene de este paquete: es la camilla del conjunto `ripperdoc` de Project: Edgerunner (ver `LICENSE_ASSETS.md`).

## Rayos X

El botón Rayos X del visor vuelve translúcida la piel del jugador (opacidad de un tercio, teñida de cian, sin escribir profundidad) y deja solo las capas de implantes, así que se ven los implantes instalados a través del cuerpo con sus propios modelos. Los módulos de `modules/` (anatomía y Mantis en rayos X) quedan como referencia: no se usan todavía.

## Pendiente

- Anatomía en rayos X (huesos, órganos) con los módulos de `modules/`.
- Las demás categorías (corteza, esqueleto, etc.) son espacios de diseño sin implantes.

## Qué no se incluyó

- `mesa-matasanos.bbmodel` (composición de referencia, 1,4 MB), `states/` (avatares ensamblados, se regeneran con `source/build_modules.py`) y `combat-source/` (copia de los modelos de combate que ya están en `art/mantis-kit/`).
- `mesa-matasanos-demo.html`: versión anterior de la maqueta; queda `mesa-matasanos-edgerunner-demo.html`.
