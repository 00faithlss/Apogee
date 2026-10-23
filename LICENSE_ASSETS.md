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
  - `textures/item/axolotl_item.png` → `assets/apogee/textures/item/control_chip.png` (Chip de control), elegido por el autor del proyecto. Tomado de `project_edgerunner-1.1.2.jar` descargado de [Modrinth](https://modrinth.com/mod/project-edgerunner), SHA-1 `3dc5ef442f443bee2c1c0f79c0b50a301767a7cc`.
  - `textures/item/arms_actuator_matrix_t4_item.png` → `assets/apogee/textures/item/blade_actuator.png` (Actuador de cuchilla), elegido por el autor del proyecto, del mismo JAR.
  - `textures/item/arms_power_core_t4_item.png` → `assets/apogee/textures/item/arm_power_core.png` (Núcleo de potencia de brazos), elegido por el autor del proyecto, del mismo JAR.
  - Bomba de sangre: `geo/blood_pump_model.geo.json` → `assets/apogee/geo/blood_pump.geo.json` (solo cambia el identificador de la geometría), `animations/blood_pump_model.animation.json` → `assets/apogee/animations/blood_pump.animation.json` (solo cambia el prefijo de los nombres de animación a `animation.blood_pump.`), `textures/models/blood_pump.png` → `assets/apogee/textures/blood_pump/blood_pump.png`, `textures/item/blood_pump_item.png` → `assets/apogee/textures/item/blood_pump.png` y `sounds/heal-bloodpump.ogg` → `assets/apogee/sounds/blood_pump.ogg`, sin otros cambios; elegidos por el autor del proyecto, del mismo JAR. La lógica del implante (cura instantánea, regeneración y recarga) es propia: se tomó como referencia solo la mecánica descrita en su guía.
  - `textures/item/hemo_chamber_t2_item.png` → `assets/apogee/textures/item/hemo_chamber.png` (Cámara hemo) y `textures/item/bio_monitor_t2_item.png` → `assets/apogee/textures/item/bio_monitor.png` (Biomonitor), sin cambios; son las piezas con las que Project: Edgerunner arma su Bomba de sangre. Elegidas por el autor del proyecto, del mismo JAR.
  - Camilla del bloque `ripperdoc`: de `geo/ripperdoc.geo.json` solo los huesos `camilla`, `base`, `cojin`, `cables` y `esposas`, recentrados sobre dos bloques → `assets/apogee/geo/ripperdoc_table.geo.json`; `textures/block/ripperdoc.png` sin cambios → `assets/apogee/textures/block/ripperdoc_table.png` (Mesa de matasanos), elegidos por el autor del proyecto, del mismo JAR.
  - Tendones reforzados (`apogee:reinforced_tendons`): el modelo 3D (`models/item/reinforced_tendons.json`, fuente `art/tendons/blockbench/reinforced_tendons.bbmodel`) y su textura (`textures/item/reinforced_tendons_3d.png`) son originales, generados por `art/tendons/blockbench/build_model.py` a partir de una imagen de referencia aportada por el autor del proyecto (solo como guía de forma, sin copiar píxeles). Ya no se usan texturas ni modelos de Project: Edgerunner en este implante. La lógica del doble salto se escribió en Java tomando como referencia el impulso vertical (0,68) y el margen de caída (4 bloques) del script de ese mod.
  - Sonidos de los Tendones reforzados: `sounds/tendons/double_jump.ogg` y `sounds/tendons/air_dash.ogg`, convertidos de `salto.mp3` y `dash.mp3` (recortados a la parte útil, con reducción de ruido, compuerta y normalización a -1 dBFS; sin otros cambios), archivos aportados por el autor del proyecto en la biblioteca. Su origen y licencia no están registrados: pendiente de confirmar con el autor antes de publicar.
- Solo referencia (sin copiar código): duración y colores de la transición radial y del tinte, cadencia, cantidad, alcance y desvanecimiento de las estelas.

## Zoe Isn't Cyberpunk 2.2.5 — vami

- Origen: `zoe-2.2.5-forge-1.20.1.jar`, [Modrinth](https://modrinth.com/mod/zoe-isnt-cyberpunk), SHA-1 `d88b631e5008ed315d1701311a9f0c084ffea589`.
- Licencia declarada en el JAR: Apache License 2.0 (el JAR no trae archivo NOTICE). Texto completo en `src/main/resources/licenses/zoe-isnt-cyberpunk-Apache-2.0.txt` (viaja dentro del JAR).
- Usado sin cambios, como texturas de piezas de las Cuchillas Mantis:
  - `textures/item/flux_wire.png` → `assets/apogee/textures/item/neural_cable.png` (Cable neural).
  - `textures/item/titanium_rod.png` → `assets/apogee/textures/item/deploy_rail.png` (Riel de despliegue).
  - `textures/item/plasma_splicer.png` y su `.mcmeta` (animación de 4 cuadros) → `assets/apogee/textures/item/thermal_blade.png` (Hoja térmica).
  - `textures/item/voidsplitter.png` y su `.mcmeta` (animación de 4 cuadros) → `assets/apogee/textures/item/electric_blade.png` (Hoja eléctrica).

## Cyberware: Reforged — w1shadel

- Origen: [github.com/w1shadel/Cyberware-Reforged](https://github.com/w1shadel/Cyberware-Reforged), commit `e2e59bd311a7ec5cffbd1417f477253206f6266c`. Es un port del Cyberware de Flaxbeard; no se confirmó si su arte original tiene la misma licencia.
- Licencia: MIT, © 2025 w1shadel. Texto completo en `src/main/resources/licenses/cyberware-reforged-MIT.txt` (viaja dentro del JAR).
- Usado sin cambios, elegidos por el autor del proyecto:
  - `assets/cyber_ware_port/textures/item/katana.png` → `assets/apogee/textures/item/mantis_blade.png` (Hoja mantis).
  - `assets/cyber_ware_port/textures/item/component_fiberoptics.png` → `assets/apogee/textures/item/monofilament.png` (Monofilamento).
  - `assets/cyber_ware_port/textures/item/component_plating.png` → `assets/apogee/textures/item/wire_housing.png` (Carcasa de nanocable).
  - `assets/cyber_ware_port/textures/item/component_microelectric.png` → `assets/apogee/textures/item/microelectric_module.png` (Módulo microeléctrico, componente del Biodyne Berserk).
  - `assets/cyber_ware_port/textures/item/component_reactor.png` → `assets/apogee/textures/item/compact_reactor.png` (Reactor compacto, componente del Militech Berserk).

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

## Sonidos de las Cuchillas Mantis eléctricas — Cyberpunk 2077

- Origen: audio del juego Cyberpunk 2077 (CD PROJEKT RED), descargado por el autor del proyecto desde [Cyberpunk 2077 SoundDB](https://sounddb.redmodding.org/), base de datos de la comunidad REDmodding con los sonidos extraídos del juego.
- Licencia: propiedad de CD PROJEKT RED. La página no declara licencia ni permiso de uso; no se ha obtenido permiso de redistribución.
- Usado: seis golpes y un despliegue → `sounds/mantis/electric_hit{1,2,3}.ogg` (los tres más graves, al golpear a un enemigo), `sounds/mantis/electric_swing{1,2,3}.ogg` (corte al aire) y `sounds/mantis/electric_deploy.ogg`.
- Modificaciones: se recortó el silencio inicial, se igualó el volumen y se convirtieron de MP3 estéreo a OGG Vorbis mono.

## Sonidos de las Cuchillas Mantis normales — Cyberpunk 2077

- Origen: audio del juego Cyberpunk 2077 (CD PROJEKT RED), descargado por el autor del proyecto desde [Cyberpunk 2077 SoundDB](https://sounddb.redmodding.org/), la misma fuente que los eléctricos, y reunido en un único MP3 con los sonidos separados por silencios (el primero es el despliegue).
- Licencia: propiedad de CD PROJEKT RED. La página no declara licencia ni permiso de uso; no se ha obtenido permiso de redistribución.
- Usado: el despliegue → `sounds/mantis/normal_deploy.ogg`; cuatro cortes al aire, los de timbre más agudo → `sounds/mantis/normal_swing{1,2,3,4}.ogg`; cuatro golpes, los más graves → `sounds/mantis/normal_hit{1,2,3,4}.ogg`. Los demás cortes del archivo no se usan.
- Modificaciones: se separaron por silencios, se recortó el silencio inicial y final, se igualó el volumen con los eléctricos y se convirtieron a OGG Vorbis mono.

Apogee no está afiliado a CD PROJEKT RED ni a los autores citados.

## Modelos 3D de inventario de las Cuchillas Mantis — diseño propio del proyecto

- Origen: paquete de recursos `mantis-items-resourcepack-1.21.1` aportado por el autor del proyecto: modelos de objeto 3D y sus texturas, hechos a partir de una referencia propia.
- Usado sin cambios: `models/item/mantis_blades{,_thermal,_electric}.json` y `textures/item/mantis_blades{,_thermal,_electric}_3d.png`.

## Kit Mantis v0.4 — diseño propio del proyecto

- Origen: kit `mantis-modelos-fabric-1.21.1` aportado por el autor del proyecto (proyectos de Blockbench, exportaciones GeckoLib, texturas y máscaras emisivas). Diseño fan-made inspirado en las Mantis Blades de Cyberpunk 2077, sin recursos copiados del juego.
- Usado: `geo/mantis_*.geo.json`, `animations/mantis_*.animation.json` y `textures/mantis/*.png`, importados con `tools/mantis_kit_import.py`, que añade la mano y el tope del hombro con el material de piel y renombra las máscaras `_emissive` a `_glowmask`.
- Fuentes editables en `art/mantis-kit/` (proyectos completos en `blockbench/` y piezas de hoja y riel por separado en `modular/`).
- Revisiones posteriores, también del autor del proyecto: metal oscuro, hoja térmica ensanchada, filo eléctrico azul, hoja y riel en grupos independientes. En el repositorio se añadieron el cyberware inferior, el mayor alcance de la hoja y la corrección de fotogramas clave duplicados que impedían cargar el repliegue en GeckoLib.

## Nanocable rojo v0.3 — diseño propio del proyecto

- Origen: paquete `nanocable-rojo-geckolib-assets` aportado por el autor del proyecto: modelos y animaciones generados por código (`art/nanowire/source/build.py`), inspirados en el Monowire de Cyberpunk 2077 sin copiar modelos, texturas ni animaciones del juego.
- Usado: `geo/nanowire_item.geo.json` y `textures/nanowire/nanowire_red{,_glowmask}.png` sin cambios; `geo/nanowire_{left,right}_attachment.geo.json` y `animations/nanowire_{left,right}_attachment.animation.json` sin los huesos del cable, quitados con `tools/nanowire_strip_wire.py` porque el mod dibuja el cable por código. Detalle en `art/nanowire/NOTA.md`.

## Sonidos del Nanocable

- Origen: audio del juego Cyberpunk 2077 (CD PROJEKT RED), descargado por el autor del proyecto desde [Cyberpunk 2077 SoundDB](https://sounddb.redmodding.org/), la misma fuente que los de las Cuchillas Mantis. Llegó en cuatro MP3: despliegue, guardado, golpes al aire y golpes (cuatro sonidos cada uno, separados por silencios).
- Convertidos a Ogg Vorbis mono de 48 kHz, recortados con un fundido de salida y con el pico normalizado a -1 dB.
- Usado:
  - `sounds/nanowire/deploy.ogg` (despliegue) y `sounds/nanowire/retract.ogg` (guardado);
  - `sounds/nanowire/swing{1,2,3,4}.ogg` (latigazo al aire);
  - `sounds/nanowire/hit{1,2,3,4}.ogg` (golpe al enemigo); el tercero, el de más cuerpo, también es el latigazo doble.

## Mesa de matasanos — diseño propio del proyecto

- Origen: paquete `mesa-matasanos-edgerunner-assets`, aportado por el autor del proyecto: diseño propio de la pantalla, inspirado en la galería pública de Project: Edgerunner sin copiar sus texturas ni su código. Fuentes en `art/ripperdoc/` (ver `art/ripperdoc/NOTA.md`).
- Usado en el juego: ninguna textura. La pantalla actual se dibuja con la textura de cofre de Minecraft y con los colores y esquinas del diseño; `textures/mesa_matasanos_gui.png` queda como referencia en `art/ripperdoc/`.
- Íconos de las ranuras `textures/gui/slots/{cortex,skeleton,nerves,optics,circulatory,legs}.png`: los `textures/item-*.png` del paquete pasados a gris. `hands.png` e `integumentary.png` se dibujaron para el mod con el mismo estilo; son originales del proyecto.
