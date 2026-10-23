# Guía de preparación para Blockbench

Fecha: 2026-10-23. Estas son **decisiones propuestas para Apogee**, no especificaciones oficiales de Cyberpunk ni modelos ya construidos.

## Entorno real del proyecto

Se revisó `../../gradle.properties`: Minecraft **Java 1.21.1**, **Fabric**, GeckoLib **4.9.3** y Trinkets **3.10.0**. Para armaduras con volumen propio, la ruta más coherente es evaluar **GeckoLib 4 / Armor**. No elegir un proyecto Bedrock ni exportar un JSON de objeto esperando que se equipe automáticamente sobre el cuerpo.

La [documentación de armaduras de GeckoLib 4](https://github.com/bernie-g/geckolib/wiki/Geckolib-Armor-%28Geckolib4%29) exige seleccionar el tipo Armor y explica el modelo, objeto y renderizador necesarios. La página mezcla ejemplos de distintas versiones y plataformas: al integrar, contrastar la API con la dependencia 4.9.3 del proyecto. Esta investigación no añade ni prueba código de renderizado.

## Paquete de trabajo por conjunto

1. Elegir una variante y tres referencias principales. Fijar la época: 2020, RED o 2077.
2. Crear el proyecto de armadura con la plantilla compatible de GeckoLib. Conservar sus pivotes y nombres estructurales; añadir piezas bajo los grupos que correspondan. No renombrar huesos raíz basándose en tutoriales de otras versiones.
3. Hacer primero silueta gris: casco, torso, hombros, cadera, piernas y botas. Revisar frente, espalda y perfiles.
4. Propuesta inicial: textura **128 × 128** por conjunto, filtrado visual tipo pixel art. Subir a 256 × 256 solo si el estilo general del mod y la cantidad de detalles lo justifican; no es una exigencia del motor.
5. Guardar el `.bbmodel` editable y exportar geometría `.geo.json`, textura `.png` y animaciones `.animation.json` únicamente si se usan. Preparar también los iconos de inventario de las cuatro piezas.
6. Integrar y probar el conjunto en Minecraft antes de producir variantes. Un modelo bonito dentro de Blockbench todavía no demuestra que se equipe bien en el juego.

## División por ranura y movimiento

| Ranura | Elementos recomendados | Riesgo que comprobar |
|---|---|---|
| Casco | Carcasa, visor, orejeras y respirador | Giro de cabeza, choque con cuello; visibilidad de la skin |
| Pechera | Chaleco, collar, brazos, hombros y equipo posterior | Hombros al usar armas; mochila, élitros y cyberware |
| Pantalones | Cinturón, faldones cortos, muslos y rodillas | Agacharse, sentarse y separar piernas |
| Botas | Punteras, suelas y parte baja de espinillas | Solapamiento con pantalones y pies durante la marcha |

Las hombreras deben seguir a su brazo y cada faldón a la parte pertinente. No cruzar ambas piernas con un único cubo rígido. Proponer 0,25–0,5 unidades de separación inicial entre superficies superpuestas y ajustar mediante pruebas; es una tolerancia de diseño, no una regla universal. Mantener el detalle diminuto en textura y usar geometría donde cambie la silueta.

## Identidad que debe sobrevivir a la simplificación

| Facción | Tres rasgos que conservar | Primera versión |
|---|---|---|
| Arasaka | Casco oscuro, cuello alto, paneles y rojo puntual | Soldado táctico |
| Militech | Oliva, bolsas modulares, casco con orejeras/visor | Operador de mangas completas |
| Trauma Team | Casco blanco, mono verde, chaleco claro y rojo | Médico de intervención |
| NetWatch | Camisa/corbata, cuello mecánico, brazos oscuros | Agente; cuatro piezas como adaptación jugable |
| MaxTac | Casco con seis ópticas, chaleco oscuro, cuello alto | Conjunto SQ030; botas por reconstruir |
| Zetatech | Coraza abultada, respirador, grandes guanteletes | Grasshopper; color propio y época explícita |

## Orden sugerido

El usuario eligió comenzar por **MaxTac**. Después se puede continuar con **Trauma Team → Arasaka → Militech → NetWatch → Zetatech**. NetWatch requiere decidir cómo representar cyberware sobre una skin; Zetatech requiere reconstruir vistas y elegir entre Grasshopper histórica y una adaptación de 2077.

El segundo conjunto iniciado es [Trauma Team](../../art/trauma-team/LEEME.md), con máscara abatible, lámina interior e indicadores rojos intermitentes.

## Criterios de aceptación del futuro modelo

- Se reconoce la facción a 8–16 bloques por silueta y masas de color, sin depender del texto del logo.
- Las cuatro piezas funcionan por separado y combinadas con armadura vanilla.
- Pruebas con brazos clásicos y delgados, distintas skins y capas exteriores.
- Revisar caminar, correr, agacharse, nadar, sentarse, apuntar y atacar; cabeza en extremos de giro.
- Comprobar convivencia con Cuchillas Mantis, Nanocable y Sandevistan de Apogee; son zonas donde el volumen puede chocar.
- Comparar primera y tercera persona. Evitar prometer brazos visibles en primera persona sin revisar el renderizador correspondiente.
- No dejar caras superpuestas que parpadeen; revisar transparencias, encantamiento y luces emisivas si se incorporan.
- Registrar qué caras se copiaron como referencia visual y cuáles fueron reconstruidas. Exportar las cuatro vistas del modelo aprobado.

## Estado de esta entrega

La investigación se conserva como base de diseño. MaxTac ya tiene una [primera versión `.bbmodel`, texturas emisivas, exportación GeckoLib e integración equipable](../../art/maxtac/LEEME.md), iniciada por elección del usuario. El visor alterna dos y seis ópticas con N. Se comprobó la cobertura con skins clásicas y delgadas dentro de Minecraft; el balance usa netherita provisional, con receta propia. La revisión completa de animaciones y convivencia con ciberware sigue pendiente.
