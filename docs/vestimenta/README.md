# Vestimenta de facciones — dossier visual para Apogee

Investigación revisada el **23 de octubre de 2026** para preparar futuras armaduras de Minecraft en Blockbench.

## Empezar aquí

Abrir **[galeria.html](galeria.html)**: galería local de **33 imágenes**, con filtros por facción, época y búsqueda. Funciona sin conexión para las imágenes; los portfolios y fuentes externas necesitan Internet. Cada imagen puede abrirse a su tamaño original.

Después, consultar la ficha de la facción y la **[guía para Blockbench](GUIA_BLOCKBENCH.md)**. Los archivos descargados están en `imagenes/`, con procedencia, créditos y SHA-256 en **[referencias.json](referencias.json)** y un índice legible en **[FUENTES.md](FUENTES.md)**.

## Cobertura y decisiones

| Facción | Imágenes | Base recomendada | Estado real |
|---|---:|---|---|
| [Arasaka](arasaka.md) | 7 | Soldado táctico 2077 | Casco y cuerpo bien documentados; espalda del conjunto exacto incompleta |
| [Militech](militech.md) | 7 | Operador oliva 2077 | Buenas variantes de casco; falta espalda limpia del operador |
| [Trauma Team](trauma-team.md) | 6 | Médico de intervención 2077 | Vistas de cuerpo y casco; diferenciar médico, piloto y uniforme equipable |
| [NetWatch](netwatch.md) | 4 | Agente con camisa y cyberware | Cuerpo completo, perfil, espalda y calzado confirmados visualmente |
| [MaxTac](maxtac.md) | 7 | Conjunto táctico SQ030 y captura oficial | Casco, torso y pantalón en frente/espalda; botas pendientes |
| [Zetatech](zetatech.md) | 2 | Grasshopper 2020/RED | Armadura oficial localizada; color, espalda y versión 2077 sin confirmar |

## Qué se completó

- Se descargaron y examinaron las 24 imágenes que antes estaban solo enlazadas; se añadieron dos referencias de Grasshopper para Zetatech.
- Se localizaron portfolios de artistas de producción, con créditos y enlaces directos al proyecto.
- Se corrigieron clasificaciones: traje de guardia no equivale a soldado, NetWatch no usa botas militares en la referencia, y los cascos Militech mostrados no incluyen la espalda limpia que sugería la tabla anterior.
- Se sustituyeron los huecos descriptivos por observaciones de forma, materiales aparentes, color y piezas; se separaron claramente las propuestas de modelado.
- Se localizaron pistas concretas de personal Zetatech en Phantom Liberty, sin presentar como confirmado un uniforme que no se pudo inspeccionar.

## Cómo leer las fichas

**Observado** significa visible en una imagen descargada y examinada. **Confirmado por fuente** significa respaldado por el portfolio o publicación citados. **Propuesto** significa adaptación artística para Apogee. Una imagen de concepto no garantiza una coincidencia exacta con el modelo final; un dibujo monocromo no confirma paleta; una búsqueda sin resultado no prueba que un diseño no exista.

La investigación ya permite iniciar los modelos. Los huecos restantes son de vistas concretas o decisiones artísticas, detallados por facción. No se inventaron vistas faltantes ni se generaron imágenes que puedan confundirse con material oficial.

## Imágenes y procedencia

Por la solicitud de reunir imágenes para uso futuro, esta revisión conserva copias locales de referencia. Reemplaza el plan anterior de mantener solo enlaces. Están en documentación, fuera de los recursos del juego, y no se incorporaron al JAR.

Los derechos de las imágenes corresponden a sus titulares; Fandom es el alojamiento de la copia, no necesariamente el autor. Se distinguen CD PROJEKT RED, artistas identificados y material de R. Talsorian Games. El uso privado del proyecto y su registro de procedencia están en [LICENSE_ASSETS.md](../../LICENSE_ASSETS.md). No se ha verificado una licencia de redistribución para estas imágenes.

La descarga de las 33 imágenes dio HTTP 200, contenido `image/*` y decodificación correcta. Algunas páginas de Fandom y descargas automatizadas de ArtStation fueron bloqueadas: eso se documenta en [FUENTES.md](FUENTES.md). Los portfolios se consultaron mediante el navegador de investigación; el archivo local evita depender del hotlink del CDN.

## Elegir las referencias definitivas

En `galeria.html`, usa **Elegir referencia** en cada imagen. Puedes escoger varias por facción, indicar su uso (conjunto, casco, torso, piernas, botas o detalles) y escribir notas. **Ver elegidas** muestra únicamente tu selección. **Confirmar selección** fija su estado como confirmado; cualquier cambio posterior vuelve a dejarla en revisión.

La selección se guarda automáticamente en este navegador y esta dirección. No se escribe directamente en los archivos del proyecto. **Exportar selección** descarga una copia JSON recuperable con **Importar selección**; **Descargar ficha** genera un resumen Markdown con imágenes, fuentes y notas para el modelado. Exporta para conservar una copia independiente o llevarla a otro navegador/dirección. Al importar se combinan las referencias del archivo con las actuales, sin borrar las demás.

MaxTac añadido: seis capturas del conjunto equipable y una captura del CDN oficial. Total actualizado: 33 imágenes de seis facciones.
