# Procedencia de recursos

Apogee es una entrega **privada y local**. Los recursos de terceros listados aquí se incluyen solo para ese uso; no se han obtenido permisos de redistribución y el JAR no debe publicarse.

## Sandevistan (Cyperpunk) 1.2.0 — Samuelchr

- Origen: `sandevistan-1.2.0-neoforge-1.21.1.jar`, [Modrinth](https://modrinth.com/mod/sandevistan-cyperpunk), SHA-1 `91b93e17961592cbc1b89329d58f142c04e486be`.
- Licencia declarada en el JAR: `Not specified`.
- Usado:
  - Geometría del implante (`SandevistanChestplateModel#createBodyLayer`): tamaños, posiciones, escalas y distribución UV 128×128 copiados en `dev.apogee.client.model.SandevistanModel`.
  - `textures/models/armor/sandevistan_layer_1.png` → `assets/apogee/textures/entity/sandevistan.png` (sin cambios).
  - `textures/models/armor/sandevistan_cyan_layer_1.png` → `assets/apogee/textures/entity/sandevistan_overdrive.png` (sin cambios).
- Modificaciones: el cableado NeoForge/Curios se sustituyó por renderizado nativo de Fabric (Trinkets, renderizador de objeto y tooltip). Las estelas se reimplementaron tomando como referencia su lógica de captura de pose.

## Project: Edgerunner 1.1.2 — A24-N22

- Origen: `project_edgerunner-1.1.2.jar` (perfil local de Modrinth App).
- Licencia declarada en el JAR: `All Rights Reserved - Modpack use permitted`. Modrinth indica CC-BY-NC-SA-4.0; las declaraciones no coinciden.
- Usado sin cambios:
  - `textures/item/os_core_t5_item.png` → `assets/apogee/textures/item/os_core_t5.png`.
  - `textures/item/apogee_overdrive_kernel_t5_item.png` → `assets/apogee/textures/item/apogee_overdrive_kernel_t5.png`.
  - `textures/item/quantum_sync_lattice_t5_item.png` → `assets/apogee/textures/item/quantum_sync_lattice_t5.png`.
  - `sounds/sandevistan_activation.ogg` → `assets/apogee/sounds/sandevistan_activate.ogg`.
  - `sounds/sandevistan_exit.ogg` → `assets/apogee/sounds/sandevistan_deactivate.ogg`.
- Solo referencia (sin copiar código): duración y colores de la transición radial y del tinte, cadencia, cantidad, alcance y desvanecimiento de las estelas.

## Rajdhani SemiBold — Indian Type Foundry

- Origen: [google/fonts](https://github.com/google/fonts/tree/main/ofl/rajdhani), `ofl/rajdhani/Rajdhani-SemiBold.ttf`.
- Licencia: SIL Open Font License 1.1 (redistribución permitida). Texto completo en `assets/apogee/font/rajdhani_ofl.txt`.
- Usado sin cambios como `assets/apogee/font/rajdhani_semibold.ttf` en el tooltip del Sandevistan.

## Frase «A rudimentary implant»

- Origen: `a-rudimentary-implant-made-with-Voicemod.mp3`, aportado por el usuario (creado con Voicemod), en referencia a la frase de Adam Smasher en *Cyberpunk: Edgerunners* (CD PROJEKT RED / Studio Trigger).
- Licencia: no especificada; solo uso privado.
- Convertido a Ogg Vorbis mono 44,1 kHz, sin silencios al inicio y al final y con volumen normalizado → `assets/apogee/sounds/rudimentary_implant.ogg`.

## Recursos propios

- `assets/apogee/icon.png` y `assets/apogee/textures/gui/slots/os.png`: generados para Apogee.
- Diseño del tooltip: maqueta aprobada del plan (fondo negro translúcido, marco metálico, detalles cian).

Apogee no está afiliado a CD PROJEKT RED ni a los autores citados.

## Kit Mantis v0.4 — diseño propio del proyecto

- Origen: kit `mantis-modelos-fabric-1.21.1` aportado por el autor del proyecto (proyectos de Blockbench, exportaciones GeckoLib, texturas y máscaras emisivas). Diseño fan-made inspirado en las Mantis Blades de Cyberpunk 2077, sin recursos copiados del juego.
- Usado: `geo/mantis_*.geo.json`, `animations/mantis_*.animation.json` y `textures/mantis/*.png`, importados con `tools/mantis_kit_import.py`, que añade la mano y el tope del hombro con el material de piel y renombra las máscaras `_emissive` a `_glowmask`.
- Fuentes editables en `art/mantis-kit/` (proyectos completos en `blockbench/` y piezas de hoja y riel por separado en `modular/`).
- Sonidos de la variante eléctrica (`sounds/mantis/electric_*.ogg`): aportados por el autor del proyecto como MP3 estéreo; se recortó el silencio inicial, se igualó el volumen y se convirtieron a OGG Vorbis mono. Origen y licencia por confirmar.
- Revisiones posteriores, también del autor del proyecto: metal oscuro, hoja térmica ensanchada, filo eléctrico azul, hoja y riel en grupos independientes. En el repositorio se añadieron el cyberware inferior, el mayor alcance de la hoja y la corrección de fotogramas clave duplicados que impedían cargar el repliegue en GeckoLib.

