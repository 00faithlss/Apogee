# Cyberpunk Fabric

Proyecto de un mod cyberpunk para **Minecraft Java 1.21.1**, **Fabric** y **Java 21**, centrado en implantes, armas, armaduras, interfaces y HUD, con sonido y arte coherentes.

**Estado:** investigación y diseño inicial. El nombre es provisional. Este repositorio todavía no contiene un mod ejecutable, configuración de compilación ni una versión publicada.

## Objetivo

Crear una experiencia integrada con un núcleo propio y reutilización selectiva de componentes autorizados. Las prioridades son rendimiento medible, compatibilidad comprobada, progresión consistente y trazabilidad de los recursos utilizados.

## Investigación realizada

El [informe del 11 de septiembre de 2026](docs/research/2026-09-11-mods-cyberpunk.md) compara 14 proyectos y documenta fuentes, loaders, versiones, licencias, riesgos técnicos y una propuesta de desarrollo.

Hallazgos iniciales:

- Cyberware: Reforged es el primer candidato para estudiar adaptación de código por su licencia MIT; requiere revisión de archivos y port a Fabric.
- Cybernetics declara MIT, pero debe aclararse el alcance sobre código y contribuciones artísticas.
- Oritech incluye recursos de terceros con licencias distintas; se necesita revisión por archivo y versión.
- TaCZ separa código GPL-3.0 de recursos CC-BY-NC-ND-4.0, que no permiten distribuir adaptaciones bajo esa licencia.
- Los proyectos con derechos reservados sirven como referencias o posibles integraciones; copiar su contenido requiere autorización aplicable.

Estas conclusiones proceden de revisión documental. No se han ejecutado los mods ni medido su rendimiento o compatibilidad conjunta.

## Alcance propuesto

- Implantes por zona corporal, energía, temperatura y límites de instalación.
- Armas, accesorios y armaduras con reglas de combate consistentes.
- HUD configurable y pantalla de instalación con costes y efectos visibles.
- Texturas, modelos, animaciones y sonidos propios o con permisos documentados.
- Validación del estado en servidor, sincronización selectiva e integraciones opcionales.

## Primer prototipo previsto

Una estación de implantes, batería, tres mejoras (movilidad, defensa y visión), un arma, una armadura y HUD. Antes de ampliar contenido se comprobarán persistencia, retirada de atributos, multijugador y rendimiento.

## Plataforma y ramas

| Elemento | Decisión inicial |
|---|---|
| Minecraft | Java Edition 1.21.1 |
| Loader | Fabric |
| Java | 21 |
| `main` | Rama principal con avances revisados |
| `develop` | Integración del desarrollo para Fabric 1.21.1 |

Las versiones exactas de Fabric Loader, Fabric API, Loom y Gradle se fijarán al crear el proyecto compilable. Los cambios se registran en [CHANGELOG.md](CHANGELOG.md) y se describen mediante Conventional Commits. Las ramas de funcionalidades pueden usar el prefijo `codex/` y partir de `develop`.

## Próximos pasos

1. Auditar los archivos concretos de los candidatos seleccionados y registrar autor, origen, revisión, licencia y modificaciones.
2. Crear el esqueleto compilable para Fabric 1.21.1.
3. Implementar el prototipo pequeño y sus pruebas de persistencia y sincronización.
4. Medir rendimiento y verificar combinaciones concretas de mods antes de declarar compatibilidad.

## Licencias y recursos

La licencia de distribución del proyecto está pendiente de definición. Las licencias citadas en la investigación pertenecen a sus respectivos proyectos y no se aplican automáticamente a este repositorio.

En esta etapa solamente se incluye documentación propia de investigación y enlaces a fuentes. No se incorporan código, modelos, texturas ni sonidos de los mods analizados. El proyecto no está afiliado a CD PROJEKT RED ni a los autores de los mods referenciados.
