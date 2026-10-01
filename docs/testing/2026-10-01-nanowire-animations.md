# Revisión de animaciones del Nanocable

Rama: `feat/animations`, creada desde `origin/feat/ripperdoc` (`95e9892`).

## Referencias del autor

- `videoplayback_y3K5ksw9.mp4`: 18,67 s, 1920 × 1080, 30 fps. Hasta aproximadamente 12,5 s muestra poses y arcos del monowire; después muestra otra arma. Tiene largos segmentos casi congelados, así que sirve sobre todo para estudiar siluetas y la separación de los dos cables.
- `v12044gd0000cf5c05bc77u2phmqcq60.mov`: 4,13 s, 576 × 1024. Muestra el agarre, barridos de un brazo, tajos dobles y lazos amplios en movimiento. Se usó como referencia principal del gesto y de la recuperación.

Los videos permanecen fuera del repositorio. Las proporciones se adaptaron a los brazos rígidos de Minecraft; no se copiaron modelos ni texturas de Cyberpunk.

## Cambios

El movimiento del brazo distingue tirón, golpe, seguimiento y recogida. Primera y tercera persona usan las mismas fases, con movimientos adaptados a sus cámaras. El brazo de apoyo suelta el cable con una transición suave.

Los latigazos simples tienen arcos más abiertos. Su primer tramo va exactamente del agarre al punto entre las manos: antes se avanzaba la distancia de ese tramo sobre una curva dirigida al primer punto del ataque, por lo que la punta no empezaba en el punto sostenido. Se limita el cable desplegado a la distancia recorrida para que las muestras no se acumulen en la raíz. Las estelas duran hasta 60 ms, con cinco copias y desvanecimiento progresivo.

Tras la observación del autor sobre el clic derecho, se reemplazó su recorrido de punta por una curva que transmite el tirón de la mano hacia el extremo con retraso. Se integran direcciones unitarias a lo largo de un cable de largo limitado: la raíz empieza a bajar mientras la punta aún queda arriba. Las tres variantes preparan por encima del agarre y terminan el golpe por debajo, variando su inclinación lateral. Es una animación procedural de propagación, no una simulación con colisiones físicas.

En primera persona, el signo de la rotación X estaba bajando la mano cuando la traslación intentaba subirla. Se corrigió ese signo y se quitó el desplazamiento hacia delante del golpe; la amplitud deja las manos visibles durante el descenso.

Al solaparse un latigazo simple con el doble, ambas vistas dan prioridad al doble, como ya hacía el cable. La pose de tercera persona usa desplazamientos angulares continuos para evitar cambiar de objetivo de rotación al cruzar cero.

Daño, alcance, cadencia, recarga, red y recursos GeckoLib conservan sus valores. Los cambios están en el renderer procedural y las poses del Nanocable.

El alcance nominal en supervivencia es de 7 bloques. Para cubrir esa distancia con una curva, el cable visual admite 8,4 bloques en el golpe simple y 9,1 en el doble. Antes admitía 5,88 y 7, respectivamente, y el barrido simple solo llegaba unos 5 bloques hacia delante. Ahora su punto más adelantado se escala a los 7 bloques. El cable del ataque doble aumenta un 30 % sin modificar su movimiento descendente.

## Verificación

- `./gradlew build`: compila el JAR y ejecuta las comprobaciones numéricas.
- `./gradlew nanowireMotionTest`: comprueba uniones con el reposo, simetría de barridos, avance continuo de la punta, muestras finitas y cable desplegado limitado al recorrido. Incluye manos cruzadas y agarres coincidentes. Para el ataque doble comprueba largo máximo, preparación arriba, golpe abajo y raíz descendiendo antes que la punta.
- Se comprueba que el barrido simple llegue al alcance nominal hacia delante y que ambos cables del ataque doble alcancen esa distancia en el momento de impacto, incluidos los estilos con retraso del brazo izquierdo.
- `./gradlew runNanowireSmoke`: prueba de cliente con equipamiento, despliegue, ambos brazos, los tres estilos dobles, interrupción de un latigazo simple por clic derecho y repliegue.
- Comparación de Minecraft real antes/después a 1280 × 720, con el mismo jugador (`NanowireReview`), FOV y escenario, mediante el script de cliente. Son capturas por tick a 20 Hz; no son una grabación de todos los fotogramas renderizados.

Los resultados y las capturas locales se guardan en `build/nanowire-animation-review`; `tools/nanowire_review.py` genera la comparación en MP4 y GIF. Estos artefactos están ignorados por Git.

La corrección específica del clic derecho tiene su comparación con la revisión anterior en `build/nanowire-whip-review/preview`, usando `--heavy-only --before-label='ANTES / lanzamiento frontal' --after-label='DESPUES / latigazo descendente'`.

La comparación del aumento de largo se guarda en `build/nanowire-length-review/preview`, frente a la versión del latigazo descendente anterior.

Las pruebas visuales de esta revisión usan el renderer normal. No se repitieron las pruebas con Punchy, Iris, Essential ni una sesión multijugador.
