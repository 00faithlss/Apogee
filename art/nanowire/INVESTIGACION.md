# Investigación visual y técnica del nanocable

Investigación realizada el 30 de septiembre de 2026. Objetivo: crear un arma ficticia para Minecraft con apariencia, rig y movimientos inspirados en el Monowire de Cyberpunk 2077. No se copiaron modelos, texturas ni archivos de animación del juego.

## Qué es y qué se toma como referencia

El Monowire es un implante de brazos presentado como un hilo cortante de ciencia ficción. La lámina aportada por el usuario muestra el dispositivo por la cara interior de la muñeca, una ranura de salida, indicadores y superficies cerámicas para agarrar/manipular el cable. Las otras dos imágenes muestran su lectura como implante individual y la postura en primera persona sosteniendo el hilo entre ambas manos.

La guía oficial de CD Projekt RED sobre la build Hack-&-Slash Netrunner relaciona Monowire con combate cercano, Siphon y recuperación de RAM, además de sinergias con el cyberdeck Militech Paraline. Esto fundamenta su identidad como cyberware de combate vinculado a netrunning. No se implementó ese sistema de juego en esta entrega, ni se trasladaron cifras de balance al mod.

Fuente oficial: https://www.cyberpunk.net/en/news/50026/hack-slash-netrunner-build-breakdown

## Qué aporta Reddit

En el hilo «Can someone explain monowire to me?» el autor describe el intercambio entre manos en ataques normales y el uso bilateral en ataques fuertes. Los comentarios proponen explicaciones para el almacenamiento y la conexión del cable. Son observaciones e hipótesis de jugadores; no se usan como especificación oficial del mecanismo.

Decisión para esta adaptación: ambas muñecas llevan un mecanismo. La v0.3 recupera el hilo agarrado entre manos en reposo, con separación durante los ataques. Los golpes individuales barren en sentidos opuestos; el doble dibuja dos curvas en S.

Hilo consultado: https://www.reddit.com/r/cyberpunkgame/comments/zr9zjz/can_someone_explain_monowire_to_me/

También se localizaron discusiones sobre animaciones nuevas y clips a través de la piel. Sirven como puntos a revisar, no como demostración de que este asset esté libre de esos fallos:

- https://www.reddit.com/r/cyberpunkgame/comments/12ex9wm/
- https://www.reddit.com/r/cyberpunkgame/comments/1u79q68/monowire_clipping_issue_when_zooming_in/

## Cable flexible y color

La documentación de investigación de REDmodding identifica el hilo del juego como una spline con un material específico. Describe controles para grosor, oscilación y emisión, y advierte que la temperatura puede cambiar el color de negro a rojo, naranja, amarillo y blanco.

Decisión de implementación: no reproducir ese shader. Usar geometría segmentada con curvas horneadas para GeckoLib y un canal emisivo rojo fijo. El atlas y la máscara usan #FF0000 sin centro blanco ni degradado naranja. Son decisiones para satisfacer el color pedido por el usuario.

Fuente de los investigadores de modding: https://wiki.redmodding.org/cyberpunk-2077-modding/for-mod-creators-theory/materials/configuring-materials/spline-material-properties-monowire

## Videos encontrados y alcance real de su revisión

| Referencia | Qué se pudo verificar |
| --- | --- |
| «Cyberpunk 2077 - All Arm Cyberware Inspect, Attack, & Finisher Animations (All variants)», Spacefloppa | Página, autor, título y descripción visibles en YouTube; duración 16:19. El reproductor permaneció en negro/cargando. No se pudieron analizar sus fotogramas. |
| «Mantis blade SO overrated. Monowire is WAY better.», RazAghoulRitz | Página y descripción visibles; clip de 34 segundos, etiquetado también como modded. No se consiguió reproducir sus fotogramas. No se toma como referencia limpia del juego base. |
| «Monowire Rampage», GillBate5, Nexus Mods | Página indexada y descripción de combate en Arasaka Tower; publicada el 1 de diciembre de 2022. No se vio el video. Su fecha es anterior a 2.0. |

Enlaces:

- https://www.youtube.com/watch?v=F95xc-0aqaI
- https://www.youtube.com/watch?v=g9R8ghSQP4c
- https://www.nexusmods.com/cyberpunk2077/videos/998

Se consultó asimismo la discusión del autor de «Monowire Pre 2.0 Animations», donde se mencionan diferencias y problemas al reutilizar secuencias anteriores a 2.0. No se descargó ni reutilizó ese mod:
https://www.nexusmods.com/cyberpunk2077/mods/32078?tab=posts

**La v0.1 se diseñó sin poder ver los fotogramas de esos videos web. Para la v0.2 sí se revisó el MP4 adjunto descrito a continuación. Los siete clips actuales siguen siendo una adaptación original, no una extracción de las animaciones del juego.**

### Video adjunto revisado en v0.2

Se extrajeron y revisaron fotogramas de `v12044gd0000cf5c05bc77u2phmqcq60.mp4`, 512×910, 30 fps, 4,13 s. Se observan movimientos alternados al inicio; alrededor de 3,2–4,1 s aparece un barrido amplio con trazos de cable separados y a distinta altura. Se hizo una revisión más densa, cada 0,1 s, de ese tramo final. El motion blur impide atribuir cada trazo luminoso a un cable físico distinto; por eso se modelan dos cables, uno por brazo, sin duplicar las estelas como geometría.

La asignación del primer click al brazo derecho, segundo al izquierdo y del click derecho al ataque doble proviene del pedido del usuario, no de una lectura de los controles del video. El negro de la carcasa, el rojo puro, la eliminación de las piezas de mano y el montaje medial se ajustan a su captura anotada `IMG_8942.jpeg`.

## GeckoLib y Blockbench

La documentación del proyecto GeckoLib establece el uso de grupos/huesos para animar y la exportación separada de modelo y animaciones desde Blockbench. La guía de GeckoLib 4 documenta `AutoGlowingGeoLayer` y la convención de textura `_glowmask` para emisión.

- https://github.com/bernie-g/geckolib/wiki/Making-Your-Models-(Blockbench)
- https://github.com/bernie-g/geckolib/wiki/Emissive-Textures-(Geckolib4)

La copia local de `cyberpunk-fabric/gradle.properties` declara Minecraft 1.21.1 y GeckoLib 4.9.3. No se verificó una revisión remota nueva del repositorio. Los assets se entregan bajo `src/main/resources/assets/apogee/` para futura inclusión en el mod.

Blockbench web abrió su página, pero informó «Error creating WebGL context». Por eso se generaron los archivos mediante código, incluyendo proyectos editables y JSON de geometría/animación. La comprobación se hizo con un renderer de revisión, no con el editor ni con Minecraft. El informe `validation.json` especifica las comprobaciones realizadas.

### Revisión visual v0.3

Se reexaminaron los fotogramas del tramo 3,0–4,1 s del MP4. La adaptación enfatiza la forma de S señalada por el usuario en el golpe doble. La unión en reposo y los sentidos opuestos del combo son también requisitos explícitos del usuario. No se interpreta cada estela blanca del video como un cable adicional.
