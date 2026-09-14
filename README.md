# Apogee

Mod privado para **Minecraft Java 1.21.1**, **Fabric** y **Java 21**, pensado para partidas con amigos mediante **Essential** (también funciona en individual, LAN y servidor dedicado). Identificador: `apogee`.

Contiene un único implante, el **Sandevistan**, y tres componentes para fabricarlo.

> Estado: primera versión compilable y probada de forma automatizada. La compatibilidad con Essential **no está comprobada** hasta completar la sesión real descrita en [los resultados de pruebas](docs/testing/2026-09-14-apogee-resultados.md).

## Contenido

| ID | Objeto | Notas |
|---|---|---|
| `apogee:sandevistan` | Sandevistan | Implante reutilizable, sin durabilidad, no apilable. Ranura Trinkets `cyberware/os`. |
| `apogee:os_core_t5` | Núcleo T5 | Componente apilable. |
| `apogee:apogee_overdrive_kernel_t5` | Overdrive Kernel T5 | Componente apilable. |
| `apogee:quantum_sync_lattice_t5` | Quantum Sync Lattice T5 | Componente apilable. |

### Recetas (mesa de crafteo, 1 unidad)

| Resultado | Ingredientes |
|---|---|
| Núcleo T5 (sin forma) | 4 lingotes de netherita, 4 bloques de redstone, 1 comparador |
| Overdrive Kernel T5 (sin forma) | 2 estrellas del Nether, 4 lingotes de netherita, 2 bloques de diamante, 1 tótem de inmortalidad |
| Quantum Sync Lattice T5 (sin forma) | 2 cristales del End, 2 bloques de amatista, 1 frasco de experiencia, 4 bloques de redstone |
| Sandevistan (con forma) | Fila 1: bloque de diamante, Núcleo T5, bloque de diamante · Fila 2: lingote de netherita, Overdrive Kernel T5, lingote de netherita · Fila 3: bloque de diamante, Quantum Sync Lattice T5, bloque de diamante |

## Uso

- Equipa el Sandevistan en la ranura **Sistema operativo** de Trinkets (la pechera y los élitros siguen libres).
- **V** (configurable) activa el efecto o lo termina antes de tiempo.
- En el tooltip, **G** (tecla de pestañas de Simply Tooltips) alterna Descripción y Estadísticas; al dejar de señalar el objeto vuelve a Descripción.

### Mecánica (valores por defecto)

| Regla | Valor |
|---|---|
| Ralentización | 85 % (entidades afectadas avanzan al 15 %) |
| Radio | 10 bloques, esfera centrada en el portador, sin línea de visión |
| Duración | 10 s |
| Movimiento | +50 % |
| Daño cuerpo a cuerpo y proyectiles lanzados durante la activación | +25 % (los proyectiles conservan el bonus hasta impactar) |
| Por baja | +2 s, con máximo absoluto de 20 s desde el inicio |
| Recarga | 30 s desde que termina (también al terminar manualmente) |

- **Afectados:** jugadores en supervivencia/aventura y monstruos que tienen al portador como objetivo o intercambiaron daño con él en los últimos 10 s (también mobs neutrales que lo atacan). Ser amigo en Essential no da inmunidad.
- **No afectados:** el portador, otros portadores activos, animales, aldeanos, gólems y monstruos ajenos al combate, bloques, redstone, tolvas y generación de mobs.
- **Proyectiles:** los del portador y de los combatientes seleccionados se ralentizan dentro del campo; los de dispensadores (sin dueño), no.
- **Bajas que extienden:** muertes de enemigos afectados causadas directamente por el portador (ataque o proyectil). Animales y muertes sin atribución directa no cuentan.
- **Fin y limpieza:** morir, desequipar, cambiar de dimensión, desconectarse o cerrar el mundo termina el efecto, inicia la recarga y quita modificadores y estado temporal.
- La recarga se guarda con el jugador y usa el tiempo de juego del mundo: se pausa con la pausa individual y continúa mientras la sesión multijugador sigue en marcha.

### Cómo funciona el tiempo selectivo

Cada entidad lleva un acumulador temporal. Mientras está ralentizada solo ejecuta su tick real cuando el acumulador supera 1 (cada ~6,7 ticks con 15 %), así que movimiento, IA, cadencia de ataque, uso de objetos y animaciones van al 15 % sin cambiar el ritmo del mundo. El cliente replica el mismo salto de ticks e interpola con el progreso del acumulador para que el movimiento se vea continuo. Cámara, menús y conexión no dependen de esos ticks y siguen fluidos. No se usan pociones de lentitud.

El servidor (integrado del anfitrión o dedicado) valida la tecla, decide objetivos, daño y temporizadores, y sincroniza con Fabric Networking al entrar, cambiar de dimensión, reaparecer y cada vez que cambia el campo.

## Pendientes

- **Varios portadores del Sandevistan:** definir y ajustar la regla. Comportamiento buscado: otro jugador que porte el Sandevistan no se ralentiza, pero sí recibe su bonus de velocidad y daño. Además, dos portadores cerca no duplican la ralentización del entorno. Hoy la ralentización no se acumula y los portadores **activos** son inmunes entre sí. Sin embargo, un portador **con el implante equipado pero inactivo** dentro de otro campo sí se ralentiza (lo cubre la prueba `activeWearersAreImmuneToEachOther`).
- **Tooltip en juego:** revisar cómo se ve dentro de Minecraft (escalas de interfaz, idioma español, tamaño del modelo en el rombo) y ajustarlo frente a la maqueta.
- **Barras de uso y recarga:** rediseñar y ajustar el HUD de duración y recarga del Sandevistan (estilo, posición y convivencia con corazones, armadura y jugador en tercera persona).

## Instalación para anfitrión e invitados

Todos deben usar **exactamente** estas versiones:

| Mod | Versión | Necesario en |
|---|---|---|
| Apogee | 0.1.0+1.21.1 (`build/libs/apogee-0.1.0+1.21.1.jar`) | Todos |
| Fabric Loader | 0.16.0 o superior (probado con 0.19.3) | Todos |
| Fabric API | 0.116.17+1.21.1 | Todos |
| Trinkets | 3.10.0 (incluye Cardinal Components) | Todos |
| Simply Tooltips | 0.1.5 | Clientes |
| Architectury API | 13.0.11+fabric | Clientes (dependencia de Simply Tooltips) |
| Fzzy Config | 0.7.6+1.21 | Clientes (dependencia de Simply Tooltips) |
| Fabric Language Kotlin | 1.13.13+kotlin.2.4.10 | Clientes (dependencia de Fzzy Config) |

No se requiere Simply Swords. Un servidor dedicado solo necesita Apogee, Fabric API y Trinkets. Sin Simply Tooltips el cliente arranca y muestra un tooltip simple.

**Con Essential:** el anfitrión abre su mundo con *Host World* de Essential ([guía oficial](https://essential.gg/wiki/start-hosting)); el mundo se ejecuta en su ordenador, así que su `config/apogee-server.json` define las reglas. Los invitados deben tener la misma lista de mods antes de unirse.

## Configuración

- `config/apogee-server.json` (quien aloja el mundo): porcentaje de ralentización, radio, duraciones en segundos, bonificaciones, extensión por baja, máximo, recarga y memoria de combate. Se lee al abrir el mundo; los clientes reciben estos valores y el tooltip los muestra.
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
- `runGametest`: 15 pruebas de servidor con jugadores simulados (`src/gametest`), informe en `build/gametest/report.xml`.
- `runSmokeClient`: abre el cliente, crea un mundo plano, equipa el implante, captura tooltip, activación, HUD y estelas en `build/smoke-client/screenshots` y se cierra.

Estructura principal: `dev.apogee.sandevistan.SandevistanManager` (autoridad del servidor), `dev.apogee.mixin` (salto de ticks, daño, anti-vuelo), `dev.apogee.client` (estado sincronizado, efectos, estelas, renderizado y tooltip).

Ramas: `main` estable, `develop` integración. Commits con Conventional Commits. Cambios en [CHANGELOG.md](CHANGELOG.md).

## Recursos y licencias

El modelo y las texturas del implante proceden de Sandevistan (Cyperpunk) 1.2.0; las texturas de componentes y los sonidos, de Project: Edgerunner 1.1.2. Detalle y modificaciones en [LICENSE_ASSETS.md](LICENSE_ASSETS.md). Ninguno autoriza la redistribución: **no publicar el JAR**.

Los documentos previos en `docs/design` y `docs/research` describen un alcance anterior (Gorilla Arms, Ripperdoc) que ya no aplica.
