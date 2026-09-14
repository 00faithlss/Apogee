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

## Recursos propios

- `assets/apogee/icon.png` y `assets/apogee/textures/gui/slots/os.png`: generados para Apogee.
- Diseño del tooltip: maqueta aprobada del plan (fondo negro translúcido, marco metálico, detalles cian).

Apogee no está afiliado a CD PROJEKT RED ni a los autores citados.
