# Changelog

Registro de cambios de Apogee. Las entradas describen trabajo completado.

## [Unreleased]

### Cambiado

- El +50 % de movimiento sube de forma gradual durante 2 s (`movementRampSeconds`) para suavizar el cambio de FOV.
- Los objetivos ralentizados reciben el 30 % del empuje (`slowedKnockbackPercent`), para que no floten al golpearlos.
- El tooltip del Sandevistan usa la fuente Rajdhani, con el título más grande.

### Añadido

- Borde del tooltip con arcos eléctricos animados en amarillo, cian y magenta.
- Prueba `staggeredWearersKeepIndependentTimers`: confirma la regla con varios portadores (bonus e inmunidad mientras el efecto propio está activo, temporizadores independientes, ralentización al terminar).

### Pendiente

- Confirmar en una partida real la regla con varios portadores (ya cubierta por pruebas automáticas).
- Revisión del tooltip en juego con la nueva fuente y el borde eléctrico.
- Rediseño de las barras de uso y de recarga.

## [0.1.0+1.21.1] — 2026-09-14

### Añadido

- Proyecto Gradle/Fabric (Loom 1.17, Gradle 9.5.1, Mojang mappings) con identificador `apogee`.
- Sandevistan como implante Trinkets en la ranura `cyberware/os`, sin durabilidad y no apilable, más Núcleo T5, Overdrive Kernel T5 y Quantum Sync Lattice T5 apilables, con sus recetas.
- Activación con V validada por el servidor: 10 s, +50 % de movimiento, +25 % de daño cuerpo a cuerpo y de proyectiles lanzados, +2 s por baja con máximo de 20 s y recarga persistente de 30 s desde el final.
- Campo de 10 bloques con tiempo selectivo por entidad al 15 % (servidor y cliente), selección de jugadores y combatientes, proyectiles propios y de combatientes, exclusión de dispensadores, inmunidad entre portadores activos y limpieza al morir, desequipar, cambiar de dimensión o desconectar.
- Sincronización con Fabric Networking de reglas del anfitrión, estado del portador y campos por dimensión.
- Modelo 3D del implante portado a Fabric, visible en la espalda, como objeto y en el tooltip.
- Tooltip de Simply Tooltips con proveedor y tema propios, vistas Descripción/Estadísticas con G y valores sincronizados.
- Audio de entrada/salida, transición radial, tinte, pulso de FOV, HUD de duración/recarga y estelas arcoíris con pose, skin y equipo.
- Configuración `apogee-server.json` y `apogee-client.json`.
- Traducciones en inglés y español (es_es, es_cl, es_mx, es_ar).
- GameTests de mecánica, prueba automatizada de cliente con capturas y medición de coste del campo.
- Procedencia de recursos en `LICENSE_ASSETS.md`.

### Cambiado

- Alcance reducido a un único implante, Sandevistan; se descartan Gorilla Arms, Ripperdoc, variantes y mejoras.

## Documentación previa — 2026-09-11 y 2026-09-12

- Investigación inicial de mods cyberpunk y diseño preliminar de Gorilla Arms y Sandevistan, sustituidos por el alcance de Apogee.
