# Viabilidad de un mod cyberpunk unificado para Minecraft

Investigación web realizada el 11 de septiembre de 2026. Base provisional: Minecraft Java 1.21.1, Fabric y Java 21, según gradle.properties y fabric.mod.json del proyecto actual. Se interpreta el pedido como mods cyberpunk para Minecraft, no como mods que se instalan dentro de Cyberpunk 2077.

## Conclusión

Es viable crear una experiencia unificada con implantes, armas, armaduras, interfaces y sonido coherentes. Recomiendo un mod propio para Fabric, con reutilización selectiva de material cuya licencia y procedencia estén claras. Unir varios proyectos completos elevaría el coste de mantenimiento y no garantiza rendimiento ni compatibilidad.

La investigación revisa fichas de autores, repositorios y declaraciones de licencia. No se ejecutaron los mods ni se midió su rendimiento. La compatibilidad indicada significa que existe una versión publicada o un entorno declarado; no equivale a compatibilidad probada entre ellos. Tampoco constituye una auditoría exhaustiva de cada archivo de sus JAR.

## Selección de mods

| Proyecto y fuente | Qué interesa | Entorno observado | Licencia y decisión preliminar |
|---|---|---|---|
| [Cyberware: Reforged](https://www.curseforge.com/minecraft/mc-mods/cyberware-reforged) | Cirugía, órganos, extremidades, ojos y costes de implantación | Publicación NeoForge 1.21.1; también otras versiones | MIT en ficha y repositorio. Primer candidato para estudiar adaptación de código; requiere port a Fabric y revisión de procedencia de recursos. |
| [Cybernetics](https://www.curseforge.com/minecraft/mc-mods/cybernetics) | Implantes, personalización corporal y HUD vinculado a mejoras | NeoForge 1.21.1 | MIT declarada, con un problema de alcance documental explicado abajo. Candidato condicionado. |
| [Oritech](https://modrinth.com/mod/oritech) | Tecnología, exotraje y progresión de aumentos | Fabric y NeoForge; [versión Fabric 1.21–1.21.1 con aumentos](https://modrinth.com/mod/oritech/version/0.14.0) | Licencias mixtas. Auditar por archivo; no tratar todo como CC0. |
| [Zoe Isn't Cyberpunk](https://modrinth.com/mod/zoe-isnt-cyberpunk) | Implantes, software, armas y progresión configurable | Forge 1.20.1 | Apache-2.0 declarada. Interesante, pero no encontré enlace al código fuente en la ficha/API consultada. Port y recursos pendientes de auditoría. |
| [Cyberbop](https://www.curseforge.com/minecraft/mc-mods/cyberbop) | Montar y controlar un cuerpo robótico por piezas | Fabric 1.21.1, versión beta | [All Rights Reserved](https://www.curseforge.com/minecraft/mc-mods/cyberbop/license). Referencia de diseño o dependencia; no base para copiar sin permiso. |
| [Project: Edgerunner](https://modrinth.com/mod/project-edgerunner) | Instalación de implantes, escáner, mejoras de brazos y sobrecarga | Forge 1.20.1; addon de Palladium | Modrinth declara CC-BY-NC-SA-4.0; CurseForge muestra Custom License. Posible adaptación no comercial bajo condiciones; resolver discrepancia y alcance antes. |
| [Cybernetic System](https://www.curseforge.com/minecraft/mc-mods/cybernetic-system) | Terminales, energía, mejoras genéticas y armamento láser | NeoForge 1.21.1 / Forge 1.20.1 | All Rights Reserved. Referencia o integración autorizada. |
| [Minepunk](https://www.curseforge.com/minecraft/mc-mods/minepunk) | Implantes, armas y ambientación de Cyberpunk 2077 | NeoForge 1.21.1 | All Rights Reserved. No reutilizar código ni arte sin autorización. |
| [CyberCraft: Blockrunners](https://www.curseforge.com/minecraft/mc-mods/cybercraft-blockrunners) | Quickhacks, facciones, persecuciones y misiones | NeoForge 1.21.1; desarrollo temprano | All Rights Reserved. El autor advierte interferencias con otros mods. Baja prioridad como base técnica. |
| [Cyberpunk Armor](https://www.curseforge.com/minecraft/mc-mods/cyberpunk-armor) | Armaduras y exoesqueleto | Forge 1.20.1 | All Rights Reserved. Útil para comparar estilos, no para extraer modelos o texturas. |
| [CyberHUD](https://www.curseforge.com/minecraft/mc-mods/cyberhud) | Barras de estado compactas de estética tecnológica | Archivo Fabric 1.21.1 publicado | All Rights Reserved. Diseñar HUD propio o usarlo como mod separado. |
| [ReWIRED](https://www.curseforge.com/minecraft/mc-mods/rewired) | Barreras, camuflaje y defensa con coste energético | Forge 1.12.2 | All Rights Reserved en la ficha. No hereda automáticamente MIT por ser addon de Cyberware. |
| [Vic's Point Blank](https://www.curseforge.com/minecraft/mc-mods/vics-point-blank) | Armas animadas, accesorios y creación de paquetes | Archivo Fabric 1.21.1 publicado | All Rights Reserved. Buen candidato a dependencia con armas propias; no a absorber su motor o sus recursos. |
| [TaCZ](https://github.com/MCModderAnchor/TACZ/blob/1.20.1/readme.md) | Motor de armas y ecosistema de paquetes | La rama revisada es Forge 1.20.1 | Código GPL-3.0; recursos CC-BY-NC-ND-4.0. Reutilización del código condicionada a GPL; recursos no aptos para distribuir modificaciones. |

El [port no oficial TaCZ Refabricated revisado](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated) publica versiones para 1.21.11 y 26.x. No lo presento como una solución confirmada para Fabric 1.21.1. 1.21.1 y 1.21.11 son versiones diferentes.

## Hallazgos de licencias que cambian la decisión

**Cyberware: Reforged.** Su [LICENSE](https://github.com/w1shadel/Cyberware-Reforged/blob/main/LICENSE) contiene MIT y permite modificación y combinación conservando los avisos correspondientes. Es la evidencia más clara encontrada para comenzar a estudiar código. Antes de incorporar archivos hay que fijar una revisión y conservar también avisos de autores anteriores y de terceros que correspondan.

**Cybernetics.** CurseForge y [gradle.properties](https://github.com/Perigrine33/Cybernetics/blob/main/gradle.properties) declaran MIT. Sin embargo, el archivo devuelto por la API de licencias de GitHub es [TEMPLATE_LICENSE.txt](https://github.com/Perigrine33/Cybernetics/blob/main/TEMPLATE_LICENSE.txt), limitado expresamente a la plantilla de NeoForge. Esto no anula por sí solo la declaración del autor, pero tampoco permite atribuir a ese archivo cobertura de todo el mod. El [README](https://github.com/Perigrine33/Cybernetics/blob/main/README.md) acredita modelos a Artistic VoidK y sonidos no vanilla a Tar. Hay que documentar el alcance de la licencia para esas contribuciones.

**Oritech.** [Modrinth](https://modrinth.com/mod/oritech) declara CC-BY-4.0, mientras que el [LICENSE.md de la rama predeterminada consultada](https://github.com/Rearth/Oritech/blob/26.1/LICENSE.md) contiene CC0. Los créditos identifican recursos de Techarium bajo CC-BY-NC-4.0 y texturas de malcolmriley bajo CC-BY-4.0. Entre los recursos atribuidos a Techarium está el modelo de exotraje. Por ello, precisamente una de las piezas más atractivas para este proyecto no debe considerarse libre de restricciones comerciales. Revisar la rama y versión que realmente se vaya a usar, no extrapolar la licencia de 26.1 a un archivo de 1.21.1.

**Project: Edgerunner.** [Modrinth](https://modrinth.com/mod/project-edgerunner) especifica CC-BY-NC-SA-4.0. [CurseForge](https://www.curseforge.com/minecraft/mc-mods/project-edgerunner/license) identifica una licencia personalizada; la extracción de esa página no permitió confirmar un texto sustantivo equivalente. Mantenerlo como candidato condicionado a cotejar el archivo distribuido y las condiciones del autor.

**TaCZ.** La separación entre código y recursos está explicitada por el propio proyecto. GPL permite modificaciones con obligaciones al distribuir el trabajo derivado; no significa que todo proyecto independiente instalado a su lado deba convertirse en GPL. En cambio, fusionar su código dentro del nuevo mod requiere evaluar las obligaciones para el conjunto derivado. La licencia [CC-BY-NC-ND-4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/) permite compartir material sin adaptar, con atribución y para fines no comerciales, pero no distribuir material adaptado. Dar créditos no elimina ND.

Cada paquete de armas tiene su propia licencia: que el motor admita paquetes no permite reutilizar los modelos de cualquiera de ellos. Point Blank enlaza un paquete comunitario de Cyberpunk 2077, pero su mención no confirma derechos para incorporarlo al proyecto nuevo.

## Regla práctica para código, imágenes y sonidos

| Situación | Tratamiento propuesto |
|---|---|
| MIT | Reutilizar material cubierto, conservando copyright y licencia. |
| Apache-2.0 | Conservar licencia, avisos aplicables y NOTICE si existe; señalar archivos modificados. |
| GPL-3.0 | Preparar cumplimiento de fuente correspondiente, avisos y licencia del trabajo derivado distribuido. |
| CC0 / CC-BY | Opciones útiles para recursos originales del licenciante; BY exige atribución e indicar cambios. |
| CC-BY-NC-SA | Adaptación sujeta a no comercial, atribución y compartir adaptaciones con la licencia exigida. |
| CC-BY-NC-ND | No distribuir adaptaciones del recurso sin otro permiso. |
| All Rights Reserved / sin licencia verificable | No incorporar copias al mod nuevo sin permiso aplicable. |

Referencias normativas: [MIT del candidato](https://github.com/w1shadel/Cyberware-Reforged/blob/main/LICENSE), [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0), [GNU GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html), [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) y [CC-BY-NC-ND-4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/). Esto es una evaluación preliminar de reutilización; la decisión final corresponde al material exacto y su licencia.

Para cada archivo incorporado, registrar origen, autor, versión o commit, ruta, hash, licencia, modificaciones y atribución. Una carpeta llamada assets no demuestra que todos sus contenidos compartan licencia. Tampoco basta que un repositorio sea público.

Separar la estética cyberpunk general del contenido concreto de Cyberpunk 2077. La licencia de un mod no concede por sí sola derechos sobre música, voces o material extraído del juego. Las [directrices de CD PROJEKT RED](https://www.cdprojektred.com/en/fan-content) también contemplan derechos de terceros. Recomiendo identidad, corporaciones, iconografía y audio propios.

## Qué combinaría en el diseño

Esta sección es una propuesta propia basada en los referentes; no describe funcionalidades ya implementadas.

1. **Implantes:** ranuras por zona corporal; consumo energético, carga térmica y capacidad de instalación. Mejoras con ventajas y costes legibles. Estudiar primero Cyberware: Reforged.
2. **Armas:** un único sistema de daño, munición, recarga y accesorios. Primera entrega con pistola, arma de energía y hoja integrada. Armas inteligentes después de validar sincronización y coste por objetivo.
3. **Armaduras:** conjuntos ligero, táctico y pesado. Protección, movilidad y refrigeración en equilibrio; exotrajes originales para evitar importar un recurso restringido.
4. **HUD:** batería, temperatura, munición, habilidad activa y escáner. Elementos movibles y escalables, con modos mínimo/completo y efectos visuales opcionales.
5. **Interfaces:** pantalla de cirugía con comparación antes/después, incompatibilidades visibles y explicación del consumo. Guía integrada y recetas consistentes.
6. **Sonido y arte:** lenguaje visual común; sonidos diferenciados para instalación, activación, agotamiento, recarga e impacto. Avisos importantes con subtítulos y volumen configurable.

Para prototipos de audio, [Kenney Sci-fi Sounds](https://kenney.nl/assets/sci-fi-sounds) y [Interface Sounds](https://kenney.nl/assets/interface-sounds) declaran CC0. Son candidatos reutilizables; no se descargaron ni se hizo una evaluación auditiva. Para el resultado final, seleccionar y ajustar sonidos que encajen entre sí o producirlos expresamente. Conservar el archivo de licencia del paquete.

La selección definitiva de texturas y modelos queda pendiente de la auditoría por archivo. Esta investigación no identificó una colección completa de armas, armaduras y HUD con todos sus recursos autorizados para fusionar sin condiciones adicionales.

## Arquitectura propuesta para optimización y compatibilidad

Mantener un producto único con módulos internos: estado del jugador, implantes, combate, equipo, interfaz, recursos e integraciones. No hacen falta siete mods instalables para organizar bien el código.

- El servidor valida instalación, energía, daño, munición y enfriamientos. El cliente presenta animaciones y efectos.
- Sincronizar cambios de estado y eventos; evitar enviar toda la información cada tick.
- Calcular atributos al cambiar equipo o estado; no reconstruirlos continuamente.
- Escáner con radio, frecuencia y cantidad de objetivos limitados. Evitar búsquedas globales de entidades por fotograma.
- Efectos, luces y partículas con presupuestos y reducción por distancia. El HUD básico debe funcionar sin shader.
- Mantener los recursos gráficos fuera de la inicialización del servidor dedicado.
- Usar eventos y puntos de extensión disponibles; reservar mixins para necesidades concretas, evitando reemplazos completos de inventario, movimiento o HUD.
- Definir recetas, etiquetas, estadísticas y compatibilidad mediante datos cuando sea práctico. Incorporar migraciones del estado guardado.
- Adaptadores opcionales para inventarios, accesorios, armas y visores externos; comportamiento claro cuando una dependencia falta.

Un efecto de aceleración tipo Sandevistan necesita especial cuidado: cambiar globalmente la velocidad del mundo afecta a otros jugadores y sistemas. Propondría velocidad/reflejos personales y efectos controlados sobre objetivos compatibles. Una ralentización más amplia podría quedar como opción separada para un jugador, con límites explícitos.

Estas son decisiones de diseño para reducir riesgos, no garantías medidas. La calidad de un mod no se deduce de su número de descargas, de su loader o de que esté hecho con MCreator.

## Pruebas que harían defendible decir «optimizado y compatible»

Comparar el mismo mundo, equipo, distancia de renderizado y recorrido antes y después de activar el mod. Medir tiempo de tick, tiempos de fotograma —incluidos picos—, memoria y tráfico de red. Probar combate y escaneo con cantidades crecientes y reproducibles de entidades.

Matriz mínima propuesta:

- Fabric 1.21.1 con dependencias mínimas; cliente y servidor dedicado.
- Muerte, reaparición, cambio de dimensión, desconexión y recarga del mundo.
- Instalación repetida, retirada de implantes, batería vacía y cambios de configuración.
- Dos jugadores activando habilidades simultáneamente, incluyendo latencia simulada.
- Sodium/Iris y escalas de GUI diferentes, usando versiones compatibles verificadas al implementar.
- Trinkets, Satchels y mods que alteren movimiento, inventario, combate o HUD que realmente use el usuario.
- Actualización de versión conservando implantes; ausencia de duplicaciones y modificadores acumulados.

Las pruebas deben producir una lista de combinaciones y versiones soportadas. No prometer compatibilidad con «todos los mods».

## Ruta de desarrollo recomendada

**Primero:** fijar si el producto será privado, público o potencialmente comercial; mantener provisionalmente Fabric 1.21.1. Elegir archivos concretos de Cyberware: Reforged para auditoría y determinar si sale mejor adaptarlos o implementar el sistema propio.

**Prototipo funcional:** estación de implantes, batería, tres mejoras —movilidad, defensa y visión—, un arma, una armadura y HUD. Debe demostrar guardado, sincronización y retirada correcta de atributos antes de multiplicar contenido.

**Versión jugable pequeña:** tres familias de armas, varios implantes diferenciados, conjunto de armadura modular, guía, recetas, sonido y arte coherentes. Integrar recursos nuevos únicamente con procedencia documentada.

**Expansión:** quickhacks, enemigos especializados y progresión más amplia, después de las mediciones y pruebas de convivencia.

Alternativa si se prioriza jugar pronto: un modpack con una capa de integración y configuración. Alternativa si se prioriza un mod autónomo y control total: núcleo Fabric propio. Para este pedido recomiendo la segunda, con Cyberware: Reforged como primer referente técnico y audio/arte propios o con licencia clara.

## Límites y pendientes concretos

No se modificó código del proyecto, instalaron mods, contactaron autores ni descargaron recursos de terceros. Falta la auditoría de archivos de las versiones elegidas y la prueba real de rendimiento. En Cybernetics hay que aclarar cobertura de contribuciones; en Oritech separar licencias por recurso y rama; en Edgerunner resolver las declaraciones entre plataformas; en Zoe localizar fuentes y avisos de los archivos a reutilizar. Los candidatos ARR necesitan autorización para copiar material, o una implementación y recursos independientes.
