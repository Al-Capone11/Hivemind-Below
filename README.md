# Hivemind

Mod de colonias subterráneas para Minecraft. Instala el mismo JAR en el cliente y en el servidor dedicado cuando juegues en multijugador.

Esta rama corresponde a **Minecraft 1.20.1**. Los cargadores son Fabric con Fabric API y Forge mediante LegacyForge 47.1.106. El JDK de compilación es 21. Los dos JAR requieren Java 17 como mínimo. La carpeta y el sufijo del JAR de Forge se llaman `neoforge`.

## Uso y colmenas

`/hivemind nest` crea una colmena al norte de la posición del jugador. Requiere permisos de operador y terreno cargado, despejado de entidades y bloques protegidos. El comando modifica el terreno. Las colmenas nuevas tienen galerías curvas y cavidades de distintas alturas. La entrada pasa por la zona de guardia. El almacén y el descanso quedan en ramales laterales. La reina, el núcleo y el panal comparten una cámara protegida al fondo.

La **cápsula de almacenamiento** es un bloque propio con 27 espacios. Está en la pestaña creativa de Hivemind y se fabrica con ocho bloques de quitina alrededor de la casilla central vacía. En las colmenas guardadas, el barril de la sala de almacén se convierte en una cápsula y conserva su contenido. Los obreros pueden recoger carne suelta y quitina. Tras la caza, los guerreros intentan recoger la carne y llevarla a la cápsula. En un nido compacto, la entrega incrementa la reserva interna. Los obreros pueden retirar quitina para reparar las cámaras. La colonia puede convertir alimento sobrante en quitina. Si la cápsula se rompe o se descarga su chunk, los obreros conservan lo que transportan hasta que haya un almacén accesible.

La reina se acerca al panal para poner y cuidar el huevo cuando hay alimento y plazas libres. Si se aleja o entra en combate durante el cuidado, se retrasa la incubación. Para eclosionar, la cría necesita que la reina esté cerca y que haya espacio. Los obreros se dedican a sus otras tareas. Los miembros heridos pueden dirigirse a los lechos de muda y los guerreros vigilan la entrada de día. Los exploradores reconocen el entorno y regresan con informes. Las criaturas creadas con huevos de aparición quedan sin colonia.

El límite de reproducción es de siete integrantes en una colmena compacta y de doce en una con cámaras especializadas. La ampliación completa aporta tres plazas. Cada asentamiento aporta otras tres cuando el refugio está completo y el paso está abierto, con suelo firme y la puerta despejada. Las colonias que superan el límite conservan sus integrantes y suspenden la puesta de huevos hasta recuperar espacio. `/hivemind status` muestra la población y su límite.

Una colonia con cámaras, al menos diez integrantes, tres obreros y suficiente alimento puede planificar su primer asentamiento. Puede iniciar el segundo a partir de quince integrantes y el tercero a partir de dieciocho, una vez que los anteriores sean utilizables. Los obreros excavan el acceso y construyen cada refugio por partes. La excavación requiere `mobGriefing` y consume alimento. El plano admite bloques comunes de terreno y piezas de quitina previstas para retirada. Las menas, los fluidos, los contenedores y los chunks descargados pueden bloquear la obra. El terreno común colocado por jugadores es indistinguible del natural. Los refugios comparten la colonia, su memoria y su reina.

Las colmenas con cámaras especializadas también pueden ampliar su salida de defensa hacia Z positivo con un corredor que empieza al final de la sala defensiva y conecta con una nueva sala con salida exterior. Requiere al menos tres obreros, alimento y quitina en el almacén. Los obreros conservan el suelo firme y pueden tender piso donde falte. Hasta dos obreros pueden excavar gradualmente bloques comunes de terreno dentro del volumen previsto si `mobGriefing` está activado; cada bloque excavado consume alimento y no genera un objeto. La excavación se detiene ante bloques no admitidos, menas, contenedores o fluidos. El terreno común colocado por jugadores es indistinguible del natural y puede ser retirado. Los materiales y el terreno retirados permanecen en el mundo entre sesiones. Una ampliación completada con la beta19 puede tener la salida exterior cerrada: hay que abrir manualmente la parte central de la pared del fondo.

`/hivemind status` muestra un informe por colonia con las coordenadas del núcleo, población y cría, alimento y caza, cámaras, asentamientos y memoria. Cada refugio indica la fase de obra y las coordenadas del bloque central inferior de la puerta que conecta con la colmena. Si se coloca obsidiana en esa casilla, el refugio figura como bloqueado y sus tres plazas dejan de contar hasta retirar el bloque. Si aún no hay refugio, el informe conserva el primer motivo de rechazo de la última búsqueda. Los obreros pueden retirar hierba, helechos, arbustos bajos y flores pequeñas del plano si el permiso de excavación está habilitado.

## Memoria y amenazas

Las colmenas activas impiden la aparición natural de criaturas ajenas en sus salas y pasillos, y en los asentamientos terminados. Los combatientes buscan hostiles que entran, y la reina y los obreros los enfrentan si los encuentran dentro. Los exploradores mantienen su función de reconocimiento. Los obreros dejan en el suelo el botín de criaturas hostiles, y la carne podrida deja de contar como alimento.

Los integrantes transmiten noticias si presencian bajas o daños a la colmena y alcanzan la zona central. Los exploradores pueden informar de jugadores dentro del núcleo y de jugadores con un objeto de ignición cerca de fuego. Esto último es una observación aproximada: no demuestra quién encendió el fuego. Los informes de fuego y de intrusión tienen tiempos de espera independientes. Los informes se pierden si el mensajero no llega.

Los avistamientos por sí solos dejan al jugador en el nivel detectado. Las bajas, los daños, las incursiones y los informes de fuego pueden elevar la amenaza. Los defensores priorizan amenazas, pueden aproximarse desde distintos ángulos y buscan posiciones laterales frente a tiradores conocidos. De noche patrullan un radio mayor. Cuando escasea el alimento buscan animales adultos sin nombre hasta a 48 bloques del guerrero, dentro del territorio de su colonia. Si detectan una amenaza hostil reciente fuera de la zona del núcleo, el grupo sale cuando se han reunido al menos dos combatientes. La expedición termina al amanecer, al perder al objetivo o al cumplir su plazo. `/hivemind status` indica si el grupo se está reuniendo, salió o está de retirada.

## Generación y configuración

La generación de chunks del Overworld construye nidos en cuevas con suelo firme y una salida transitable. Según el sitio genera un nido compacto con cámara real y cría o una colmena con cámaras especializadas. Al entrar el área en simulación, el marcador del núcleo registra la colonia y aparecen la reina y sus integrantes si la población y la separación entre colonias lo permiten. Un mundo existente puede generar esos nidos al explorar chunks nuevos.

Cada mundo crea `data/hivemind.properties`. Los cambios se aplican al volver a cargar el mundo.

| Ajuste | Valor inicial | Efecto |
| --- | --- | --- |
| `naturalColoniesPerDimension` | 4 | Límite de colonias activas que permite nuevas fundaciones naturales. Cero desactiva las fundaciones. |
| `colonySpacing` | 128 | Distancia horizontal mínima entre un nido natural y otra colmena, en bloques. |
| `hunting` | true | Activa la caza organizada de las colonias. |
| `upkeepPerFourMembers` | 1 | Alimento diario por cada grupo de hasta cuatro integrantes. Cero desactiva el mantenimiento. |
| `expansion` | true | Permite nuevas ampliaciones y asentamientos. Los refugios registrados siguen admitiendo mantenimiento. |

## Datos y limitaciones

Los datos se guardan en formato 11 y admiten colonias de los formatos 1 a 10. Al actualizar se conservan la distribución de las colmenas existentes, los asentamientos, la cría y la memoria de la colonia. Conserva una copia anterior del mundo si necesitas probar una beta que solo admite los formatos antiguos.

Los asentamientos usan una planta de 7 × 7. Los obreros pueden ampliar los de 5 × 5 guardados en las betas anteriores, reutilizando el suelo de quitina y retirando la antigua pared y el techo cuando quedan en el interior del nuevo plano. Para cada nuevo refugio se examinan ubicaciones entre 24 y 40 bloques al este, oeste o norte. Se prueban primero los pasillos rectos y después los trazados con desplazamiento lateral. La ampliación de defensa utiliza la salida sur. Si una partida antigua ya tiene un refugio al sur, la colonia conserva ese proyecto.

La ampliación de defensa conecta con la galería sur. En una ampliación anterior, los obreros pueden retirar el techo bajo dentro del espacio interior y añadir el nuevo. En los accesos al este, oeste y norte, los obreros añaden paredes y cubierta en el espacio libre una vez terminado el asentamiento. El terreno sólido que ya bordea un acceso subterráneo puede servir de pared.

Las tareas y el mantenimiento se ejecutan cuando la colonia está cargada. El comportamiento, el equilibrio y la compatibilidad de las ocho variantes se comprueban dentro de Minecraft durante la depuración.

## Compilación

Ejecuta `./build-version.ps1 -Version 0.1.0` desde la raíz de la rama. El script comprueba `Core/` y compila ambos loaders con el wrapper incluido. Para indicar otro número de compilación, pasa la versión asignada con `-Version`. Los JAR quedan en `fabric/build/libs/` y `neoforge/build/libs/`. Los archivos `-sources.jar` contienen el código fuente.
