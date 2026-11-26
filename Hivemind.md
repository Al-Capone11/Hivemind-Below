# Diseño y gestión del proyecto --- Mod de colonias subterráneas

> Documento maestro de diseño (GDD) y guía de organización técnica.\
> Estado: especificación inicial. Las decisiones marcadas como
> **definidas** se consideran acordadas; las marcadas como **propuesta**
> todavía pueden ajustarse.

------------------------------------------------------------------------

## 1. Visión general

El mod introduce una especie hostil subterránea organizada en colonias
independientes. Cada colonia gira alrededor de una reina que funciona
como centro de reproducción, memoria colectiva, coordinación y toma de
decisiones.

Los integrantes de una colonia exploran el subsuelo, cazan criaturas
para alimentarse, recolectan recursos, construyen y mantienen colmenas,
defienden su territorio y responden a las amenazas. La colonia puede
aprender de sus encuentros con el jugador y modificar sus tácticas.

El objetivo principal no es crear enemigos con más vida o daño, sino un
ecosistema que parezca organizado y que reaccione a las acciones del
jugador.

### Pilares del diseño

1.  **Organización social:** cada criatura pertenece a una colonia y
    cumple una función.
2.  **Inteligencia centralizada:** la reina conserva la memoria
    colectiva y coordina las prioridades.
3.  **Autonomía local:** las criaturas pueden ejecutar tareas y
    reaccionar sin recibir una orden nueva en cada instante.
4.  **Colonias independientes:** cada colonia aprende y evoluciona por
    separado.
5.  **Mundo dinámico:** las colonias cazan, recolectan, construyen,
    reparan y se expanden.
6.  **Consecuencias:** atacar repetidamente una colonia cambia su
    comportamiento.
7.  **Punto vulnerable definitivo:** matar a la reina extermina la
    colonia.

------------------------------------------------------------------------

## 2. Identidad biológica y apariencia

### 2.1 Concepto visual

La especie debe parecer un organismo que evolucionó para vivir bajo
tierra. La referencia general es un artrópodo cavernícola, pero no una
hormiga gigante convencional.

La anatomía combina rasgos de insectos, artrópodos y depredadores
subterráneos. Todas las castas deben compartir una identidad biológica
reconocible.

### 2.2 Rasgos compartidos

-   Exoesqueleto duro, similar a la quitina.
-   Cuerpo segmentado, con cabeza, tórax y abdomen diferenciados.
-   Extremidades articuladas adaptadas a cuevas y terrenos irregulares.
-   Mandíbulas para alimentarse, transportar materiales y defenderse.
-   Antenas u órganos sensoriales para detectar vibraciones, movimiento
    y señales químicas.
-   Adaptación a la oscuridad: visión limitada compensada por otros
    sentidos.
-   Pigmentación principalmente grisácea, marrón, blanquecina u oscura.
-   Variaciones anatómicas entre castas derivadas de una estructura
    corporal común.

### 2.3 Dirección artística propuesta

La dirección recomendada es una combinación de:

-   Cuerpos orgánicos y segmentados.
-   Zonas de exoesqueleto oscuro y resistente.
-   Partes más pálidas en las zonas menos expuestas.
-   Diferencias visibles entre castas sin que parezcan especies
    distintas.
-   Aspecto naturalista y biológico, evitando una apariencia mecánica o
    tecnológica.

La especie debe resultar inquietante por su anatomía y comportamiento,
no depender exclusivamente de efectos visuales o elementos
sobrenaturales.

------------------------------------------------------------------------

## 3. Castas

Todas las castas pertenecen a la misma especie. Se diferencian por
tamaño, proporciones, extremidades, atributos y comportamiento.

### 3.1 Reina

**Función:** reproducción, memoria colectiva, coordinación y decisiones
estratégicas.

Características:

-   Es la criatura de mayor tamaño.
-   Abdomen muy desarrollado para la producción de huevos.
-   Exoesqueleto grueso y resistente.
-   Cabeza relativamente pequeña en proporción al cuerpo.
-   Antenas y órganos sensoriales desarrollados.
-   Extremidades robustas, no especializadas en perseguir objetivos a
    largas distancias.
-   Permanece principalmente en la cámara central de la colmena.

Comportamiento:

-   Coordina las prioridades de la colonia.
-   Recibe y procesa información transmitida por los integrantes.
-   Conserva los registros de encuentros y amenazas.
-   Decide prioridades de defensa, exploración, caza y expansión.
-   Produce huevos cuando existen recursos y condiciones suficientes.
-   Se defiende si un intruso alcanza su cámara, pero no funciona como
    un enemigo perseguidor convencional.

La reina es el único punto vulnerable necesario para exterminar una
colonia.

### 3.2 Explorador

**Función:** reconocimiento, detección y transmisión de información.

Características:

-   Cuerpo pequeño, ligero y alargado.
-   Patas relativamente largas.
-   Antenas prominentes.
-   Exoesqueleto más delgado que el del combatiente.
-   Buena movilidad y capacidad para desplazarse por túneles y terrenos
    irregulares.

Comportamiento:

-   Explora cuevas y zonas próximas al territorio.
-   Detecta jugadores, presas, recursos y amenazas.
-   Evita enfrentamientos innecesarios.
-   Regresa a la colmena para transmitir información.
-   Puede perderse o morir antes de comunicar lo que descubrió.

Los exploradores son una parte importante del sistema de memoria: la
reina no debe conocer automáticamente todo lo que ocurre en el mundo.

### 3.3 Combatiente

**Función:** defensa, caza y enfrentamiento directo.

Características:

-   Cuerpo ancho y robusto.
-   Exoesqueleto grueso.
-   Mandíbulas grandes.
-   Extremidades delanteras fuertes, posiblemente similares a pinzas.
-   Menor velocidad que el explorador, pero mayor resistencia.

Comportamiento:

-   Defiende la colmena y sus cámaras importantes.
-   Caza animales y otras criaturas.
-   Protege a exploradores y obreros cuando sea necesario.
-   Participa en incursiones y ataques coordinados.
-   Modifica sus tácticas según las adaptaciones aprendidas por la
    colonia.

Su identidad debe estar en el combate coordinado, no únicamente en
causar más daño.

### 3.4 Obrero

**Función:** construcción, mantenimiento, transporte y recolección.

Características:

-   Tamaño intermedio.
-   Mandíbulas adaptadas para cortar y transportar materiales.
-   Extremidades delanteras fuertes.
-   Exoesqueleto de resistencia moderada.
-   Abdomen adaptado al transporte temporal de alimento o recursos.

Comportamiento:

-   Construye y repara cámaras y túneles.
-   Transporta alimento y materiales.
-   Mantiene las instalaciones de la colmena.
-   Participa en la expansión.
-   Atiende las cámaras de incubación.
-   Evita el combate cuando puede, pero puede defenderse si es atacado.

### 3.5 Escala inicial

Las dimensiones son valores de referencia para el diseño y deben
probarse dentro del juego.

  Casta           Altura aproximada
  ------------- -------------------
  Explorador            0,8 bloques
  Obrero                   1 bloque
  Combatiente           1,5 bloques
  Reina                3--4 bloques

La escala final debe considerar colisiones, navegación, animaciones,
accesibilidad de túneles y equilibrio.

### 3.6 Implementación de entidades

Para reducir duplicación, se recomienda compartir una clase base o
componentes comunes para:

-   Identificador de colonia.
-   Estados de actividad.
-   Comunicación.
-   Detección de amenazas.
-   Necesidades y tareas.
-   Navegación y retorno a la colmena.

Las castas pueden implementarse como entidades separadas si sus
dimensiones, atributos, modelos o comportamientos lo justifican. No es
obligatorio forzar todas las diferencias en una única entidad
configurable.

------------------------------------------------------------------------

## 4. Colonias

### 4.1 Independencia

Cada colonia es una unidad completamente independiente.

Cada una posee:

-   Identificador único.
-   Una reina.
-   Memoria propia.
-   Población.
-   Territorio.
-   Una colmena principal.
-   Posibles asentamientos secundarios.
-   Recursos y nivel de desarrollo.
-   Adaptaciones aprendidas.

Dos colonias cercanas no comparten memoria, información ni órdenes. La
proximidad no las convierte en una misma organización.

### 4.2 Identificador de colonia

Cada entidad integrante debe conocer el identificador de su colonia. Ese
identificador permite consultar los datos persistentes correspondientes.

Los asentamientos secundarios pertenecen a la colonia principal y
utilizan el mismo identificador. No tienen reina ni memoria
independientes.

### 4.3 Estados de una colonia

La colonia debe manejar un estado persistente:

``` java
enum ColonyState {
    ACTIVE,
    COLLAPSING,
    EXTERMINATED
}
```

-   `ACTIVE`: la reina está viva y la colonia funciona.
-   `COLLAPSING`: la reina murió; se ejecuta el proceso de exterminio.
-   `EXTERMINATED`: el proceso terminó y la colonia no puede
    recuperarse.

La colonia debe considerarse funcionalmente exterminada desde el inicio
de `COLLAPSING`, aunque la limpieza de entidades y datos todavía esté en
curso.

------------------------------------------------------------------------

## 5. Colmenas y asentamientos

### 5.1 Funciones

La colmena es el centro físico de la colonia. Cumple funciones de:

-   Refugio.
-   Reproducción.
-   Coordinación.
-   Almacenamiento.
-   Descanso.
-   Defensa.
-   Construcción y mantenimiento.

### 5.2 Cámaras

Una colmena puede contener diferentes tipos de cámaras:

  -----------------------------------------------------------------------
  Cámara                              Función
  ----------------------------------- -----------------------------------
  Central                             Alberga a la reina y concentra la
                                      actividad de la colonia.

  Incubación                          Contiene huevos y permite la
                                      reproducción.

  Descanso                            Permite que las criaturas
                                      permanezcan inactivas o recuperen
                                      su estado.

  Almacenamiento                      Guarda alimento y materiales.

  Defensiva                           Facilita la protección de entradas
                                      y zonas importantes.

  Túneles                             Conectan las cámaras y permiten el
                                      desplazamiento.
  -----------------------------------------------------------------------

No todas las colmenas necesitan tener todas las cámaras. Su composición
depende del tamaño y desarrollo de la colonia.

### 5.3 Generación natural

-   Algunas colmenas aparecen durante la generación del mundo.
-   Se encuentran principalmente en cuevas y sistemas subterráneos.
-   Pueden existir colmenas de distintos tamaños y niveles de
    desarrollo.
-   Su población inicial debe corresponder a su tamaño.
-   La generación debe evitar estructuras imposibles de alcanzar o que
    bloqueen de manera injustificada la progresión del jugador.

### 5.4 Construcción autónoma

Los obreros pueden construir cámaras, túneles y asentamientos
secundarios.

Los asentamientos secundarios:

-   No tienen reina propia.
-   Pertenecen a la colonia principal.
-   Comparten su identificador y memoria.
-   Dependen de la reina principal para funcionar.
-   Deben quedar inactivos cuando la colonia es exterminada.

La construcción debe estar limitada por recursos, espacio, población y
condiciones del entorno.

### 5.5 Colmenas abandonadas

Las estructuras pueden permanecer en el mundo después del exterminio de
una colonia.

Una colmena abandonada es un vestigio físico: ya no tiene población
activa, reproducción, coordinación ni capacidad de expansión.

------------------------------------------------------------------------

## 6. Ciclo de actividad

La actividad de la especie cambia según el momento del día, pero no
depende exclusivamente del horario.

### Durante el día

-   Actividad general reducida.
-   Combatientes descansan o permanecen cerca de zonas importantes.
-   Obreros pueden continuar tareas de construcción y mantenimiento.
-   Exploradores realizan recorridos limitados.
-   La reina permanece en su cámara central.

### Durante la noche

-   Aumenta la actividad general.
-   Exploradores reconocen el territorio.
-   Combatientes patrullan y cazan.
-   Pueden organizarse incursiones.
-   Aumenta la probabilidad de encontrar integrantes lejos de la
    colmena.

### Respuesta a emergencias

Una invasión, un ataque o una amenaza importante puede activar a los
defensores incluso durante el día.

Las criaturas deben regresar a la colmena cuando:

-   Terminan su tarea.
-   Están heridas.
-   Se alejan demasiado.
-   Reciben una orden de repliegue.
-   La reina prioriza la defensa del núcleo.

Las criaturas no mueren por exposición a la luz solar.

------------------------------------------------------------------------

## 7. Alimentación y recursos

### 7.1 Caza

La colonia caza otras criaturas para alimentarse y obtener recursos.

Posibles presas iniciales:

-   Vacas.
-   Ovejas.
-   Cerdos.
-   Conejos.
-   Gallinas.
-   Otras entidades compatibles.

La selección de presas debe considerar el riesgo, la distancia, la
disponibilidad y la capacidad de la colonia. No todas las criaturas del
mundo deben ser objetivos automáticos.

### 7.2 Recolección y transporte

-   Los combatientes participan principalmente en la caza.
-   Los obreros pueden transportar alimento y materiales a la colmena.
-   Los recursos se depositan en cámaras de almacenamiento.
-   Los recursos pueden representarse inicialmente mediante contadores y
    estados, sin exigir un inventario completo por entidad.

### 7.3 Uso de recursos

Los recursos pueden utilizarse para:

-   Reproducción.
-   Mantenimiento de la población.
-   Construcción de cámaras y túneles.
-   Reparación de estructuras.
-   Expansión territorial.

La disponibilidad de recursos debe limitar el crecimiento para evitar
una reproducción ilimitada.

------------------------------------------------------------------------

## 8. Memoria colectiva e inteligencia

### 8.1 Principio general

La memoria pertenece a la colonia y está centralizada en la reina. No
pertenece a toda la especie ni se comparte entre colonias.

La reina procesa la información que recibe y la utiliza para decidir
cómo responder a amenazas y oportunidades.

No debe tener conocimiento omnisciente del mundo.

### 8.2 Transmisión de información

La reina puede recibir información mediante:

-   Exploradores que regresan.
-   Integrantes que presencian enfrentamientos.
-   Mensajeros o criaturas que transportan información.
-   Señales entre integrantes cercanos, si se implementa ese sistema.

Un explorador eliminado antes de regresar no debe transmitir
automáticamente lo que descubrió.

### 8.3 Memoria del jugador

La colonia debe mantener un registro por UUID de jugador.

Datos sugeridos:

-   UUID del jugador.
-   Nivel de amenaza.
-   Número de encuentros.
-   Bajas causadas a la colonia.
-   Daño estructural.
-   Tácticas de combate identificadas.
-   Fecha o momento del último encuentro.
-   Adaptaciones aprendidas.

### 8.4 Niveles de amenaza

  -----------------------------------------------------------------------
  Nivel                               Significado
  ----------------------------------- -----------------------------------
  Desconocido                         La colonia no tiene información
                                      relevante sobre el jugador.

  Detectado                           El jugador fue localizado, pero no
                                      representa todavía una amenaza
                                      importante.

  Amenaza                             Ha causado daños o interferido con
                                      actividades de la colonia.

  Hostil                              Ha atacado repetidamente o causado
                                      pérdidas importantes.

  Prioridad máxima                    Representa una amenaza crítica para
                                      la supervivencia de la colonia.
  -----------------------------------------------------------------------

Los cambios de nivel deben depender de eventos y criterios medibles, no
de incrementos arbitrarios.

### 8.5 Adaptación de comportamiento

Las adaptaciones deben producir cambios tácticos observables.

  -----------------------------------------------------------------------
  Conducta del jugador                Posible respuesta
  ----------------------------------- -----------------------------------
  Combate cuerpo a cuerpo             Intentar rodearlo o atacar desde
                                      varios ángulos.

  Ataques a distancia                 Buscar rutas alternativas y reducir
                                      la exposición.

  Uso frecuente de fuego              Evitar zonas incendiadas o accesos
                                      afectados.

  Invasión de la colmena              Reforzar entradas y aumentar la
                                      vigilancia.

  Destrucción de estructuras          Priorizar la protección de cámaras
                                      importantes.

  Ataques repetidos                   Aumentar patrullajes y
                                      persecuciones en el territorio.
  -----------------------------------------------------------------------

No todas las colonias deben desbloquear necesariamente todas las
adaptaciones. El sistema debe tener límites para evitar que una colonia
se vuelva invencible.

------------------------------------------------------------------------

## 9. Territorio y desarrollo

### 9.1 Territorio

Cada colonia posee un territorio propio que determina:

-   Zonas de patrullaje.
-   Alcance de exploración.
-   Recursos que considera accesibles.
-   Zonas en las que puede construir.
-   Espacios que defiende.
-   Distancia a la que sus integrantes deben regresar.

Los territorios de colonias diferentes pueden acercarse o superponerse
parcialmente, pero esto no implica que compartan memoria ni
organización.

### 9.2 Etapas de desarrollo

  -----------------------------------------------------------------------
  Etapa                               Características
  ----------------------------------- -----------------------------------
  Incipiente                          Población pequeña y pocas cámaras.

  Establecida                         Población estable, almacenamiento y
                                      reproducción.

  Desarrollada                        Mayor territorio, defensas y
                                      asentamientos secundarios.

  Dominante                           Red de asentamientos y mayor
                                      capacidad territorial.
  -----------------------------------------------------------------------

El desarrollo debe depender de recursos, población, espacio disponible y
supervivencia de la reina.

------------------------------------------------------------------------

## 10. Muerte de la reina y exterminio

Esta es una regla central del diseño:

**Matar a la reina es la única condición que necesita cumplir el jugador
para exterminar una colonia.**

No es necesario matar a todos los integrantes, destruir los huevos ni
demoler cada cámara.

### 10.1 Proceso

Cuando muere la reina:

1.  La colonia pasa inmediatamente de `ACTIVE` a `COLLAPSING`.
2.  Se detiene la reproducción.
3.  Se interrumpe la coordinación y la emisión de órdenes.
4.  Se invalida la memoria de la colonia.
5.  Los asentamientos secundarios dejan de funcionar.
6.  Los integrantes supervivientes son eliminados automáticamente por el
    proceso de colapso.
7.  Los huevos y elementos funcionales de la colonia dejan de
    representar una fuente de reproducción.
8.  La colonia pasa a `EXTERMINATED`.
9.  Las estructuras pueden permanecer como colmenas abandonadas.

### 10.2 Persistencia del exterminio

El exterminio debe ser irreversible.

El sistema debe impedir que:

-   Entidades descargadas vuelvan a activar la colonia.
-   Entidades que se carguen posteriormente reconstruyan su estado.
-   Los huevos generen una nueva reina.
-   Un asentamiento secundario continúe funcionando.
-   Se regenere la memoria o se cree una sucesora.

La marca de exterminio debe guardarse en datos persistentes y
comprobarse cuando se carguen entidades o estructuras asociadas a esa
colonia.

### 10.3 Limpieza técnica

La limpieza debe ser segura y compatible con chunks descargados. No se
debe depender de que todas las entidades estén cargadas simultáneamente.

La muerte de la reina debe registrar el estado de colapso en los datos
persistentes. A partir de ahí, cada entidad o estructura asociada debe
comprobar el estado de su colonia al cargarse o actualizarse.

------------------------------------------------------------------------

## 11. Arquitectura técnica

La implementación debe separar entidades, lógica de colonias, memoria,
IA, estructuras y persistencia.

### 11.1 Organización lógica sugerida

``` text
com.modname
├── entity
│   ├── QueenEntity
│   ├── ScoutEntity
│   ├── WarriorEntity
│   └── WorkerEntity
├── colony
│   ├── ColonyManager
│   ├── ColonyData
│   ├── ColonyState
│   └── ColonyTerritory
├── memory
│   ├── MemoryManager
│   ├── PlayerMemory
│   ├── ThreatEvaluator
│   └── EncounterTracker
├── ai
│   ├── QueenController
│   ├── CommunicationSystem
│   ├── HuntingGoals
│   ├── DefenseGoals
│   └── AdaptationController
├── hive
│   ├── HiveManager
│   ├── HiveGenerator
│   ├── HiveExpansion
│   └── HiveBlockEntities
├── reproduction
│   ├── BreedingManager
│   └── PopulationManager
├── resources
│   ├── ResourceManager
│   └── HuntingManager
└── world
    ├── SpawnManager
    └── WorldData
```

Es una separación conceptual, no una obligación de conservar exactamente
esos nombres o paquetes.

### 11.2 Persistencia

Los datos de las colonias deben persistir independientemente de las
entidades cargadas.

En Minecraft Java, utilizar el mecanismo de datos persistentes del mundo
correspondiente a cada versión (por ejemplo, `SavedData` cuando
aplique).

`ColonyData` debe contener, como mínimo:

-   UUID de colonia.
-   Referencia o UUID de la reina.
-   Estado de la colonia.
-   Referencias a colmenas y asentamientos.
-   Población registrada.
-   Etapa de desarrollo.
-   Territorio.
-   Memoria por jugador.
-   Adaptaciones aprendidas.
-   Recursos relevantes.

La reina es el centro lógico de la memoria, pero los datos no deben
existir exclusivamente dentro de su entidad. Esto evita perder el estado
de la colonia al descargar chunks o eliminar accidentalmente una
entidad.

Al morir definitivamente la reina, los datos deben invalidarse y
limpiarse según el proceso de exterminio.

### 11.3 Entidades y bloques

Separar responsabilidades:

-   **Entidades:** reina, exploradores, combatientes y obreros.
-   **Bloques:** paredes orgánicas, huevos, túneles y bloques de
    cámaras.
-   **Block Entities:** núcleos de colmena o almacenamiento cuando
    necesiten datos propios.
-   **Datos persistentes del mundo:** estado, memoria e identificadores
    de colonias.

### 11.4 Rendimiento

El sistema debe evitar una IA costosa en todas las entidades y en todos
los chunks.

Criterios:

-   No ejecutar cálculos complejos en cada tick si no son necesarios.
-   Actualizar la memoria mediante eventos.
-   Limitar la frecuencia de decisiones estratégicas de la reina.
-   Mantener objetivos de IA simples y específicos por casta.
-   Evitar búsquedas globales frecuentes de entidades.
-   No mantener simulación completa de entidades descargadas.
-   Utilizar simulación simplificada para colonias fuera de los chunks
    activos.
-   Limitar población, expansión, distancia de exploración y número de
    tareas simultáneas.
-   Evitar que cada entidad consulte continuamente todos los datos de la
    colonia.

------------------------------------------------------------------------

## 12. Plan de desarrollo

El desarrollo debe realizarse por fases. Cada fase debe tener una
versión funcional y verificable antes de añadir sistemas más complejos.

### Fase 1 --- Núcleo funcional

-   Crear la estructura del proyecto.
-   Registrar las entidades y castas.
-   Implementar la reina.
-   Crear una colmena básica.
-   Asignar identificadores de colonia.
-   Persistir datos de colonia.
-   Registrar encuentros con jugadores.
-   Verificar carga, descarga y guardado del mundo.

**Criterio de salida:** una colonia básica puede existir, guardar su
estado y reconocer a sus integrantes.

### Fase 2 --- Inteligencia y exterminio

-   Implementar comunicación.
-   Registrar bajas y daños.
-   Calcular niveles de amenaza.
-   Implementar memoria por jugador.
-   Añadir primeras adaptaciones.
-   Implementar muerte de la reina.
-   Implementar colapso y exterminio irreversible.

**Criterio de salida:** una colonia reconoce amenazas, adapta algunas
tácticas y desaparece funcionalmente al morir su reina.

### Fase 3 --- Ecosistema

-   Implementar caza.
-   Añadir recursos.
-   Añadir transporte y almacenamiento.
-   Implementar ciclos de actividad.
-   Implementar reproducción.
-   Controlar población.
-   Añadir tareas de construcción y reparación.

**Criterio de salida:** la colonia puede mantener una población y
realizar actividades básicas sin depender de la presencia constante del
jugador.

### Fase 4 --- Mundo dinámico

-   Generar colmenas naturales.
-   Añadir cámaras y variantes de colmena.
-   Implementar asentamientos secundarios.
-   Añadir expansión autónoma.
-   Añadir interacción con otras criaturas.
-   Implementar territorio y etapas de desarrollo.

**Criterio de salida:** las colonias forman parte del mundo y pueden
desarrollarse de manera autónoma.

### Fase 5 --- Optimización y contenido

-   Optimizar navegación e IA.
-   Balancear población y dificultad.
-   Mejorar modelos y animaciones.
-   Añadir sonidos y efectos.
-   Añadir configuraciones.
-   Probar compatibilidad con otros mods.
-   Verificar mundos antiguos, chunks descargados y migraciones de
    datos.

**Criterio de salida:** el mod es estable, configurable y está listo
para distribución.

------------------------------------------------------------------------

# 13. Gestión del proyecto: multiloader y multiversión

## 13.1 Objetivo

La matriz mantiene Fabric y un segundo cargador por versión. El segundo
cargador es NeoForge en 26.3, 26.2 y 1.21.1, y Forge mediante LegacyForge
en 1.20.1.

Las versiones de Minecraft son:

1.  26.3.
2.  26.2.
3.  1.21.1.
4.  1.20.1.

La arquitectura debe reducir la duplicación de código, pero no debe
forzar a que todas las versiones utilicen exactamente las mismas
implementaciones cuando las API o los sistemas internos de Minecraft
sean diferentes.

## 13.2 Estructura del espacio de trabajo

La carpeta madre de Hivemind contiene un único repositorio Git bare, un
worktree por versión mantenida y el `CONTEXT.md` local.

```text
Hivemind/
├── CONTEXT.md
├── .bare/
├── Hivemind - 26.3/       [main]
├── Hivemind - 26.2/       [26.2]
├── Hivemind - 1.21.1/     [1.21.1]
└── Hivemind - 1.20.1/    [1.20.1]
```

Cada rama contiene `fabric/`, `neoforge/` y `Core/` en su raíz, junto con
el wrapper de Gradle, `build-version.ps1` y la comprobación del núcleo.
En 1.20.1, el módulo `neoforge/` usa LegacyForge y produce un JAR para Forge.

## 13.3 Núcleo compartido

`Core/` contiene la lógica común y está versionado dentro de cada rama.
Ambos loaders incorporan las fuentes de la copia de su propio worktree.
Cada rama puede compilar sus dos loaders con los archivos del checkout.

La comprobación del núcleo usa `verify-core.ps1`. Para compilar el par de
una rama, usa `build-version.ps1 -Version 0.1.0` desde su raíz.
`main` también contiene el orquestador `build-all.ps1` y el único
`beta-sequence.txt` para coordinar la matriz de las ocho variantes.

## 13.4 Responsabilidad de cada capa

### Core --- lógica común

Debe contener la lógica que pueda compartirse sin depender directamente
de las API específicas de Minecraft o de un loader.

Ejemplos:

-   Modelo de datos de colonias.
-   Estados de colonia.
-   Memoria por jugador.
-   Evaluación de amenazas.
-   Reglas de adaptación.
-   Lógica de población y recursos.
-   Decisiones estratégicas de la reina.
-   Reglas de exterminio.
-   Interfaces para entidades, mundo, almacenamiento y comunicación.

El núcleo debe evitar importar directamente clases de Fabric, NeoForge o
clases internas de Minecraft cuando sea posible.

### Módulo de versión

Cada versión debe encargarse de las diferencias propias de Minecraft:

-   Cambios de API.
-   Registro de entidades y bloques.
-   Componentes y datos persistentes.
-   Generación de estructuras.
-   Navegación y objetivos de IA.
-   Serialización.
-   Recursos y metadatos.
-   Compatibilidad con las herramientas de compilación de esa versión.

### Módulo Fabric

Contiene exclusivamente la integración con Fabric:

-   Inicializadores.
-   Registro mediante API de Fabric.
-   Eventos específicos.
-   Configuración de Fabric.
-   Dependencias y metadatos del loader.

### Módulo NeoForge y excepción Forge 1.20.1

Contiene la integración con NeoForge en 26.3, 26.2 y 1.21.1. En 1.20.1 contiene la integración con Forge mediante LegacyForge. Sus responsabilidades son:

-   Inicializadores.
-   Registro mediante API de NeoForge.
-   Eventos específicos.
-   Configuración de NeoForge.
-   Dependencias y metadatos del loader.

No se deben introducir referencias de Fabric dentro del módulo NeoForge
ni referencias de NeoForge dentro del módulo Fabric.

## 13.5 Diferencias entre versiones

No se debe asumir que las cuatro versiones comparten las mismas API,
mappings, dependencias, versiones de Java o herramientas de compilación.

Cada branch debe declarar y mantener explícitamente:

-   Versión exacta de Minecraft.
-   Loader y versión del loader.
-   Versión de Java requerida.
-   Versión de Gradle y plugin de compilación.
-   Mappings correspondientes.
-   Dependencias.
-   Configuración de publicación.
-   Cambios de API que requieran adaptadores.

Las versiones 1.20.1 y 1.21.1 pueden requerir adaptaciones importantes
frente a las versiones 26.x. La lógica del núcleo debe mantenerse lo más
independiente posible de esas diferencias.

------------------------------------------------------------------------

# 14. Convenciones de Git y GitHub

## 14.1 Ramas de versión

| Rama | Minecraft | Cargadores |
| --- | --- | --- |
| `main` | 26.3 | Fabric y NeoForge |
| `26.2` | 26.2 | Fabric y NeoForge |
| `1.21.1` | 1.21.1 | Fabric y NeoForge |
| `1.20.1` | 1.20.1 | Fabric y Forge vía LegacyForge |

`main` es la rama predeterminada. Cada línea estable avanza desde el
historial existente. El tag `v0.1.0` identifica la instantánea original
del lanzamiento. Conserva ese tag y publica la reorganización de
`main` como avance normal desde el commit original.

## 14.2 Trabajo diario

Antes de comenzar:

1.  Confirmar en qué carpeta y branch se está trabajando.
2.  Consultar el estado de Git.
3.  Si hay que sincronizar, revisar primero el remoto y los cambios locales.
4.  Revisar los cambios pendientes.
5.  Trabajar únicamente en la versión prevista, salvo que la tarea
    requiera modificar el núcleo común.

Antes de cambiar de versión:

1.  Guardar los cambios.
2.  Crear commits coherentes.
3.  Comprobar que no quedan modificaciones accidentales.
4.  Abrir la carpeta de la versión de destino.

No copiar archivos completos entre versiones sin revisar las diferencias
de API y configuración.

## 14.3 Convención de commits

Usar mensajes breves y descriptivos. Se recomienda una convención
similar a:

``` text
feat(core): add colony threat evaluation
feat(fabric): register scout entity
feat(neoforge): register hive blocks
fix(1.21.1): correct colony data loading
fix(core): prevent exterminated colonies from reloading
refactor(ai): separate scout and warrior goals
docs: update colony design
build(26.3): update loader dependencies
```

Prefijos recomendados:

-   `feat`: funcionalidad nueva.
-   `fix`: corrección.
-   `refactor`: reorganización sin cambio funcional intencional.
-   `perf`: optimización.
-   `docs`: documentación.
-   `test`: pruebas.
-   `build`: configuración, Gradle o dependencias.
-   `chore`: mantenimiento general.

## 14.4 Ramas temporales

Las ramas de trabajo deben derivarse del branch de versión
correspondiente.

Ejemplos:

``` text
feature/colony-memory
feature/queen-collapse
feature/hive-generation
fix/worker-navigation
refactor/common-ai
```

Al terminar, integrar los cambios mediante merge o pull request y
eliminar la rama temporal si ya no es necesaria.

## 14.5 Sincronización del núcleo

Desarrolla y prueba los cambios en `Core/` en la línea correspondiente.
Aplica los cambios compartidos en cada rama afectada y comprueba la
paridad del núcleo. Verifica los dos loaders de cada rama y registra
las adaptaciones de API y las diferencias deliberadas en las variantes.

------------------------------------------------------------------------

# 15. Reglas de organización del código

1.  **No duplicar lógica sin motivo.** Si una regla de comportamiento es
    común, debe vivir en Core o en una abstracción compartida.
2.  **No forzar abstracciones innecesarias.** Si una API cambió
    sustancialmente, utilizar adaptadores específicos de versión.
3.  **Separar lógica y plataforma.** El núcleo decide qué debe ocurrir;
    los módulos de Minecraft ejecutan la acción mediante sus API.
4.  **No mezclar loaders.** Fabric y NeoForge deben compilar desde sus
    respectivos módulos.
5.  **Mantener compatibilidad por versión.** No introducir una clase o
    método de una versión en otra sin verificar su disponibilidad.
6.  **Persistencia explícita.** Todo dato que deba sobrevivir a
    reinicios o descarga de chunks debe guardarse mediante el sistema
    apropiado.
7.  **Identificadores estables.** Las colonias y entidades deben
    utilizar identificadores persistentes, no depender de referencias
    temporales a objetos cargados.
8.  **Evitar lógica pesada por tick.** Preferir eventos, tareas
    programadas por ticks y actualizaciones con frecuencia controlada.
9.  **Probar ambos loaders.** Una funcionalidad no se considera
    terminada si solo compila en Fabric o solo en NeoForge.
10. **Documentar excepciones.** Toda diferencia importante entre
    versiones debe quedar registrada.

------------------------------------------------------------------------

# 16. Pruebas y criterios de aceptación

Cada funcionalidad debe probarse en las combinaciones que se pretenden
distribuir.

Matriz objetivo:

  Minecraft   Fabric      NeoForge
  ----------- ----------- -----------
  26.3        Pendiente   Pendiente
  26.2        Pendiente   Pendiente
  1.21.1      Pendiente   Pendiente
  1.20.1      Pendiente   Pendiente

### Pruebas mínimas del sistema de colonias

-   La colonia conserva su identificador después de guardar y cargar el
    mundo.
-   Las entidades pertenecen a la colonia correcta.
-   Dos colonias cercanas mantienen memorias independientes.
-   La reina recibe información únicamente cuando se transmite.
-   La memoria por jugador se conserva correctamente.
-   Las adaptaciones se aplican y producen cambios observables.
-   La descarga de chunks no elimina ni duplica colonias.
-   La muerte de la reina inicia el colapso.
-   Una colonia en colapso no vuelve a operar.
-   Los integrantes cargados posteriormente no reactivan la colonia.
-   Los asentamientos secundarios quedan inactivos.
-   Los huevos no producen una nueva reina tras el exterminio.
-   Las colmenas abandonadas permanecen como estructuras sin actividad.
-   La población y expansión respetan los límites configurados.

### Pruebas de rendimiento

-   Medir el coste de IA con varias colonias activas.
-   Medir el coste de navegación en colmenas grandes.
-   Comprobar que la memoria no se actualiza innecesariamente cada tick.
-   Comprobar el comportamiento con chunks descargados.
-   Limitar población, tareas y exploración.
-   Verificar que la generación natural no cause picos excesivos de
    carga.

------------------------------------------------------------------------

# 17. Orden de prioridades

Para evitar que el proyecto se vuelva demasiado complejo desde el
principio, se recomienda este orden:

1.  Definir y crear la estructura del workspace, repositorios y
    branches.
2.  Configurar una versión y un loader como entorno inicial de
    desarrollo.
3.  Crear el núcleo común y sus interfaces.
4.  Implementar una colonia mínima con reina y una casta.
5.  Implementar persistencia e identificadores.
6.  Implementar memoria y niveles de amenaza.
7.  Implementar exterminio irreversible.
8.  Añadir las demás castas y sus comportamientos.
9.  Añadir caza, recursos y reproducción.
10. Añadir generación de colmenas y expansión.
11. Portar y verificar las funcionalidades en el segundo loader.
12. Portar las funcionalidades a las demás versiones.
13. Optimizar, balancear y preparar distribución.

No es necesario desarrollar simultáneamente las ocho combinaciones de
versión y loader. Es preferible validar primero la arquitectura en una
combinación, mantener el núcleo independiente y después integrar
progresivamente las demás.

------------------------------------------------------------------------

# 18. Decisiones pendientes

Los siguientes puntos todavía necesitan especificación antes de
considerarse requisitos cerrados:

-   Nombre definitivo de la especie y del mod.
-   Diseño anatómico final y número de extremidades.
-   Modelos, texturas, animaciones y sonidos.
-   Atributos exactos de cada casta.
-   Lista definitiva de presas.
-   Bloques y materiales de construcción.
-   Reglas exactas de generación y frecuencia de colmenas.
-   Límites de población y expansión.
-   Fórmulas para niveles de amenaza y aprendizaje.
-   Número y condiciones de desbloqueo de adaptaciones.
-   Comportamiento exacto de las criaturas durante el colapso.
-   Configuraciones disponibles para el usuario.
-   Dependencias externas permitidas.
-   Versiones exactas de loaders, Java, Gradle y mappings para cada
    branch.

Estas decisiones deben documentarse cuando se definan. No deben asumirse
como implementadas solo por aparecer como propuestas en este documento.

------------------------------------------------------------------------

## 19. Principio rector del proyecto

La prioridad es construir un sistema coherente antes de acumular
contenido.

La reina, la memoria, la independencia de las colonias y el exterminio
deben funcionar de manera fiable antes de desarrollar una expansión
compleja, una gran variedad de cámaras o muchas criaturas adicionales.

La arquitectura multiloader y multiversión debe permitir compartir la
mayor cantidad posible de lógica, pero sin sacrificar estabilidad ni
compatibilidad con las API particulares de cada versión de Minecraft.
