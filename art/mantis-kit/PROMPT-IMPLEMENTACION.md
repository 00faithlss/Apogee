# Prompt para integrar las Mantis en el mod

Copia el texto de abajo y adjunta este paquete junto con tu repositorio. Si ya existe el mod, pide que trabaje en él y conserve su configuración.

---

Trabaja sobre mi mod de Minecraft Java **1.21.1 con Fabric**. Quiero implementar dos Mantis Blades montadas en los antebrazos del jugador, inspiradas en las referencias visuales adjuntas. Conserva el nivel de detalle de las guías visuales: cables enrutados, carcasa abierta, articulaciones, tornillos, pistones, electrodos o disipadores. No sustituyas el diseño por un modelo más sencillo. Utiliza los modelos y animaciones reales del paquete `mantis-kit`; lee primero su `LEEME.md`.

Antes de modificar el proyecto, revisa el modid, los mappings, Java, Fabric Loader, Fabric API, Loom y las dependencias existentes. Usa **GeckoLib 4 compatible con Fabric 1.21.1**, con una versión concreta comprobada en la fuente oficial. No mezcles código de Forge, NeoForge, Minecraft Bedrock o GeckoLib 5. Si el proyecto ya tiene otra solución de animación, explica la adaptación necesaria y conserva lo que funciona.

La corrección esencial de v0.4 es la transformación del propio brazo: cerrado se ve normal, con toda la mecánica oculta; al activarse se abren las cubiertas superiores y las dos cubiertas inferiores. No añadas puertas que se abran a izquierda y derecha del antebrazo. Conserva todas las cubiertas, los carros `dorsal_slide`/`rear_slide`, el elevador y los ocho huesos de piel retráctil, sus pivotes y las claves entregadas. Las placas inferiores `ventral_root` y `ventral_tip` son independientes. La hoja y los soportes se guardan 0.45 unidades más abajo que antes. No interpoles directamente del cierre a idle: las tapas deben despejar temporalmente la trayectoria, dejar subir el mecanismo, volver a su ángulo de reposo abierto y permitir después la extensión del filo. La hoja debe salir después de iniciar la apertura de la piel. No hagas aparecer el implante de golpe ni desaparezcas la piel durante la transición.

Implementa estas funciones:

1. Un implante izquierdo y otro derecho, con modelos reflejados y estados de animación independientes. Usa la skin real del jugador. Los grupos `reference_forearm` y `reference_hand` son referencias; no deben dibujarse con piel genérica en el juego. La mano debe seguir el hueso `wrist`, y las tapas de piel deben remapearse a la skin real. El atlas beige solo identifica material; crea los UV correctos de la skin y su segunda capa. Reconstruye mano y anclaje proximal siguiendo las piezas de referencia excluidas del JSON. Segmenta o enmascara el antebrazo vanilla: si dejas su cubo sólido debajo, tapará el hueco y los cables.
2. Variantes normal, térmica y eléctrica, usando las seis geometrías específicas entregadas (una por variante y lado). La térmica tiene sus canales y disipadores; la eléctrica tiene electrodos, aislantes y condensadores. Usa las máscaras emisivas mediante una capa registrada, y añade los efectos visuales apropiados sin confundir brillo completo con bloom.
3. Despliegue y repliegue con una tecla configurable, evitando conflictos de controles. Usa las animaciones de despliegue/repliegue de 1.0 segundos entregadas (los ataques duran 0.6 segundos).
4. Ataques alternados izquierda/derecha y regreso a la pose desplegada. El daño y cooldown deben ser controlados por el servidor. No confíes en un mensaje del cliente para infligir daño libremente.
5. Renderizado en primera persona de **ambas Mantis desplegadas y completas al mismo tiempo** durante la pose de combate; durante cada ataque una corta y la otra acompaña o guarda. Evita duplicar manos, recortar las puntas y separar el implante del brazo.
6. Renderizado en tercera persona visible para otros jugadores, adherido a la CARA EXTERNA de ambos antebrazos: el mecanismo derecho abre hacia la derecha anatómica y el izquierdo hacia la izquierda, a los laterales del cuerpo. No coloques ambos mecanismos en la cara frontal. Añade separación de los brazos respecto al torso en la pose de combate para que la apertura inferior no atraviese el cuerpo; comprueba también la ropa y armadura. Usa las escenas `*_montaje_lateral.bbmodel` y `montaje-referencia.json` para interpretar ejes y jerarquía. Deben seguir adheridos correctamente a ambos brazos durante caminar, correr, agacharse y atacar. Sincroniza variante y estado desplegado; evita que la animación de `arm_root` duplique la rotación del brazo vanilla.
7. Adaptación a brazos clásicos y Slim/Alex, y comportamiento definido al llevar armadura, nadar, usar otros ítems o cambiar de perspectiva.
8. Sonido, brasas térmicas y arcos eléctricos discretos, con recursos originales o cuya licencia permita incluirlos.

La revisión v0.4 incluye comprobación numérica de holguras entre soporte/hoja y piel/cubiertas en poses muestreadas, documentada en `HOLGURAS.txt`. Conserva esa prueba al editar animaciones; revisa también contactos con torso y armadura en el juego.

Los modelos son un prototipo; comprueba visualmente importación, escala, ejes y matrices. Las vistas del paquete se calcularon desde el JSON, pero **no demuestran que el renderizado esté probado dentro de Minecraft**. Si hay problemas, modifica los modelos editables o su generador y conserva los archivos de textura y animación coherentes.

Entrega cambios reales en archivos del proyecto, no solo pseudocódigo. Separa recursos, cliente y lógica de servidor. Compila el proyecto, ejecuta el cliente de desarrollo si el entorno lo permite y prueba ambas perspectivas y la sincronización. Muestra capturas reales del juego o renders del modelo, e identifica claramente de dónde sale cada imagen. No uses una imagen generada como prueba de que el modelo funciona.

Explica brevemente qué cambiaste, cómo lo probaste y qué queda pendiente. No afirmes que algo está listo si no pudo verificarse.
