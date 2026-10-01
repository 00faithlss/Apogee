# Mesa matasanos — distribución inspirada en Project: Edgerunner

Revisión visual para Minecraft Java 1.21.1 / Fabric. Es un paquete de assets editables y una maqueta interactiva; no es un mod compilado ni una integración Java terminada.

## Revisión del diseño

La galería pública de Project: Edgerunner 1.1.0 muestra un Ripperdoc con fondo oscuro, detalles cian, selección magenta, jugador central, categorías a ambos lados e inventario inferior. Esta revisión reconstruye esa distribución con assets propios, conserva slots de 18×18 y añade nuestra anatomía con skin translúcida. No incluye código ni texturas extraídos de ese mod.

Referencias: https://modrinth.com/mod/project-edgerunner/gallery y https://www.curseforge.com/minecraft/mc-mods/project-edgerunner/gallery . La publicación consultada es Forge 1.20.1; esta maqueta sigue destinada a Fabric 1.21.1.

Se añadieron espacios Manos y Tegumentario como futuras categorías. Se retiró la barra de carga y su etiqueta. También se retiraron las indicaciones visuales Clic: mover, Mayús + clic: transferir y Esc / E: salir. Los iconos Mantis proceden de los renders de nuestros modelos 3D de inventario; en esta demostración son PNG de 16×16, mientras que la pantalla Java final podrá renderizar el ItemStack 3D.

## Cambios

- Se eliminaron la X superior y los botones Instalar, Retirar y Cerrar.
- Clic toma, coloca o intercambia un implante. Mayús + clic lo transfiere a una ranura compatible libre. Esc o E cierra la maqueta y devuelve el objeto del cursor a una ranura para conservarlo.
- La vista de rayos X responde al contenido de la ranura Brazos: sin Mantis, física, térmica o eléctrica. El cambio afecta a ambos antebrazos.
- Se eliminaron los implantes decorativos permanentes del avatar base. Cerebro, huesos y nervios son anatomía, no equipo instalado.
- Las demás categorías permanecen como espacios de diseño. No se simula que sus mejoras ya estén implementadas.

## Archivos para abrir

| Archivo o carpeta | Contenido |
| --- | --- |
| `mesa-matasanos-demo.html` | Demostración local autocontenida; abrir en un navegador. |
| `mesa-matasanos.bbmodel` | Composición editable con GUI de referencia y Mantis térmicas instaladas. |
| `jugador-rayos-x.bbmodel` | Anatomía base y skin de muestra sin implantes. |
| `modules/mantis_xray_<variante>_pair.bbmodel` | Módulo bilateral independiente para el avatar de diagnóstico. |
| `modules/mantis_xray_<variante>_<lado>.bbmodel` | Cada antebrazo por separado. Variantes: normal, thermal, electric; lados: left, right. |
| `states/` | Avatar ensamblado con cada variante, o sin Mantis. |
| `combat-source/` | Seis modelos completos de combate de la revisión local anterior, copiados sin alterar geometría ni animaciones. |
| `implant-manifest.json` | Correspondencia entre IDs reales del mod y módulos de diagnóstico. |
| `layout.json` | Posiciones de slots y controles; `actions` vacío. |
| `previews/` | Muestras renderizadas desde los modelos. |
| `source/` | Generadores y comprobación de interacción. |

Los modelos de diagnóstico son estáticos, plegados y adaptados al antebrazo del avatar. No sustituyen a los modelos de combate. La adaptación uniforme a la escala del avatar se aplica durante la construcción del asset: no es una animación que estire la hoja o el riel. Cada mecanismo conserva piezas independientes; la piel exterior la aporta el avatar.

Texturas incrustadas en los `.bbmodel`. En Blockbench: Archivo → Abrir modelo. El formato de los módulos nuevos es Free Model. No colocar estos archivos directamente en `assets/apogee/geo`: primero requieren exportación y un renderer de interfaz. Los grupos nuevos tienen transformación neutra; las posiciones y rotaciones están horneadas en cada cubo.

## Regla para futuras mejoras

El estado autorizado del equipo debe ser la única fuente para elegir módulos. Al equipar, reemplazar o retirar un implante, se vuelve a resolver el conjunto de capas visibles del avatar. Cambiar la selección del inventario no instala nada. La vista de diagnóstico muestra equipo instalado aunque sus hojas estén retraídas.

Para cada nuevo implante se añadirá un identificador real, ranura, modelo, punto de anclaje y, si procede, un aspecto por nivel. No basta con recolorear todo el esqueleto. Ejemplos de diseño pendientes: sistema operativo sobre la columna, ópticas dentro de la cabeza, refuerzos sobre los huesos y bomba circulatoria en el torso. No hay en este paquete assets ni implementación de esos ejemplos.

En Java, la pantalla se abrirá al interactuar con la futura mesa. El servidor validará cambios de slots, compatibilidad y devolución de objetos; el cliente dibujará el estado sincronizado. Usar la tecla configurada para inventario, cuyo valor predeterminado es E, además de Esc. La demo usa E fija. Resolver la skin del jugador conectado en tiempo de ejecución: la PNG incluida es únicamente una muestra Classic de 64×64; el modelo Slim requiere adaptación.

El render de anatomía y módulos deberá respetar profundidad, dibujando después la piel translúcida. El renderer de combate actual usa recorte de transparencia y no sirve sin adaptar para esta piel. Los PNG de la demo son vistas prerenderizadas: la integración final debe dibujar el modelo 3D con la skin real.

## Verificación y límites

- Geometría, UUID, referencias de textura y jerarquías comprobadas por script.
- Muestras revisadas visualmente; rotaciones locales de cubos aplicadas en el renderer de revisión.
- 16 comprobaciones de interacción en un DOM simulado: intercambio, restricciones, cierre, conservación de objetos y variantes. No equivalen a una prueba del navegador ni del juego.
- Blockbench web falló al crear el contexto WebGL, incluso después de recargar. Los `.bbmodel` se generaron programáticamente; no pudieron abrirse y validarse en el editor durante esta sesión.
- El conector GitHub devolvió 404 para el repositorio y su README en la rama indicada. El análisis corresponde al ZIP local y sus revisiones locales, no a la rama remota actual. Ver `ANALISIS-REPO.md`.
- Sin compilación, commit ni push. No se modificó el código Java del proyecto.

## Regenerar las muestras

Dependencias Python: Pillow, NumPy y SciPy. Node.js para comprobar la interacción.

```sh
python3 source/build_modules.py
python3 source/build_ui.py
python3 source/render_modules.py
python3 source/build_preview.py
node source/check_demo.cjs
```

Mantener `combat-source/` y los archivos base incluidos. Las texturas tienen filtrado nearest-neighbor y la GUI está diseñada a 480×356 píxeles; la adaptación a ventanas pequeñas queda pendiente de integración.
