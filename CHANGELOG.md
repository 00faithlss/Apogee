# Changelog

Registro de cambios de Apogee. Las entradas describen trabajo completado.

## [Unreleased]

### Añadido (Cuchillas Mantis)

- Implante «Cuchillas Mantis» en la nueva ranura Trinkets `cyberware/arms`, con receta con forma simétrica y logro de desafío «Mantis religiosa». Variantes **térmica** (prende fuego al objetivo) y **eléctrica** (lo aturde un momento), que se fabrican rodeando las Cuchillas Mantis con varas de blaze y bloques de magma, o con bloques de redstone y pararrayos.
- Tecla R (configurable) para desplegar o guardar las cuchillas. Clic izquierdo: cortes alternados, un brazo distinto en cada golpe (+6 de daño y +1,5 de velocidad de ataque mientras están desplegadas). Clic derecho: estocada con ambas cuchillas, con impulso, daño a lo largo de 4 bloques y recarga de 2 s. El servidor valida el despliegue, el daño y la recarga.
- Con las cuchillas desplegadas la ranura de la hotbar queda fija (rueda y teclas 1-9), el inventario no se abre y el clic derecho no usa objetos. El servidor también fija la ranura y cierra contenedores.
- Modelo y animaciones del kit Mantis v0.4 (GeckoLib, una geometría por variante y lado, 25 huesos): cerrado, el brazo se ve como uno normal; al desplegar se abren las cubiertas superiores e inferiores, sube el mecanismo por el costado externo del antebrazo y se extiende la hoja telescópica. Animaciones de despliegue y repliegue (1 s) y de ataque por brazo (0,6 s).
- El modelo reemplaza el brazo vanilla mientras se lleva el implante: los paneles de piel, la mano y el tope del hombro se dibujan con la skin del portador (clásica o Slim), proyectando cada panel sobre la zona del brazo que cubre al estar cerrado.
- Tercera persona con postura de combate (brazos separados 20° del torso) y primera persona con ambas cuchillas desplegadas; plegadas se ve el brazo normal.
- Máscaras emisivas de las variantes térmica y eléctrica, activas solo con el implante abierto, y partículas de brasas o chispas al atacar.
- GeckoLib 4.9.3 como dependencia obligatoria que se instala aparte (no va dentro del JAR).
- `tools/mantis_kit_import.py` importa las exportaciones del kit al mod; los proyectos de Blockbench y la documentación del kit quedan en `art/mantis-kit/`.
- Prueba de cliente `runMantisSmoke`: equipa cada variante y captura plegada, desplegando, desplegada, atacando, primeros planos y primera persona.
- `tools/mantis_tests.sh`: prueba multijugador (servidor dedicado y dos clientes que comprueban la sincronización de variante, despliegue, ataques y repliegue) y prueba de compatibilidad con Punchy y Fresh Animations (con Entity Model Features y Entity Texture Features).
- Con packs que animan los brazos, como Fresh Animations Player Extension, el implante abierto recibe igualmente la separación de 20° del torso para que sus cubiertas inferiores no lo atraviesen.
- Sonidos provisionales (reutilizan eventos de Minecraft) hasta contar con grabaciones propias.

### Cambiado

- El +50 % de movimiento sube de forma gradual durante 2 s (`movementRampSeconds`) para suavizar el cambio de FOV.
- Los objetivos ralentizados reciben el 30 % del empuje (`slowedKnockbackPercent`), para que no floten al golpearlos.
- El tooltip del Sandevistan usa la fuente Rajdhani, con el título más grande.
- Las recetas de Núcleo T5, Overdrive Kernel T5 y Quantum Sync Lattice T5 pasan a ser con forma, con un patrón simétrico (material principal en esquinas, ingrediente clave al centro).

### Corregido

- Al guardar una entidad se descartan los efectos de poción no registrados (p. ej. laceración de otro mod) que hacían crashear el guardado NBT.
- El bonus de velocidad del Sandevistan ya no abre el FOV (se ignora en el cálculo de campo de visión de Minecraft).
- Compatibilidad con Cinematic Zoom: el ajuste de FOV de Apogee se encadena con el de otros mods en lugar de cancelarlos, así que el zoom vuelve a funcionar.

### Añadido

- Al craftear el Sandevistan suena para todos los jugadores del servidor la frase «A rudimentary implant» de Adam Smasher.
- Logro de desafío (marco morado) «Un implante rudimentario» (frase de Adam Smasher en Cyberpunk: Edgerunners) al craftear el Sandevistan, en una pestaña propia de Apogee que se desbloquea al conseguir cualquier componente.
- Compatibilidad opcional con Immersive Armors: dash aéreo hacia delante con los pantalones Steampunk al pulsar la tecla de correr en el aire (uno por salto), con vapor y sonido sincronizados.
- HUD rediseñado estilo ciberware de Cyberpunk 2077: recuadro abajo a la izquierda con el icono del implante centrado, segundos restantes a su derecha en el color del estado y relleno que baja (cian activo, magenta en recarga). Borde amarillo con «+2 s» al conseguir una baja, parpadeo rojo en los últimos 2 s, franja amarilla con el tiempo extra por bajas, y oculto cuando el implante no está en uso.
- Borde del tooltip con arcos eléctricos animados en amarillo, cian y magenta.
- Tooltip animado: título ondulado con degradado y estelas de colores (como el efecto del Sandevistan), partículas neón cayendo por el fondo y scanlines con un barrido cian.
- Prueba `staggeredWearersKeepIndependentTimers`: confirma la regla con varios portadores (bonus e inmunidad mientras el efecto propio está activo, temporizadores independientes, ralentización al terminar).

### Pendiente

- Confirmar en una partida real la regla con varios portadores (ya cubierta por pruebas automáticas).
- Revisión del tooltip en juego con la nueva fuente y el borde eléctrico.

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
