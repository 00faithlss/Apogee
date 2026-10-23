# Vestimenta de facciones (referencias)

Investigación de referencias visuales para la vestimenta de las facciones de Cyberpunk que aparecerán en Apogee: Arasaka, Militech, Zetatech, Trauma Team y NetWatch.

## Estado

- **Texto descriptivo:** investigado por facción, con las fuentes citadas en cada archivo.
- **Imágenes de referencia:** pendientes. El entorno de investigación no pudo abrir los hosts de imágenes de la wiki (el proxy de salida bloqueó `cyberpunk.fandom.com`, `static.wikia.nocookie.net` y `cyberpunk.net`), así que ninguna URL de imagen directa fue verificada y no se descargó ninguna imagen.
- **Pendiente:** repetir la búsqueda de imágenes por ángulo (cabeza, torso, piernas, botas) cuando haya acceso a esos hosts, y verificar cada URL antes de anotarla.

## Archivos

| Facción | Archivo | Calidad de la fuente |
|---|---|---|
| Arasaka | [arasaka.md](arasaka.md) | Buena: colores, insignias y varias piezas de uniforme |
| Militech | [militech.md](militech.md) | Baja: casi solo nombres de productos |
| Zetatech | [zetatech.md](zetatech.md) | Muy baja: sin descripción visual |
| Trauma Team | [trauma-team.md](trauma-team.md) | Media: colores contradictorios entre fuentes |
| NetWatch | [netwatch.md](netwatch.md) | Muy baja: sin descripción visual |

## Licencia y uso de imágenes

Las imágenes de referencia son propiedad de CD PROJEKT RED y de los autores de las wikis. Por eso el plan es **enlazarlas desde su fuente**, no descargarlas ni redistribuirlas en el repositorio. Esto es coherente con [LICENSE_ASSETS.md](../../LICENSE_ASSETS.md), que ya exige registrar la procedencia de cualquier recurso de terceros.

## Cómo añadir una imagen

Al verificar una imagen, anotarla en la sección "Imágenes de referencia" de la facción correspondiente con esta forma:

- Ángulo: frente, lateral, espalda, 3/4 u otro.
- Pieza: cabeza, torso, piernas o botas.
- Página de origen y URL directa, comprobada con `curl -sIL -o /dev/null -w "%{http_code} %{content_type}\n" "<url>"` (debe devolver 200 e `image/*`).
