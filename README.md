# Apogee

Mod privado para **Minecraft Java 1.21.1**, **Fabric** y **Java 21**, pensado para partidas con amigos mediante **Essential** (también funciona en individual, LAN y servidor dedicado). Identificador: `apogee`.

Contiene tres implantes, el **Sandevistan**, las **Cuchillas Mantis** y el **Nanocable**, tres componentes T5 y diez piezas para armar el ciberware de brazos.

> Estado: primera versión compilable y probada de forma automatizada. La compatibilidad con Essential **no está comprobada** hasta completar la sesión real descrita en [los resultados de pruebas](docs/testing/2026-09-14-apogee-resultados.md).

## Contenido

| ID | Objeto | Notas |
|---|---|---|
| `apogee:sandevistan` | Sandevistan | Implante reutilizable, sin durabilidad, no apilable. Ranura Trinkets `cyberware/os`. |
| `apogee:mantis_blades` | Cuchillas Mantis | Implante reutilizable, sin durabilidad, no apilable. Ranura Trinkets `cyberware/arms`. |
| `apogee:mantis_blades_thermal` | Cuchillas Mantis térmicas | Variante que prende fuego al objetivo. |
| `apogee:mantis_blades_electric` | Cuchillas Mantis eléctricas | Variante que aturde al objetivo. |
| `apogee:nanowire` | Nanocable | Implante reutilizable, sin durabilidad, no apilable. Ranura Trinkets `cyberware/arms`. |
| `apogee:os_core_t5` | Núcleo T5 | Componente apilable. |
| `apogee:apogee_overdrive_kernel_t5` | Overdrive Kernel T5 | Componente apilable. |
| `apogee:quantum_sync_lattice_t5` | Quantum Sync Lattice T5 | Componente apilable. |
| `apogee:neural_cable` | Cable neural | Pieza de las Cuchillas Mantis; la usan casi todas las demás. |
| `apogee:control_chip` | Chip de control | Pieza del actuador. |
| `apogee:blade_actuator` | Actuador de cuchilla | Abre las cubiertas y empuja la hoja. |
| `apogee:deploy_rail` | Riel de despliegue | Guía la hoja por el antebrazo. |
| `apogee:mantis_blade` | Hoja mantis | Hoja de las Cuchillas Mantis normales y base de las otras dos. Apila hasta 16. |
| `apogee:thermal_blade` | Hoja térmica | Hoja de las Cuchillas Mantis térmicas. Apila hasta 16. |
| `apogee:electric_blade` | Hoja eléctrica | Hoja de las Cuchillas Mantis eléctricas. Apila hasta 16. |
| `apogee:arm_power_core` | Núcleo de potencia de brazos | Alimenta las Cuchillas Mantis y el Nanocable. |
| `apogee:monofilament` | Monofilamento | El cable del Nanocable. |
| `apogee:wire_housing` | Carcasa de nanocable | Carcasa del antebrazo que guarda y lanza el cable. |

### Recetas (mesa de crafteo)

| Resultado | Ingredientes |
|---|---|
| Núcleo T5 (con forma) | 4 lingotes de netherita, 4 bloques de redstone, 1 comparador: netherita en esquinas, redstone en los lados, comparador al centro |
| Overdrive Kernel T5 (con forma) | 2 estrellas del Nether, 4 lingotes de netherita, 2 bloques de diamante, 1 tótem de inmortalidad: netherita en esquinas, estrellas arriba y abajo, diamante a los lados, tótem al centro |
| Quantum Sync Lattice T5 (con forma) | 2 cristales del End, 2 bloques de amatista, 1 frasco de experiencia, 4 bloques de redstone: redstone en esquinas, cristales arriba y abajo, amatista a los lados, frasco al centro |
| Sandevistan (con forma) | Fila 1: bloque de diamante, Núcleo T5, bloque de diamante · Fila 2: lingote de netherita, Overdrive Kernel T5, lingote de netherita · Fila 3: bloque de diamante, Quantum Sync Lattice T5, bloque de diamante |

#### Cuchillas Mantis por partes

Todas con forma; dan 1 unidad salvo el cable neural. Se arman en este orden: cables y chips, mecanismos, hojas e implante.

| Resultado | Fila 1 · Fila 2 · Fila 3 |
|---|---|
| 4 Cables neurales | cobre, redstone, cobre · redstone, lingote de oro, redstone · cobre, redstone, cobre |
| Chip de control | cuarzo, redstone, cuarzo · cable neural, fragmento de amatista, cable neural · cuarzo, redstone, cuarzo |
| Actuador de cuchilla | hierro, cable neural, hierro · cable neural, bloque de redstone, cable neural · hierro, chip de control, hierro |
| Riel de despliegue | hierro, cable neural, hierro · hierro, actuador de cuchilla, hierro · hierro, cable neural, hierro |
| Hoja mantis | vacío, hierro, vacío · hierro, espada de netherita, hierro · vacío, lingote de netherita, vacío |
| Hoja térmica | polvo de blaze, bloque de magma, polvo de blaze · bloque de magma, hoja mantis, bloque de magma · polvo de blaze, cable neural, polvo de blaze |
| Hoja eléctrica | bloque de redstone, pararrayos, bloque de redstone · cobre, hoja mantis, cobre · bloque de redstone, cable neural, bloque de redstone |
| Núcleo de potencia de brazos | chatarra de netherita, bloque de redstone, chatarra de netherita · cable neural, estrella del Nether, cable neural · chatarra de netherita, bloque de redstone, chatarra de netherita |
| Cuchillas Mantis | hoja mantis, núcleo de potencia de brazos, hoja mantis · riel de despliegue, Núcleo T5, riel de despliegue · actuador de cuchilla, cable neural, actuador de cuchilla |
| Cuchillas Mantis térmicas | igual, con dos hojas térmicas |
| Cuchillas Mantis eléctricas | igual, con dos hojas eléctricas |

#### Nanocable

| Resultado | Fila 1 · Fila 2 · Fila 3 |
|---|---|
| Monofilamento | hilo, diamante, hilo · hilo, cable neural, hilo · hilo, diamante, hilo |
| Carcasa de nanocable | hierro, lingote de netherita, hierro · hierro, actuador de cuchilla, hierro · hierro, chip de control, hierro |
| Nanocable | carcasa de nanocable, monofilamento, carcasa de nanocable · cable neural, núcleo de potencia de brazos, cable neural · cable neural, Núcleo T5, cable neural |

Todas con forma. El cable neural, el chip de control, el actuador de cuchilla y el núcleo de potencia salen de las recetas de las Cuchillas Mantis.

### Logros

- Pestaña **Apogee**: aparece al conseguir cualquier componente, pieza o implante.
- **Un implante rudimentario** (desafío, marco morado): conseguir el Sandevistan de cualquier forma (crafteándolo, de un cofre o de otro jugador). Muestra el aviso de desafío completado y se anuncia en el chat.
- **Mantis religiosa** (desafío): craftear cualquiera de las tres Cuchillas Mantis.
- **Pendiendo de un hilo** (desafío): craftear el Nanocable.
- Cuando un jugador consigue el Sandevistan por primera vez (al completar ese logro, sea crafteándolo, de un cofre o de otro jugador), todos los jugadores del servidor oyen a Adam Smasher decir «A rudimentary implant», estén donde estén.

## Uso

- Equipa el Sandevistan en la ranura **Sistema operativo** de Trinkets (la pechera y los élitros siguen libres).
- **V** (configurable) activa el efecto o lo termina antes de tiempo.
- Mientras está activo o recargando aparece un recuadro de ciberware abajo a la izquierda con los segundos restantes: cian con el efecto, magenta en la recarga, amarillo con «+2 s» al conseguir una baja y parpadeo rojo en los últimos 2 s. En reposo se oculta.
- En el tooltip, **G** (tecla de pestañas de Simply Tooltips) alterna Descripción y Estadísticas; al dejar de señalar el objeto vuelve a Descripción. El tooltip usa la fuente Rajdhani, un borde con arcos eléctricos en amarillo, cian y magenta, título ondulado con estelas de colores, partículas neón y scanlines con barrido.

### Cuchillas Mantis

- Equípalas en la ranura **Brazos** de Trinkets. **R** (configurable) las despliega o las guarda.
- Cerradas, el brazo se ve como uno normal con tu skin (clásica o Slim). Al desplegarlas se abren las cubiertas del antebrazo, el mecanismo sube por el costado externo y la hoja se extiende.
- Desplegadas: **clic izquierdo** da cortes alternados (derecho, izquierdo, derecho...) y **clic derecho** hace una estocada con ambas cuchillas hacia adelante (recarga de 2 s).
- Mientras están fuera la ranura de la hotbar queda fija (sin rueda ni teclas 1-9), el inventario no se abre y el objeto en mano se oculta.
- Daño con las cuchillas fuera: **10 por golpe** (15 con golpe crítico) y **2 ataques por segundo**; la estocada hace 9 a todo lo que esté en su línea (4 bloques). El objeto que quede en la mano no suma daño, velocidad, encantamientos, barrido ni pierde durabilidad; los efectos como Fuerza sí cuentan.
- No se pueden encantar (ni en la mesa, ni con libros en el yunque, ni con `/enchant`).
- Variantes: la térmica prende fuego 4 s y la eléctrica aplica Lentitud III 1,5 s y hace **+4 de daño** si el objetivo está en el agua. La hoja térmica brilla al rojo vivo, con el filo difuminado a blanco, destellos de glitch y partículas de calor; el filo y los electrodos de la eléctrica brillan en azul y sueltan pequeños rayos mientras el implante está abierto. Con shaders, el brillo recibe el bloom del shader.
- Tooltip al estilo de las armas de Cyberpunk 2077 (con Simply Tooltips): fondo rojo translúcido, marco rojo salmón con la esquina y el detalle cian del juego, textos amarillos, daño por golpe y ataques por segundo en grande junto al tipo de daño (físico, térmico o eléctrico). **G** alterna la vista Descripción (controles y habilidad de la variante) y Estadísticas. Las teclas que muestra (la de desplegar y la de pestañas) siguen la configuración de controles del juego. Sin Simply Tooltips se muestra un tooltip simple.
- Sonidos propios de despliegue, corte al aire y golpe para la normal (que la térmica comparte) y para la eléctrica; la estocada suena como un corte con un golpe más grave encima (eléctricos en la eléctrica). Guardar es provisional; para cambiarlos hay que reemplazar los eventos `mantis_*` en `assets/apogee/sounds.json` por grabaciones `.ogg`.

### Nanocable

- Equípalo en la ranura **Brazos** de Trinkets (la misma de las Cuchillas Mantis). **R** (configurable) saca el cable o lo recoge.
- La carcasa del cable va en la cara interna de cada antebrazo; guardada, queda casi oculta junto al torso. Fuera, el cable rojo brillante va tenso de una mano a la otra.
- Desplegado: **clic izquierdo** da latigazos alternados (derecho, izquierdo...) de **8 de daño** con **5 bloques** de alcance. **Clic derecho** da un latigazo doble que golpea a todo lo que haya delante, en un arco de 140° dentro del alcance (**12 de daño**, recarga de 3 s). Valores provisionales.
- Bloquea la hotbar y el inventario igual que las Cuchillas Mantis, y tampoco se puede encantar.
- Usa la misma tarjeta de tooltip que las Cuchillas Mantis: daño, ataques por segundo y tipo en grande; **G** alterna los controles y las estadísticas del latigazo doble.
- Sonidos provisionales de Minecraft hasta tener grabaciones propias (eventos `nanowire_*` en `assets/apogee/sounds.json`).

### Mecánica del Sandevistan (valores por defecto)

| Regla | Valor |
|---|---|
| Ralentización | 85 % (entidades afectadas avanzan al 15 %) |
| Radio | 10 bloques, esfera centrada en el portador, sin línea de visión |
| Duración | 10 s |
| Movimiento | +50 %, alcanzado de forma gradual en 2 s; no altera el FOV |
| Daño cuerpo a cuerpo y proyectiles lanzados durante la activación | +25 % (los proyectiles conservan el bonus hasta impactar) |
| Por baja | +2 s, con máximo absoluto de 20 s desde el inicio |
| Recarga | 30 s desde que termina (también al terminar manualmente) |
| Empuje sobre objetivos ralentizados | 30 % del normal, para que no queden flotando al golpearlos |

- **Afectados:** jugadores en supervivencia/aventura y monstruos que tienen al portador como objetivo o intercambiaron daño con él en los últimos 10 s (también mobs neutrales que lo atacan). Ser amigo en Essential no da inmunidad.
- **No afectados:** el portador, otros portadores activos, animales, aldeanos, gólems y monstruos ajenos al combate, bloques, redstone, tolvas y generación de mobs.
- **Varios portadores:** quien tiene su Sandevistan **activo** recibe el bonus de velocidad y daño y no sufre ralentización, aunque esté dentro del campo de otro. Cada portador lleva su propio temporizador y los campos no se acumulan. Ejemplo: A activa; a los 6 s activa B, que deja de estar ralentizado y cuenta sus 10 s propios; cuando termina el efecto de A, A pasa a ralentizarse por el campo de B hasta que B termine.
- **Proyectiles:** los del portador y de los combatientes seleccionados se ralentizan dentro del campo; los de dispensadores (sin dueño), no.
- **Bajas que extienden:** muertes de enemigos afectados causadas directamente por el portador (ataque o proyectil). Animales y muertes sin atribución directa no cuentan.
- **Fin y limpieza:** morir, desequipar, cambiar de dimensión, desconectarse o cerrar el mundo termina el efecto, inicia la recarga y quita modificadores y estado temporal.
- La recarga se guarda con el jugador y usa el tiempo de juego del mundo: se pausa con la pausa individual y continúa mientras la sesión multijugador sigue en marcha.

### Cómo funciona el tiempo selectivo

Cada entidad lleva un acumulador temporal. Mientras está ralentizada solo ejecuta su tick real cuando el acumulador supera 1 (cada ~6,7 ticks con 15 %), así que movimiento, IA, cadencia de ataque, uso de objetos y animaciones van al 15 % sin cambiar el ritmo del mundo. El cliente replica el mismo salto de ticks e interpola con el progreso del acumulador para que el movimiento se vea continuo. Cámara, menús y conexión no dependen de esos ticks y siguen fluidos. No se usan pociones de lentitud.

El servidor (integrado del anfitrión o dedicado) valida la tecla, decide objetivos, daño y temporizadores, y sincroniza con Fabric Networking al entrar, cambiar de dimensión, reaparecer y cada vez que cambia el campo.

## Compatibilidad con Immersive Armors

Si Immersive Armors está instalado, los **pantalones Steampunk** (que ya dan doble salto) ganan un **dash aéreo hacia delante**:

- Pulsa la tecla de correr (Ctrl izquierdo por defecto) estando en el aire. Si venías manteniéndola desde el suelo, hay que soltarla y volver a pulsarla.
- Un dash por salto: se recarga al tocar el suelo y es independiente del doble salto.
- Impulsa hacia donde miras, con un leve empuje hacia arriba, y anula el daño por caída acumulado.
- Vapor y sonido del propulsor, visibles para los demás jugadores.
- En Controles hay una tecla opcional «Dash aéreo (pantalones Steampunk)», sin asignar, por si prefieres otra.
- Sin Immersive Armors esta función no se carga.

## Pendientes

- **Varios portadores en juego:** la regla ya está comprobada con pruebas automáticas (ver *Varios portadores*); falta confirmarla en una partida real con Essential.
- **Tooltip en juego:** revisar dentro de Minecraft la fuente Rajdhani y el borde eléctrico en distintas escalas de interfaz y en español.
- **Nanocable:** faltan sonidos propios; los valores de combate son provisionales.

## Instalación para anfitrión e invitados

Todos deben usar **exactamente** estas versiones:

| Mod | Versión | Necesario en |
|---|---|---|
| Apogee | 0.6.0+1.21.1 (`build/libs/apogee-0.6.0+1.21.1.jar`) | Todos |
| Fabric Loader | 0.16.0 o superior (probado con 0.19.3) | Todos |
| Fabric API | 0.116.17+1.21.1 | Todos |
| Trinkets | 3.10.0 (incluye Cardinal Components) | Todos |
| GeckoLib | 4.9.3 para Fabric 1.21.1 | Todos (obligatorio, se instala aparte) |
| Simply Tooltips | 0.1.5 | Clientes |
| Architectury API | 13.0.11+fabric | Clientes (dependencia de Simply Tooltips) |
| Fzzy Config | 0.7.6+1.21 | Clientes (dependencia de Simply Tooltips) |
| Fabric Language Kotlin | 1.13.13+kotlin.2.4.10 | Clientes (dependencia de Fzzy Config) |

No se requiere Simply Swords. Un servidor dedicado solo necesita Apogee, Fabric API, Trinkets y GeckoLib. Sin Simply Tooltips el cliente arranca y muestra un tooltip simple.

**Con Essential:** el anfitrión abre su mundo con *Host World* de Essential ([guía oficial](https://essential.gg/wiki/start-hosting)); el mundo se ejecuta en su ordenador, así que su `config/apogee-server.json` define las reglas. Los invitados deben tener la misma lista de mods antes de unirse.

## Configuración

- `config/apogee-server.json` (quien aloja el mundo): porcentaje de ralentización, radio, duraciones en segundos, bonificaciones, rampa de velocidad (`movementRampSeconds`), extensión por baja, máximo, recarga, memoria de combate y empuje sobre ralentizados (`slowedKnockbackPercent`). Se lee al abrir el mundo; los clientes reciben estos valores y el tooltip los muestra.
- `config/apogee-client.json` (cada jugador): estelas (cantidad 28, alcance 48 bloques, desvanecimiento 5,2 s, separación, opacidad), volumen, tinte, transición radial, pulso de FOV y HUD.

## Desarrollo

```bash
./gradlew build
```

```bash
./gradlew runGametest
```

```bash
./gradlew runSmokeClient
```

- `build`: genera el JAR de entrega en `build/libs`. Las pruebas no se incluyen en él.
- `runGametest`: 23 pruebas de servidor con jugadores simulados (`src/gametest`), informe en `build/gametest/report.xml`.
- `runSmokeClient`: abre el cliente, crea un mundo plano, equipa el implante, captura tooltips (Sandevistan, Cuchillas Mantis y Nanocable), activación, HUD y estelas en `build/smoke-client/screenshots` y se cierra.

```bash
./gradlew runMantisSmoke
```

- `runMantisSmoke`: abre el cliente, equipa cada variante de las Cuchillas Mantis y captura plegada, desplegando, desplegada, atacando, primeros planos y primera persona (también caminando) en `build/mantis-smoke/screenshots`.

```bash
./gradlew runNanowireSmoke
```

- `runNanowireSmoke`: abre el cliente, equipa el Nanocable y filma cada tick de los latigazos y del latigazo doble, en tercera y primera persona, y el repliegue, en `build/nanowire-smoke/screenshots`.

```bash
tools/mantis_tests.sh mp
tools/mantis_tests.sh compat
tools/mantis_tests.sh iris
tools/mantis_tests.sh motion
tools/mantis_tests.sh calibrate
```

- `mp`: servidor dedicado y dos clientes (`Observer` y `Wearer`). El servidor equipa al portador y dirige despliegue, corte, estocada y repliegue; cada cliente comprueba lo que ve y captura en `build/mantis-mp/<cliente>/screenshots`.
- `compat`: `runMantisSmoke` con Punchy 2.8c, Entity Model Features 3.3.9, Entity Texture Features 7.2.4 y Fresh Animations 1.10.4 con sus packs *Extensions* y *Player Extension*, descargados en `build/mantis-compat`.
- `motion`: captura cada tick de la primera persona (quieto, caminando, deteniéndose y guardando las cuchillas), en vanilla y con Punchy, en `build/mantis-motion`; con `MOTION_SPRINT=true`, corriendo, y con `MOTION_ITEM=true`, desplegando y guardando con una espada en la mano. `python3 tools/mantis_motion.py` resume el movimiento de las manos.
- `calibrate`: ajusta la pose en que termina el repliegue sobre la mano que aparece después (vanilla o Punchy) y la registra en `build/mantis-calibrate/<caso>/run.out`.
- `iris`: `runMantisSmoke` con Sodium 0.8.13, Iris 1.8.14 y el shader MakeUp UltraFast 9.5e, en una instancia como la de un launcher armada en `build/mantis-iris` (Iris no arranca en el entorno de desarrollo).
- Todos requieren Linux con Xvfb.

El modelo de las Cuchillas Mantis viene del kit Mantis v0.4: los proyectos de Blockbench y su documentación están en [`art/mantis-kit/`](art/mantis-kit/LEEME.md). Tras exportar desde Blockbench con el flujo de GeckoLib, `python3 tools/mantis_kit_import.py <carpeta del kit>` copia geometrías, animaciones y texturas a `assets/apogee` y añade la mano y el tope del hombro que el kit deja fuera de su exportación. Los recursos actuales ya incluyen revisiones posteriores al kit v0.4 (cyberware inferior, hoja y riel separados, alcance de la hoja, colores); volver a importar el kit v0.4 las desharía.

El Nanocable usa el paquete `nanocable-rojo-geckolib-assets` v0.3: fuentes y notas en [`art/nanowire/`](art/nanowire/NOTA.md). El mod dibuja el cable por código (`dev.apogee.client.nanowire`), así que `python3 tools/nanowire_strip_wire.py <paquete>/src/main/resources/assets/apogee` copia las exportaciones de cada brazo sin los huesos del cable.

Estructura principal: `dev.apogee.sandevistan.SandevistanManager` y `dev.apogee.mantis.MantisManager` (autoridad del servidor), `dev.apogee.client.mantis` (modelo GeckoLib de las Cuchillas Mantis), `dev.apogee.client.nanowire` (carcasas, brazos y cable del Nanocable), `dev.apogee.mixin` (salto de ticks, daño, anti-vuelo), `dev.apogee.client` (estado sincronizado, efectos, estelas, renderizado y tooltip).

Ramas: `main` estable, `develop` integración. Commits con Conventional Commits. Cambios en [CHANGELOG.md](CHANGELOG.md).

## Recursos y licencias

El modelo y las texturas del implante proceden de Sandevistan (Cyperpunk) 1.2.0; las texturas de componentes y los sonidos, de Project: Edgerunner 1.1.2. La fuente Rajdhani (Indian Type Foundry) se distribuye bajo SIL OFL 1.1. Detalle y modificaciones en [LICENSE_ASSETS.md](LICENSE_ASSETS.md). Ninguno autoriza la redistribución: **no publicar el JAR**.

Los documentos previos en `docs/design` y `docs/research` describen un alcance anterior (Gorilla Arms, Ripperdoc) que ya no aplica.
