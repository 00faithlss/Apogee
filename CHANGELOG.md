# Changelog

Registro de cambios de Apogee. Las entradas describen trabajo completado.

## [Sin publicar]

- Implante **Tendones reforzados** `apogee:reinforced_tendons` (ranura Piernas): pulsa saltar en el aire para un doble salto, con 4 bloques de caída amortiguados y una recarga por vuelo. Escapes en los tobillos, receta propia, textos en español e inglés, pruebas de servidor y de cliente (`runTendonsSmoke`).

## [1.1.0+1.21.1] — 2026-10-01

- Nanocable un 30 % más grueso, con núcleo claro, borde rojo incandescente y halo emisivo.
- Variante blanca brillante `apogee:nanowire_white`, con el mismo alcance de 7 bloques, animaciones y compatibilidad con Punchy. Se obtiene combinando un Nanocable rojo y cuarzo; se instala en la ranura de Brazos.
- Ambos Nanocables provocan Sangrado: 1 de daño por segundo durante 4 s, con partículas rojas e icono de estado. Los cortes aceptados renuevan la duración sin acumular intensidad. No se aplica al bloquear el corte con escudo y no altera la inmunidad temporal de los golpes normales.
- Paleta emisiva blanca y proyectos editables de Blockbench para ambos brazos; tooltip y traducciones actualizados.

## [1.0.0+1.21.1] — 2026-10-01

Primera versión oficial de Apogee: Sandevistan, Cuchillas Mantis normales, térmicas y eléctricas, Nanocable y mesa de matasanos para instalar y gestionar implantes.

### Cambiado

- La descripción del mod en Minecraft refleja el ciberware disponible, la mesa de matasanos, el alcance de 7 bloques del Nanocable y la compatibilidad opcional con Punchy.
- El Nanocable lleva modelos de ciberware de palma y antebrazo creados en Blockbench y renderizados con GeckoLib. El cable sale de la boca del mecanismo y sigue su posición animada.
- Al desplegar las Cuchillas Mantis en tercera persona, las mangas de las pecheras vanilla dejan espacio al mecanismo y conservan la parte superior de la armadura, sus colores, adornos y brillo de encantamiento.

### Corregido

- Los cortes de primera persona de las Cuchillas Mantis entran hacia el centro desde la muñeca; la otra mano acompaña y ambas vuelven a su pose de reposo. Se eliminan los movimientos de ataque superpuestos y se conserva la pose nativa al llevar un objeto, con y sin Punchy.
- El Nanocable conserva la skin del jugador y el agarre mantiene un recorrido limpio entre las salidas del ciberware.
- El paciente de la mesa de matasanos apoya las piernas en el cojín y mantiene los brazos en reposo, también con Entity Model Features y con las cuchillas desplegadas.

### Añadido (Compatibilidad con Punchy)

- Compatibilidad de las Cuchillas Mantis y el Nanocable con las animaciones de primera persona de Punchy: caminar, correr, balanceo, giros de cámara y transiciones de despliegue y guardado con retorno al objeto en mano. Respeta la mano principal y la configuración de animaciones de Punchy; el mod sigue siendo opcional.

### Cambiado (Animaciones del Nanocable)

- Barridos de clic izquierdo más amplios, con un tirón corto, golpe rápido, seguimiento y recuperación gradual. Los brazos de primera y tercera persona comparten las fases del movimiento.
- Clic derecho: ambos brazos preparan arriba y tiran hacia abajo. Se corrigió el signo de la rotación en primera persona que contrarrestaba la subida del brazo; se eliminó el alcance hacia delante de la pose.
- El cable del ataque doble transmite la curva desde la mano hasta la punta con retraso, con largo limitado y descenso real en sus tres variantes. Reemplaza la trayectoria de punta que hacía parecer que se lanzaba el cable.
- Cables visuales más largos para cubrir el alcance nominal de 7 bloques: hasta 8,4 bloques de cable en el golpe simple y 9,1 en el doble (antes 5,88 y 7). El tramo extra permite la curvatura del latigazo; el alcance de daño permanece en 7. El punto más adelantado del barrido simple se escala a ese alcance.
- El cable empieza y termina exactamente en el punto sostenido entre las manos. El tramo desplegado se limita al recorrido de la punta para evitar muestras amontonadas en el agarre.
- Estelas más breves y suaves: cinco copias espaciadas por tiempo real, con aparición y desaparición progresivas durante el golpe.
- El ataque doble tiene prioridad sobre la pose de un latigazo simple en curso, para que brazos y cable interpreten el mismo ataque.

### Pruebas

- Cuchillas Mantis: las tres variantes, cortes de ambos brazos y estocada mediante clics del juego, con objetos equipados; prueba con Punchy 2.8a y 2.8c y sin Punchy. Comprobación de que la muñeca atacante se acerca a la mira durante el corte.
- Mesa de matasanos: instalación, visor y pose en las cuatro orientaciones con Entity Model Features, ETF y Fresh Animations, incluidas las cuchillas desplegadas.
- Compatibilidad comprobada con Punchy 2.8a para Fabric 1.21.1: movimiento y guardado de ambos implantes, mano principal derecha e izquierda, balanceo de la vista activado y desactivado, y ataques del Nanocable en primera y tercera persona.
- `nanowireMotionTest`, incluido en `check` y `build`: continuidad al salir y volver al reposo, barridos opuestos simétricos, coordenadas finitas y límites de despliegue, incluso con manos cruzadas o coincidentes; preparación del ataque doble por encima del agarre, golpe por debajo y retraso de la punta respecto de la raíz.
- Comprobación del alcance visual: el barrido simple llega a 7 bloques hacia delante y ambos cables del ataque doble llegan a esa distancia al momento de impacto en los tres estilos.
- `runNanowireSmoke`: capturas reales de ambas vistas, tres ataques dobles y clic derecho durante un latigazo simple.
- `tools/nanowire_review.py`: comparación de capturas antes/después en MP4 y GIF.

## [0.14.0+1.21.1] — 2026-10-01

### Cambiado (Nanocable)

- Más alcance: el latigazo y el latigazo doble golpean hasta **7 bloques** (antes 5). En creativo, Minecraft suma sus 2 bloques de siempre.
- Las trayectorias y el largo del cable dibujado se estiran en proporción (×1,4 hacia los lados y adelante, ×1,2 en altura), para que el cable llegue hasta donde golpea. Se calculan a partir del alcance, así que siguen cualquier ajuste futuro de `NanowireRules.REACH_BONUS`.

### Pruebas

- Gametest nuevo: con el nanocable fuera, el bonus de alcance es de 4 bloques sobre la base de 3, y el latigazo doble golpea a un objetivo a 6 bloques pero no a uno fuera de alcance.

## [0.13.0+1.21.1] — 2026-10-01

### Añadido

- Recuadro de recarga del ataque con clic derecho (la estocada de las Cuchillas Mantis y el latigazo doble del Nanocable), con el mismo estilo que el del Sandevistan: aparece abajo a la izquierda mientras el implante está fuera, con el ícono del implante; cian cuando está listo, magenta con los segundos que faltan mientras recarga. Si el del Sandevistan también se ve, se apila encima. Respeta la opción `hud` de `config/apogee-client.json`.

### Cambiado

- El objeto Nanocable lleva el cable recto y tenso entre las bocas de sus dos carcasas, en vez del aro enrollado del paquete.

### Corregido

- El asiento invisible de la mesa de matasanos no tenía nombre: los mods que muestran lo que se mira (como Jade) mostraban `entity.apogee.ripperdoc_seat`. Ahora se llama «Asiento de matasanos» («Ripperdoc seat» en inglés). Se revisaron los demás nombres, subtítulos, teclas y mensajes de muerte del mod y no falta ninguno.

## [0.12.0+1.21.1] — 2026-10-01

### Añadido (Mesa de matasanos)

- Los diez sistemas del diseño del autor son ranuras reales de Trinkets en el grupo de ciberware, con la cantidad del diseño: Corteza frontal (3), Brazos (1), Esqueleto (2), Sistema nervioso (2), Tegumentario (3), Sistema operativo (1), Sistema ocular (1), Manos (2), Circulatorio (3) y Piernas (1). La mesa los muestra todos en dos columnas, con su nombre y el ícono del sistema en cada ranura vacía. Los que todavía no tienen implantes se ven apagados y lo dicen al pasar el mouse; un implante futuro solo tiene que entrar en la etiqueta `trinkets:cyberware/<sistema>`.

### Corregido (Mesa de matasanos)

- El modelo de la camilla se dibujaba girado: el respaldo sobresalía por el lado del pie, fuera de sus dos bloques. Ahora el respaldo queda sobre la mitad de la cabecera.
- El paciente se orientaba según hacia dónde miraba la cámara, así que podía quedar boca abajo con las piernas metidas en el cojín. Ahora se orienta según la camilla: boca arriba, con la cabeza en el respaldo y las piernas estiradas sobre la parte plana. La cabeza gira desde ahí, hasta 60° a cada lado.
- Con Entity Model Features (Fresh Animations), las piernas y los brazos del paciente siguen la animación de dormir del pack.

## [0.11.0+1.21.1] — 2026-10-01

### Cambiado (Mesa de matasanos)

- La pantalla se arma como la de un cofre: arriba un panel del matasanos (las ranuras Brazos y Sistema operativo con su nombre y el implante instalado, el visor del paciente y los botones Rayos X y Skin) y abajo el inventario normal de Minecraft. Se dibuja al tamaño normal de la interfaz, sin escalado propio, y sus textos siguen el idioma del juego.
- El paciente toma la pose de dormir de la cama, recostado sobre el respaldo de la camilla. Es la pose del jugador, así que se sincroniza: en multijugador los demás también lo ven acostado. No cuenta como dormir: no salta la noche ni fija el punto de reaparición.

### Corregido (Mesa de matasanos)

- Las ranuras de la pantalla no coincidían con las de la textura cuando la ventana obligaba a reducirla, y por eso los botones Rayos X y Skin no respondían al clic. Con el nuevo diseño el clic cae donde se ve el botón.
- Arrastrar el visor hacia la derecha giraba al personaje hacia la izquierda.

### Pruebas

- La prueba multijugador (`tools/mantis_tests.sh mp`) acuesta al portador en una mesa de matasanos: su cliente cambia las Cuchillas Mantis por el Nanocable con clics reales y activa los rayos X, el servidor comprueba la instalación y el daño, y el observador comprueba que lo ve acostado.
- `COMPAT_SMOKE=ripperdoc tools/mantis_tests.sh compat` corre la prueba de la mesa con Punchy, EMF, ETF y Fresh Animations.

## [0.10.0+1.21.1] — 2026-10-01

### Añadido (Mesa de matasanos)

- Botones **Rayos X** y **Skin** bajo el visor del paciente. En rayos X la piel se vuelve translúcida y teñida de cian, la armadura, la capa y los objetos en mano se ocultan, y se ven los implantes instalados a través del cuerpo (también la piel de los brazos de las Cuchillas Mantis). Si no hay implantes, el visor avisa «Sin ciberware detectado». El botón activo queda marcado y el modo se recuerda entre visitas a la mesa.

### Corregido (Mesa de matasanos)

- El jugador se acuesta de verdad en la camilla: boca arriba, con la cadera en la bisagra del cojín, la espalda sobre el respaldo y las piernas estiradas sobre la parte plana. Antes quedaba en la pose de montar inclinado hacia adelante, como agachado.
- El botón de los implantes ya no aparece en el inventario. Trinkets armaba esas ranuras desde el componente del jugador, no desde la lista que se filtraba; ahora el grupo de ciberware se deja fuera al armar el inventario, en el servidor y en el cliente.
- El aviso del costo de la cirugía se parte en dos líneas y ya no se monta sobre la barra rápida.

## [0.9.0+1.21.1] — 2026-10-01

### Añadido (Mesa de matasanos)

- Bloque `apogee:ripperdoc_table`: una camilla reclinable de dos bloques con la camilla del conjunto ripperdoc de Project: Edgerunner, elegida por el autor del proyecto. Se coloca como una cama y se rompe entera.
- Clic derecho acuesta al jugador en la camilla, con la espalda en el respaldo, y abre la pantalla del matasanos con el diseño del autor (sin los textos de muestra ni «Capacidad por definir»). Al cerrarla se levanta.
- La pantalla tiene las ranuras Sistema operativo y Brazos, conectadas a las de Trinkets, y el inventario. En el centro se ve el propio personaje con sus implantes, que se rota arrastrándolo, y abajo a la izquierda la lista de implantes instalados. Se reduce sola si la ventana es más chica que la pantalla.
- Instalar un implante es una cirugía: cuesta 3 corazones, atraviesa la armadura y nunca mata. Tipo de daño propio `apogee:surgery`.
- Receta con forma: cables neurales, chip de control, lana blanca y hierro.
- Prueba de servidor de la receta, la instalación y el daño; prueba de cliente `runRipperdocSmoke` con capturas.

### Cambiado

- Los implantes solo se instalan o quitan en la mesa de matasanos: las ranuras de ciberware ya no aparecen en el inventario normal ni en el creativo, y usar un implante con clic derecho ya no lo equipa. Lo que ya estaba equipado sigue igual.

## [0.8.0+1.21.1] — 2026-10-01

### Añadido (Nanocable)

- El latigazo doble (clic derecho) tiene tres formas que se turnan en cada golpe, según las referencias del autor del proyecto:
  - **X:** cada brazo sube a su esquina y baja cruzando hacia el otro lado, y los cables trazan una X;
  - **vertical:** los dos brazos suben sobre la cabeza y bajan juntos, con los cables cayendo en paralelo;
  - **S:** el cable derecho por arriba y el izquierdo por abajo.

### Corregido (Nanocable)

- En el latigazo doble el brazo izquierdo casi no se movía. Ahora los dos brazos lanzan su cable, en primera y en tercera persona.

## [0.7.1+1.21.1] — 2026-10-01

### Corregido (Nanocable)

- El latigazo doble (clic derecho) dibuja una S, como pidió el autor del proyecto: el cable de la mano derecha va por arriba, sale hacia la derecha y cruza la vista hacia la izquierda; el de la mano izquierda hace el trazo de abajo, al revés. Antes los dos daban las mismas vueltas grandes y se cruzaban en el centro, así que parecían un solo cable.
- La procedencia de los sonidos del Nanocable en `LICENSE_ASSETS.md`: son de Cyberpunk 2077 SoundDB, como los de las Cuchillas Mantis.

## [0.7.0+1.21.1] — 2026-09-30

### Añadido (Nanocable)

- Sonidos propios, aportados por el autor del proyecto, en lugar de los provisionales de Minecraft:
  - despliegue y guardado;
  - cuatro latigazos al aire, que se alternan al azar;
  - cuatro golpes al enemigo, también al azar;
  - el golpe de más cuerpo para el latigazo doble del clic derecho.

## [0.6.1+1.21.1] — 2026-09-30

### Cambiado

- El núcleo de potencia de brazos lleva un diamante al centro en lugar de una estrella del Nether, que encarecía demasiado las Cuchillas Mantis y el Nanocable.

## [0.6.0+1.21.1] — 2026-09-30

### Añadido

- Tres piezas nuevas para el ciberware de brazos, con texturas elegidas por el autor del proyecto:
  - núcleo de potencia de brazos, con la textura `arms_power_core_t4_item` de Project: Edgerunner;
  - monofilamento, con la textura `component_fiberoptics` de Cyberware: Reforged;
  - carcasa de nanocable, con la textura `component_plating` de Cyberware: Reforged.
- Recetas en el README; procedencia en `LICENSE_ASSETS.md`.

### Cambiado

- Las Cuchillas Mantis (las tres variantes) llevan el núcleo de potencia de brazos en lugar del Overdrive Kernel T5, que queda solo para el Sandevistan.
- El Nanocable se arma con dos carcasas, el monofilamento, el núcleo de potencia de brazos, el Núcleo T5 y cuatro cables neurales. La receta anterior ya no funciona.
- La prueba de servidor arma las piezas nuevas y comprueba que las recetas con el Overdrive Kernel ya no funcionan.

## [0.5.1+1.21.1] — 2026-09-30

### Corregido (Nanocable)

- Al golpear, los cables del Nanocable ya no salen del centro como un solo cable: cada uno sale como un látigo desde su propia mano y se vuelve a unir con el otro en el centro al final. En el golpe doble el cable izquierdo gira más abajo, más a la izquierda y un instante después, así que se ven dos látigos separados.

## [0.5.0+1.21.1] — 2026-09-30

### Añadido (Nanocable)

- Receta del Nanocable con las piezas de las Cuchillas Mantis: cuatro cables neurales, el chip de control, dos lingotes de netherita, el Overdrive Kernel T5 y el Núcleo T5. Detalle en el README.
- Logro de desafío «Pendiendo de un hilo» al craftearlo; la pestaña Apogee también aparece al conseguirlo.
- Tooltip con la tarjeta de las Cuchillas Mantis: 8 de daño, 1,33 ataques por segundo y tipo físico en grande, con un ícono de cable. La vista Descripción muestra los controles y la de Estadísticas el golpe crítico, el alcance y el daño, arco y recarga del latigazo doble. Sin Simply Tooltips se muestran líneas simples.
- La prueba de servidor arma el Nanocable y comprueba que no se puede encantar; la prueba de cliente captura su tooltip.

### Cambiado

- Las cifras del tooltip se redondean a dos decimales.

### Corregido

- La tabla de instalación del README mostraba la versión 0.2.7.

## [0.4.0+1.21.1] — 2026-09-30

### Añadido (Nanocable)

- Nuevo implante de brazos `apogee:nanowire` (Nanocable) para la ranura `cyberware/arms`, con el paquete de modelos y animaciones del autor del proyecto. Comparte con las Cuchillas Mantis el despliegue con R, el bloqueo de la barra y el inventario, la alternancia derecha/izquierda del clic izquierdo y la sincronización con otros jugadores.
- La carcasa va en la cara interna de cada antebrazo, sobre el brazo del propio jugador (no lo reemplaza), con máscara emisiva roja.
- El cable lo dibuja el mod en cada fotograma como una curva continua: núcleo rojo emisivo, halo y estelas de luz. En reposo va tenso de una mano a la otra. Las exportaciones del paquete quedan solo con la carcasa (de 8,3 MB a 36 KB de animación por brazo).
- Primera persona desplegado: los dos brazos con la piel del jugador en la posición de Minecraft, con el balanceo al caminar de las Cuchillas Mantis. La coreografía y sus tiempos se compararon fotograma a fotograma con un video del Monowire de Cyberpunk 2077:
  - el latigazo cruza el brazo por la vista en 0,2 s y el cable cierra un lazo amplio hacia los 0,35 s antes de volver a la mano;
  - el golpe doble lleva el brazo derecho adelante y hace girar el cable en espirales durante medio segundo; dura 0,9 s en total;
  - al soltarse, la punta se devuelve en un lazo redondo; al recogerse, el cable se tensa y llega recto a las manos, sin saltos.
- Clic izquierdo: latigazo con el brazo que toca (8 de daño, alcance de 5 bloques, uno cada 0,75 s). Clic derecho: latigazo doble a todo lo que haya delante en un arco de 140° dentro del alcance (12 de daño, recarga de 3 s). Valores provisionales.
- El objeto muestra las dos carcasas con el cable enrollado, en el inventario y en la mano.
- Sonidos provisionales de Minecraft hasta tener grabaciones propias.
- Prueba de cliente `runNanowireSmoke`: filma tick a tick cada movimiento en primera y tercera persona, y captura el objeto en la mano.

## [0.3.0+1.21.1] — 2026-09-30

### Añadido (Cuchillas Mantis)

- Las Cuchillas Mantis se arman por partes, según la propuesta del banco de piezas aprobada por el autor del proyecto. Siete piezas nuevas:
  - cable neural (4 por receta);
  - chip de control, con la textura `axolotl_item` de Project: Edgerunner;
  - actuador de cuchilla, con la textura `arms_actuator_matrix_t4_item` de Project: Edgerunner;
  - riel de despliegue;
  - hoja mantis, con la receta del autor: hierro alrededor de una espada de netherita y un lingote de netherita abajo;
  - hoja térmica y hoja eléctrica, hechas desde la hoja mantis.
- Cada implante se arma con dos hojas de su tipo, el Overdrive Kernel T5, el Núcleo T5, dos rieles, dos actuadores y un cable neural. Recetas completas en el README.
- Las texturas de las piezas vienen de Zoe Isn't Cyberpunk (Apache 2.0), Cyberware: Reforged (MIT) y Project: Edgerunner; procedencia en `LICENSE_ASSETS.md` y textos de licencia en `src/main/resources/licenses/`.

### Cambiado (Cuchillas Mantis)

- Se quitaron las recetas anteriores: la de netherita con dos Núcleos T5 y las mejoras de las Cuchillas Mantis terminadas a térmicas o eléctricas.
- El logro «Mantis religiosa» se obtiene al craftear cualquiera de las tres variantes, y la pestaña Apogee también aparece al conseguir cualquier pieza.

### Añadido (pruebas)

- Prueba de servidor que arma cada receta en una mesa de crafteo simulada, del cable al implante, y comprueba que las recetas anteriores ya no funcionan.
- La prueba de cliente pone las siete piezas en la barra, comprueba que cada una tiene modelo y la captura.

## [0.2.11+1.21.1] — 2026-09-30

### Cambiado (Cuchillas Mantis)

- La estocada de la normal y la térmica usa los sonidos de las Mantis: un corte con un golpe más grave encima, como la eléctrica, en lugar del sonido provisional de tridente.

## [0.2.10+1.21.1] — 2026-09-30

### Corregido (Sandevistan)

- Las estelas bajaban mucho los FPS después de unos segundos de efecto (de 600 a 30 en el equipo del autor), sobre todo con las Cuchillas Mantis equipadas. Cada silueta volvía a posar y dibujar el modelo del jugador con todas sus capas (armadura, implantes y el modelo GeckoLib de las Mantis) en cada frame. Ahora cada silueta se graba una sola vez como vértices cuando queda atrás y cada frame solo se repiten, agrupados por tipo de render para vaciar el búfer pocas veces. En los modelos muy densos (más de 4.000 vértices, como las Mantis) se descartan las caras de menos de un cuarto de píxel, que en una silueta translúcida no se ven. En la prueba con 25 siluetas, Mantis y armadura de netherita, dibujarlas pasó de 48 ms a 12 ms por frame y de 316.000 a 145.000 vértices.

### Cambiado (Cuchillas Mantis)

- El tooltip tiene el tamaño del del Sandevistan: mismo ancho mínimo, números y título más chicos, una línea de descripción y controles más cortos.
- La estocada eléctrica suena eléctrica: un corte eléctrico con una descarga grave encima, en lugar del golpe de tridente de la normal.

## [0.2.9+1.21.1] — 2026-09-30

### Cambiado (Cuchillas Mantis)

- El tooltip ya no muestra el tier ni la franja de color del tier, igual que el del Sandevistan.

### Añadido (pruebas)

- La prueba de cliente cambia la tecla de desplegar las Cuchillas Mantis y comprueba que el tooltip muestra la nueva.

## [0.2.8+1.21.1] — 2026-09-30

### Cambiado (Sandevistan)

- El logro «Un implante rudimentario» se obtiene al conseguir el Sandevistan de cualquier forma (crafteo, cofre, otro jugador), no solo al craftearlo. Se mantiene su id `apogee:craft_sandevistan`, así que quien ya lo tenía no lo pierde.
- La frase de Adam Smasher suena al conseguir el Sandevistan, no al craftearlo: se reproduce para todo el servidor cuando un jugador completa ese logro, así que una vez por jugador.

### Cambiado (Cuchillas Mantis)

- Daño: 10 por golpe (15 con crítico) y 2 ataques por segundo, antes 7 y 5,5. El objeto que quede en la mano al desplegar ya no suma: su daño y velocidad se descuentan, y no aporta encantamientos, barrido, bonificaciones propias (como la maza) ni pierde durabilidad con los cortes.
- Las cuchillas eléctricas hacen +4 de daño (cortes y estocada) si el objetivo está en el agua.
- No se pueden encantar: ni en la mesa de encantamientos, ni con libros en el yunque, ni con `/enchant`.
- Tooltip nuevo al estilo de las armas de Cyberpunk 2077: fondo rojo translúcido, marco rojo salmón con la esquina y el detalle cian del juego (colores tomados de su inventario), franja de tier, textos amarillos y título con glitch cromático. Arriba, en grande, el daño por golpe y los ataques por segundo junto al tipo de daño con su ícono. La vista Descripción lista los controles y la habilidad de la variante (como las armas únicas de Simply Swords) y la vista Estadísticas los valores exactos; se alternan con **G**. La térmica suelta brasas, la eléctrica lleva arcos en el borde.
- El modelo 3D de las Cuchillas Mantis en la mano (primera persona) se ve completo: se ajustó su posición y escala, que lo dejaban casi fuera de la pantalla. En el suelo se ve con la pose del modelo.

### Añadido (pruebas)

- Pruebas de servidor de las Cuchillas Mantis: daño y velocidad con una espada de netherita con Filo V en la mano, golpe crítico, bonificación eléctrica en el agua y rechazo de todos los encantamientos.
- La prueba de cliente captura los tres modelos en la barra de objetos, en la mano y tirados en el suelo, y los tooltips de las tres variantes en ambas vistas.

## [0.2.7+1.21.1] — 2026-09-30

### Cambiado (Cuchillas Mantis)

- Modelos 3D de inventario para las tres variantes (normal, térmica y eléctrica), aportados por el autor del proyecto. Reemplazan a los íconos planos, que se eliminaron.
- Desplegar o guardar con un objeto en la mano ya no depende de la animación de cambio de objeto de cada mod. Punchy no la reproducía en este caso, así que el objeto aparecía y desaparecía de golpe. Ahora toda la vista de las manos (brazo y objeto, los dibuje vanilla o Punchy) baja suave fuera de la pantalla, y después suben los brazos del implante; al guardar, lo inverso. Cada tramo dura entre 5 y 7 ticks con curvas suaves, así que el cambio se ve más pausado que el cambio de objeto de vanilla.

## [0.2.6+1.21.1] — 2026-09-30

### Corregido (Cuchillas Mantis)

- Desplegar o guardar las cuchillas con un objeto en la mano cambiaba de golpe entre el objeto y el implante. Ahora funciona como un cambio de objeto: al desplegar, el objeto baja con la animación de vanilla o, con Punchy, con su animación de salida (`hand_out`), y después suben los brazos del implante. Al guardar, los brazos bajan y el objeto vuelve a subir con la animación de equipar de vanilla o con la de entrada de Punchy (`hand_in`). Para lograrlo, mientras el implante está en pantalla, el seguimiento de objetos de primera persona ve las manos vacías.

### Añadido (pruebas)

- `MOTION_ITEM=true tools/mantis_tests.sh motion` graba cada tick del despliegue y el repliegue con una espada en la mano, en vanilla y con Punchy.

## [0.2.5+1.21.1] — 2026-09-30

### Corregido (Cuchillas Mantis)

- Al mantener el clic sobre un bloque, Minecraft repite el golpe en cada tick mientras lo rompe, y cada repetición disparaba un corte alternando brazo y un sonido: los brazos golpeaban a toda velocidad. Ahora un corte empieza solo cuando terminó el anterior (7 ticks), en el cliente y en el servidor.
- Con Punchy, corriendo, los brazos iban de lado a lado demasiado rápido: se usaba el vaivén de caminar a la velocidad de la carrera. Ahora la carrera tiene su propio movimiento, medido sobre la mano de Punchy: vaivén lateral más corto, rebote vertical una vez por ciclo y menos bajada. La transición entre caminar y correr es suave.

### Añadido (pruebas)

- `MOTION_SPRINT=true tools/mantis_tests.sh motion` mide la carrera en lugar de la caminata.

## [0.2.4+1.21.1] — 2026-09-30

### Cambiado (Cuchillas Mantis)

- Movimiento de los brazos en primera persona medido, no a ojo. Se quitó el balanceo por paso de 0.2.3.
  - Sin Punchy: los brazos se mueven exactamente como la mano vanilla. No tienen movimiento propio; solo reciben el balanceo de la vista si está activado, y se retrasan al girar.
  - Con Punchy: imitan cómo mueve la mano Punchy, medido cuadro a cuadro. Quietos hacen una deriva lenta de unos 4 s; al caminar, un vaivén lateral al ritmo del paso con una bajada suave; y no reciben el balanceo de la vista.
- Al guardar las cuchillas, el brazo cerrado termina en la misma pose que la mano que aparece después: la de vanilla, o la de Punchy según una pose ajustada en el juego.
- El implante dibuja la capa exterior de la manga de la skin, igual que el brazo vanilla, así que al guardarlo ya no cambia la manga.

### Añadido (pruebas)

- `tools/mantis_tests.sh motion`: captura cada tick de la primera persona quieto, caminando, deteniéndose y guardando las cuchillas, con y sin balanceo de la vista, en vanilla y con Punchy. `tools/mantis_motion.py` sigue las manos en esos cuadros y resume cuánto y con qué ritmo se mueven.
- `tools/mantis_tests.sh calibrate`: ajusta en el juego la pose final del repliegue, comparando cuadro a cuadro con la mano que aparece después.

## [0.2.3+1.21.1] — 2026-09-30

### Cambiado (Cuchillas Mantis)

- En primera persona, con las cuchillas fuera, los brazos ya no quedan inmóviles: respiran levemente quietos, se balancean con cada paso (alternando derecho e izquierdo) y se retrasan un poco al girar la vista, como las manos con objetos. Así el cambio a las manos normales al guardarlas se nota menos.
- La prueba de cliente captura la primera persona caminando con las cuchillas desplegadas.

## [0.2.2+1.21.1] — 2026-09-30

### Cambiado (Cuchillas Mantis)

- En primera persona, con el implante plegado solo se ve la mano principal, como en vanilla. Al desplegar, el otro brazo sube desde abajo de la pantalla, y al guardar baja hasta salir de ella.
- Hoja térmica de un rojo puro y uniforme, sin ruido ni tonos anaranjados, con el lado del filo difuminado a blanco por el calor.
- La eléctrica suelta pequeños rayos que recorren la hoja o saltan de ella, con otra forma en cada tick, y chispas eléctricas. Se dibujan con la hoja, así que la siguen en primera y tercera persona.

### Corregido (Cuchillas Mantis)

- Con shaders, la hoja térmica se veía naranja. GeckoLib recortaba de la textura base las zonas emisivas y la hoja quedaba transparente bajo la capa emisiva, que se sumaba al fondo. Ahora la base queda entera y la máscara `_glowmask` se dibuja con el tipo emisivo de vanilla (el de los ojos de araña), con su propio color.

## [0.2.1+1.21.1] — 2026-09-29

### Cambiado (Cuchillas Mantis)

- Sonidos propios para las Cuchillas Mantis normales: despliegue, corte al aire (4 variantes) y golpe al enemigo (4 variantes, las más graves), recortados de un único audio y con el volumen igualado al de la eléctrica. La térmica usa los mismos eventos, así que ahora también suena con ellos. Guardar y estocada siguen provisionales.
- La hoja térmica pasa a un rojo más intenso y se quitó el halo de bloom falso: el brillo es solo la máscara emisiva y los shaders le aplican su propio bloom.
- Plegadas y con ambas manos vacías, en primera persona siguen viéndose los dos brazos del implante, así que el repliegue termina donde empezó en lugar de cambiar de golpe a las manos de otro mod (Punchy).

### Corregido (Cuchillas Mantis)

- Con shaders (Iris), las cuchillas de primera persona quedaban fijas mirando hacia adelante sin seguir la cámara. GeckoLib las dibujaba en el búfer del mundo en lugar del de las manos; ahora usan siempre el búfer que recibe el render.
- Las estelas del Sandevistan ya no vuelven a registrar la hoja ni a dibujar los destellos de glitch de la térmica en cada copia, lo que aligera el uso conjunto del Sandevistan y las Cuchillas Mantis.

### Añadido (pruebas)

- `tools/mantis_tests.sh iris`: `runMantisSmoke` en una instancia como la de un launcher (nombres intermediary) con Sodium 0.8.13, Iris 1.8.14 y el shader MakeUp UltraFast 9.5e. En el entorno de desarrollo Iris no arranca, por eso se arma aparte.
- La prueba de cliente captura la primera persona mirando al suelo, para ver que las cuchillas siguen a la cámara.

## [0.2.0+1.21.1] — 2026-09-29

### Añadido (Cuchillas Mantis)

- Implante «Cuchillas Mantis» en la nueva ranura Trinkets `cyberware/arms`, con receta con forma simétrica y logro de desafío «Mantis religiosa». Variantes **térmica** (prende fuego al objetivo) y **eléctrica** (lo aturde un momento), que se fabrican rodeando las Cuchillas Mantis con varas de blaze y bloques de magma, o con bloques de redstone y pararrayos.
- Tecla R (configurable) para desplegar o guardar las cuchillas. Clic izquierdo: cortes alternados, un brazo distinto en cada golpe (+6 de daño y +1,5 de velocidad de ataque mientras están desplegadas). Clic derecho: estocada con ambas cuchillas, con impulso, daño a lo largo de 4 bloques y recarga de 2 s. El servidor valida el despliegue, el daño y la recarga.
- Con las cuchillas desplegadas la ranura de la hotbar queda fija (rueda y teclas 1-9), el inventario no se abre y el clic derecho no usa objetos. El servidor también fija la ranura y cierra contenedores.
- Modelo y animaciones del kit Mantis v0.4 (GeckoLib, una geometría por variante y lado, 25 huesos): cerrado, el brazo se ve como uno normal; al desplegar se abren las cubiertas superiores e inferiores, sube el riel por el costado externo del antebrazo y la hoja sale de él en dos láminas rígidas (la principal y la punta), sin que el riel se estire. Animaciones de despliegue y repliegue (1 s) y de ataque por brazo (0,6 s).
- El modelo reemplaza el brazo vanilla mientras se lleva el implante: los paneles de piel, la mano y el tope del hombro se dibujan con la skin del portador (clásica o Slim), proyectando cada panel sobre la zona del brazo que cubre al estar cerrado.
- Tercera persona con postura de combate (brazos separados 20° del torso) y primera persona con ambas cuchillas desplegadas; plegadas se ve el brazo normal.
- Máscaras emisivas de las variantes térmica y eléctrica, activas solo con el implante abierto, y partículas de calor o chispas al atacar.
- GeckoLib 4.9.3 como dependencia obligatoria que se instala aparte (no va dentro del JAR).
- `tools/mantis_kit_import.py` importa las exportaciones del kit al mod; los proyectos de Blockbench y la documentación del kit quedan en `art/mantis-kit/`.
- Prueba de cliente `runMantisSmoke`: equipa cada variante y captura plegada, desplegando, desplegada, atacando, primeros planos y primera persona (despliegue con un objeto en la mano, corte de cada brazo y repliegue).
- `tools/mantis_tests.sh`: prueba multijugador (servidor dedicado y dos clientes que comprueban la sincronización de variante, despliegue, ataques y repliegue) y prueba de compatibilidad con Punchy y Fresh Animations (con Entity Model Features y Entity Texture Features).
- Cyberware bajo el piso de la carcasa, visible al abrirse las cubiertas inferiores: cables a lo largo del antebrazo con abrazaderas y dos módulos. Los cables son rojos en la térmica, azules en la eléctrica y grises en la normal (sus cables pasan de rojo a gris).
- En primera persona las dos cuchillas quedan hacia los bordes de la pantalla, a la misma distancia que un objeto en cada mano; antes se juntaban en el centro.
- La prueba de cliente guarda una captura de referencia con una espada en cada mano para comparar la separación.
- Con packs que animan los brazos, como Fresh Animations Player Extension, el implante abierto recibe igualmente la separación de 20° del torso para que sus cubiertas inferiores no lo atraviesen.
- Revisión del modelo: hoja y riel en grupos independientes (`blade_slider` y `cutting_edge` deslizan dentro del riel fijo) y alcance mayor que el del kit original; los proyectos de Blockbench por pieza quedan en `art/mantis-kit/modular/`.
- Metal oscuro en las tres variantes; la térmica tiene la hoja más ancha y la eléctrica un filo azul que brilla.
- Hojas rectas en las tres variantes: se quitaron los dientes de sierra de la hoja y del riel.
- La hoja térmica brilla al rojo vivo: textura roja con ruido, halo de brillo aditivo en capas (simula bloom sin shaders), destellos de «glitch» que desplazan una copia de la hoja por un instante, y partículas rojas de calor y vetas de ruido que salen de la hoja. Al golpear suelta chispas rojas en lugar de llamas.
- En primera persona, el brazo izquierdo de la térmica gira un poco hacia la cámara para mostrar más el mecanismo.
- En primera persona se ve la animación de despliegue y de repliegue con ambos brazos (antes aparecían ya abiertas de golpe, sobre todo con un objeto en la mano).
- En primera persona cada corte mueve el brazo que golpea, alternando derecho e izquierdo; antes solo se notaba el derecho.
- Sonidos propios para la variante eléctrica: despliegue, corte al aire (3 variantes) y golpe al enemigo (3 variantes, las más graves). Se recortaron los silencios iniciales para que suenen sin retraso, se igualó el volumen y se pasaron a mono para que se oigan con posición en multijugador.
- Las variantes normal y térmica, el guardado y la estocada siguen con sonidos provisionales que reutilizan eventos de Minecraft.

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
