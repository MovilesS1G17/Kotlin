# Sprint 4 · Features nuevas, micro-optimizaciones y cierre de la app

> **Borrador de trabajo.** La política del curso no permite contenido generado por IA en la entrega final: cada integrante debe reescribir con sus propias palabras las secciones que le tocan, poner sus capturas del profiler y revisar las cifras con datos reales antes de publicar en la wiki.

Centralia guarda shorts de TikTok, Instagram y YouTube en una sola biblioteca con carpetas, tags y notas, y ayuda a volver a encontrarlos. En el Sprint 4 cada integrante agrega una feature con su propia vista, las dos apps las tienen todas (apps espejo) y se optimiza el rendimiento con evidencia de profiling.

Equipo (grupo 17): **Android** (Kotlin / Compose): Enrique, Alex · **iOS** (SwiftUI): David, Alejandro, Gustavo · **Backend** (FastAPI + PostgreSQL): compartido.

## 1. Tabla por integrante (Sprint 4)

| Integrante | Feature y vista nueva | BQ nueva | Tipo | Estrategia principal de la feature |
|---|---|---|---|---|
| Enrique | **Rediscover**: shorts guardados hace más de 7 días que no se abren hace 14 | BQ11 | 3 | Multithreading: lecturas en paralelo en IO + puntaje en paralelo en Default / task group |
| Alex | **Watch later**: cola ordenada de shorts para ver después (botón en el detalle) | BQ12 | 3 | Almacenamiento local relacional: tabla Room `watch_later` / Core Data `WatchLater.sqlite` |
| David | **Trends**: categorías y tags más guardados por la comunidad (solo lo que guardaron 3+ personas) | BQ13 | 1 | Caché en dos niveles: LRU en memoria + JSON en disco, con TTL de 10 min |
| Alejandro | **Creators**: shorts agrupados por creador, el más guardado primero | BQ14 | 3 | Multithreading (agrupar por plataforma en paralelo) + caché LRU por huella de la biblioteca |
| Gustavo | **Insights**: guardados por semana (8 semanas), plataformas, % en carpeta, top tags, racha | BQ15 | 3 | Almacenamiento local en archivo: último snapshot en JSON ("desde tu última visita") |

Las cinco vistas se abren desde la fila **Discover** de la biblioteca (Watch later · Rediscover · Creators · Trends · Insights), en las dos apps.

Código: Android desde `app/src/main/java/com/centralia/app/` (`domain/{rediscover,watchlater,trends,creators,insights,analytics}`, `feature/{discover,rediscover,watchlater,trends,creators,insights}`), iOS desde `Centralia/` (`Domain/Discover/`, `Features/{Discover,Rediscover,WatchLater,Trends,Creators,Insights}`, `App/Sprint4Services.swift`), backend `app/routers/trends.py` y `app/analytics/business.py`.

## 2. Cambios por la retroalimentación del Sprint 3

| Comentario del monitor | Cambio |
|---|---|
| El dashboard de las BQ debe ser parametrizable | Panel "Business question parameters": fechas, plataforma de origen, metas de búsqueda y de carga, ventana de reapertura, regla de IA y (nuevo) ventana de Watch later. Se guardan en la URL |
| El mensaje de un link inadmisible debe ser más diciente | El backend manda un título y una explicación por caso (falta https, perfil de TikTok, post de Instagram, video normal de YouTube, sitio no soportado); las dos apps los muestran |
| Se esperaría que se guarde lo que se acaba de hacer | Done/Save del editor de tags agrega también el tag que se estaba escribiendo |
| Tags muy largos se salen de la pantalla | Límite de 30 caracteres con contador, el backend rechaza más largos (`tag_too_long`) y los chips muestran una línea con "…" |

## 3. Features (5.a)

| Sprint | Feature | Android | iOS |
|---|---|---|---|
| 2 | Cuentas con verificación por código, login, reset | ✓ | ✓ |
| 2 | Guardar un short: detección de plataforma, metadatos, tags y carpeta sugeridos | ✓ | ✓ |
| 2 | Biblioteca, búsqueda con filtros, carpetas, detalle, nota, tags | ✓ | ✓ |
| 2 | Reproducción nativa (MP4 del backend) con respaldo web | ✓ | ✓ |
| 2 | Context-aware: ahorro de datos con datos móviles | ✓ | ✓ |
| 2 | Sensor: sacudir para deshacer un borrado | ✓ | ✓ |
| 2 | Smart folder: tarjeta "Smart suggestion" | ✓ | ✓ |
| 3 | Modo offline: biblioteca local, cola de cambios, banner, sincronización | ✓ | ✓ |
| 3 | Recently viewed (base de datos local) | ✓ | ✓ |
| 3 | Remember my email | ✓ (DataStore) | ✓ (Keychain) |
| 3 | Dashboard de BQ parametrizable, mensajes de link claros, límite de tags | ✓ | ✓ |
| **4** | **Rediscover** (Enrique) | ✓ | ✓ |
| **4** | **Watch later** (Alex) | ✓ | ✓ |
| **4** | **Trends** (David) | ✓ | ✓ |
| **4** | **Creators** (Alejandro) | ✓ | ✓ |
| **4** | **Insights** (Gustavo) | ✓ | ✓ |
| **4** | Micro-optimizaciones (búsqueda, biblioteca, fechas, tamaño del APK) | ✓ | ✓ |

## 4. Business questions del Sprint 4 (5.b)

> **Pendiente:** confirmar el número de cada BQ en la lista del Sprint 1 (columna "Sprint 1") y, si alguna no está en la lista, reemplazarla por la más parecida respetando el máximo por tipo.

| BQ | Sprint 1 | Integrante | Tipo | Pregunta | Por qué importa | Eventos |
|---|---|---|---|---|---|---|
| 11 | #? | Enrique | 3 | ¿Rediscover hace que se vuelvan a abrir shorts olvidados, y qué tan viejos son? | La propuesta de valor es poder volver a lo guardado; mide si la vista lo logra | `rediscover_viewed {shown}`, `rediscover_opened {video_id, days_since_save}` |
| 12 | #? | Alex | 3 | ¿Los usuarios ven lo que ponen en Watch later, y cuánto esperan? | Dice si la cola sirve o se vuelve otro lugar donde se acumulan cosas | `watch_later_added`, `watch_later_opened {hours_in_queue}` (parámetro: ventana de días) |
| 13 | #? | David | 1 | ¿Cuánto tarda Trends en mostrar sus datos y cuánto ahorra la caché frente a la red? | Rendimiento de la vista y justificación numérica de la caché | `trends_loaded {source: memory/disk/network, duration_ms, stale}` |
| 14 | #? | Alejandro | 3 | ¿Los usuarios siguen guardando shorts de los mismos creadores? | Justifica la vista Creators (y, a futuro, seguir creadores) | `video_saved.creator` (lo registra el servidor), `creators_viewed` |
| 15 | #? | Gustavo | 3 | ¿Quienes miran Insights archivan en carpetas una mayor parte de lo que guardan después? | Dice si mostrarle sus hábitos al usuario lo lleva a organizar | `insights_viewed`, `video_saved.folder` |

Se responden en `app/analytics/business.py` (`bq_rediscover`, `bq_watch_later`, `bq_trends_load`, `bq_repeat_creators`, `bq_insights_organize`), en `GET /v1/analytics/business-questions` y en la fila "Sprint 4" del dashboard. Las apps mandan los eventos con un buffer que los envía en lotes y los guarda mientras no hay red.

Resultados con datos **simulados** (40 usuarios, 21 días; reemplazar por datos reales): BQ11 13.8 % de las sugerencias se abren (12 shorts rescatados, mediana 10.5 días desde que se guardaron) · BQ12 73.9 % de la cola se ve en 7 días (mediana 46.7 h) · BQ13 383 ms en promedio; 42.6 % desde caché (memoria 4 ms, disco 21 ms, red 662 ms) · BQ14 40.2 % de los guardados son de un creador ya guardado; el top 3 de creadores es el 73.2 % de lo que guarda cada usuario · BQ15 +10.7 puntos de archivado en carpeta después de ver Insights (72.5 % vs 61.8 %).

## 5. Estrategias del Sprint 4 (5.c – 5.f)

### Multithreading / concurrencia (5.e)

| Dónde | Android | iOS | Por qué |
|---|---|---|---|
| Rediscover | `viewModelScope` (Main) → `withContext(Dispatchers.IO)` con 3 `async` (biblioteca, Room, SharedPreferences) → `Rediscover.pickInParallel`: un `async(Dispatchers.Default)` por bloque de candidatos → estado en Main | `async let` (biblioteca, Core Data) → `Task.detached` + `withTaskGroup` con una tarea por bloque → estado en el main actor | Puntuar toda la biblioteca es trabajo de CPU; hacerlo en el hilo principal congela la vista con bibliotecas grandes |
| Creators | Biblioteca en IO, `Creators.groupInParallel`: un `async(Default)` por plataforma, se mezclan los grupos | `Task.detached` + `withTaskGroup` por plataforma | Agrupar y ordenar es CPU; cada plataforma es independiente |
| Insights | Biblioteca en IO, cálculo en `Dispatchers.Default`, archivo en IO | Cálculo en `Task.detached`, archivo con `await` | No bloquear la UI mientras se calcula |
| Eventos de BQ | `BatchingAnalyticsTracker`: `track` solo agrega a un buffer con `Mutex` y vuelve; el envío corre en el scope de IO | `BatchingAnalyticsTracker` con `NSLock` y `Task` | Medir no debe hacer esperar a la UI |

### Almacenamiento local (5.d)

| Qué | Android | iOS | Por qué |
|---|---|---|---|
| Cola Watch later | Room: tabla `watch_later` (posición indexada, `@Transaction` para agregar y reordenar, `Flow` que la pantalla observa). Migración 1→2 que conserva Recently viewed | Core Data: `WatchLater.sqlite` con su modelo en código, contexto en segundo plano, restricción de unicidad | Tiene orden que el usuario cambia y consultas puntuales ("¿ya está en la cola?"): relacional |
| "Not now" de Rediscover | `SharedPreferences` (id → fecha) | `UserDefaults` | Pocos pares clave-valor |
| Snapshot de Insights | Archivo JSON en `filesDir/insights/`, escritura atómica | JSON en Application Support, atómico y protegido | Un documento que se lee y escribe completo |
| Copia de Trends | JSON en `cacheDir/trends/` | JSON en `Caches/trends/` | Es caché: el sistema la puede borrar sin problema |

Al cerrar sesión se borra todo lo anterior en las dos apps.

### Caché (5.f)

| Qué | Android | iOS | Política |
|---|---|---|---|
| Trends | `LruCache` (4 entradas, una por ventana 7/30 días) + archivo | `LRUCache` propio (del Sprint 3) dentro de un `actor` + archivo | TTL 10 min; memoria → disco → red; con error de red se muestra la copia más nueva marcada "Updated X ago" |
| Creators | `CreatorsCache` (LRU de 4) por huella de la biblioteca | `CreatorsCache` (LRU con `NSLock`) | Volver a la vista sin cambios en la biblioteca no reagrupa |
| Búsqueda | Índice de búsqueda por biblioteca (micro-optimización) | Índice + LRU de resultados del Sprint 3 | Ver sección 6 |

### Conectividad eventual de las features nuevas (5.c)

| Feature | Estrategia | Sin conexión |
|---|---|---|
| Rediscover | Cache falling back to network (usa la copia local de la biblioteca) | Funciona igual; avisa "You're offline. These picks come from the copy of your library on this phone." |
| Watch later | Local only (Room / Core Data) | Funciona igual: agregar, ordenar, quitar |
| Trends | Network falling back to cache, con TTL | Muestra la última copia con "Updated 3 h ago · you're offline"; si nunca se cargó: "Trends needs a connection the first time…" |
| Creators | Cache falling back to network + LRU | Funciona con la copia local |
| Insights | Local first: muestra el último snapshot y recalcula | Funciona con la copia local; si no puede leer la biblioteca muestra el último snapshot |
| Eventos de BQ | Buffer en memoria (hasta 200), reenvío al volver la red | No se pierden mientras la app siga abierta |

Escenarios para probar en el teléfono: (1) modo avión → abrir las 5 vistas; (2) agregar 3 shorts a Watch later offline, reordenar, cerrar y abrir la app; (3) abrir Trends con red, activar modo avión, volver a abrir: sale de la copia; (4) cerrar sesión y entrar con otra cuenta: Watch later, Insights y Trends vacíos.

## 6. Puntos de falla de rendimiento (PPoF) y micro-optimizaciones (punto 3)

Seguimos la guía de la clase *Performance bugs*: (1) listar los puntos de falla de rendimiento de la app, (2) ubicarlos en el código, (3) decidir si van en un hilo de trabajo o en una función asíncrona y (4) elegir la estrategia (caché, micro-optimización, manejo de memoria, multithreading).

### 6.1 Puntos de falla (PPoF)

| PPoF (tipo según la clase) | Dónde (Android / iOS) | ¿Hilo o async? | Estrategia |
|---|---|---|---|
| Peticiones HTTP | `ApiClient.perform` / `APIClient.send` | Sí: `Dispatchers.IO` / `async` (URLSession) | Multithreading + caché (Trends, reproducción) + offline-first |
| Acceso a I/O local | Room, JSON de la biblioteca y la cola, snapshot de Insights / Core Data, archivos, Keychain | Sí: `Dispatchers.IO` / contexto de Core Data en segundo plano, `actor` | Multithreading + escritura atómica |
| Tareas largas locales | Búsqueda en cada tecla; puntaje de Rediscover; agrupar Creators; Insights | Sí: `Dispatchers.Default` / `Task.detached` + task group | Multithreading + **micro-optimización M1/M4** (índice de búsqueda) + caché LRU (Creators) |
| Pintar listas con scroll | Feed de la biblioteca, conteos por carpeta | No (hilo principal), por eso debe ser barato | **Micro-optimización M3/M6**: no recalcular por redibujo, `contentType` para reutilizar filas |
| Decodificar respuestas (fechas) | — / decodificador de `APIClient` | Corre al recibir datos | **Micro-optimización M5**: formateadores compartidos (la clase lista "costly date-formatting" como bug de rendimiento) |
| Bitmaps (portadas) | Coil / `CoverImageLoader` | Sí: Coil y GCD `global(qos: .userInitiated)` | Caché en memoria + disco, prefetch en paralelo |
| Tamaño de la app y recursos sin usar | APK release | — | **Micro-optimización M7**: R8 + `shrinkResources` (ítems 10–11 de la clase) |

### 6.2 Micro-optimizaciones implementadas

| # | App | Ítem de la clase | Qué se cambió | Evidencia (herramienta de la clase) |
|---|---|---|---|---|
| M1 | Android | 1. Evitar crear objetos innecesarios (en ciclos) · 4. ciclos indexados | `SearchIndex`: el texto de cada short se normaliza una vez por biblioteca, no en cada tecla; términos normalizados una vez; regex precompilada; camino ASCII en `TextMatch.fold` | **Memory Profiler → Record allocations** mientras se escribe "pasta bea" (menos objetos `String`/`ArrayList` y menos GC) + **CPU Profiler → System Trace**, sección `search_filter` |
| M3 | Android | 1. Objetos innecesarios · listas con scroll | Biblioteca: `filteredVideos` se calcula una vez por estado (`by lazy`); filas del feed con `contentType` | **CPU Profiler** al hacer scroll + Layout Inspector (conteo de recomposiciones) |
| M7 | Android | 10–11. Reducir tamaño del APK y quitar recursos sin usar | Release con `isMinifyEnabled = true` y `isShrinkResources = true` (R8) | **Build ▸ Analyze APK** antes y después (tamaño total, `classes.dex`, `res/`) + tiempo de arranque (Logcat, línea `Displayed …`) |
| M4 | iOS | 1. Objetos innecesarios | `VideoSearchIndex`: `folding` una vez por biblioteca en vez de `localizedStandardContains` sobre 7 campos por término y tecla | **Instruments → Time Profiler** (tiempo de `VideoSearch.filter`) + **Allocations** (marcar generación antes y después de escribir) |
| M5 | iOS | Costly date-formatting (taxonomía de bugs iOS) | Dos `ISO8601DateFormatter` compartidos en vez de uno por fecha | **Time Profiler** al cargar la biblioteca + **Allocations** (instancias de `ISO8601DateFormatter`) |
| M6 | iOS | Cálculo innecesario al pintar | Conteo por carpeta en un recorrido (antes carpetas × shorts por redibujo) | **Instruments → SwiftUI** (invocaciones de `body` y tiempo por frame) + Time Profiler |

> **Pendiente del equipo (ítems 8 y 9 de la clase):** activar *Developer options ▸ Debug GPU overdraw* en el teléfono y revisar la biblioteca y las vistas nuevas (zonas rojas = el mismo píxel se pinta 4+ veces), y correr **Lint** (*Code ▸ Inspect Code* en Android Studio). Poner las capturas y lo que se corrigió.

Una idea que se probó y se descartó: cambiar `Long?` por `Long` primitivo y evitar `sqrt` en el detector de sacudida (ítem 3 de la clase). El benchmark no mostró diferencia medible (≈9.5 ns por lectura antes y después), así que se dejó el código original: una micro-optimización solo se justifica con una medición.

### 6.3 Cifras de referencia y cómo medir en el teléfono

Las cifras siguientes se midieron en la JVM de escritorio (`MicroOptimizationBenchmark`); **son indicativas**. Como se vio en *Compilation & execution*, ART compila distinto (intérprete + JIT + AOT guiado por el perfil de uso) y el teléfono tiene otro hardware, así que la evidencia para la wiki son las capturas del profiler en un dispositivo real.

| Caso (JVM, mediana de 15 rondas) | Antes | Después | Mejora |
|---|---|---|---|
| Búsqueda, 1 tecla, 200 shorts | 322 µs | 13 µs | ≈25× |
| Búsqueda, 1 tecla, 1000 shorts | 1157 µs | 56 µs | ≈21× |
| Filtro por 2 tags, 1000 shorts | 390 µs | 40 µs | ≈10× |
| Construir el índice (una vez por cambio de biblioteca) | — | 868 µs | — |

**Android (Android Studio Profiler, clase *Profiling Android*):**
1. Instalar el build **release** (ya es `profileable`) en un teléfono real; `View ▸ Tool Windows ▸ Profiler`, seleccionar el proceso.
2. **CPU:** *System Trace*, escribir "pasta bea" en Search, detener. En el hilo `DefaultDispatcher` buscar `search_filter` y anotar la duración. Verde = corriendo, amarillo = esperando I/O, gris = dormido: el hilo `main` no debe ponerse verde largo mientras se escribe.
3. **Memory:** *Record allocations* durante la misma búsqueda; comparar el número de asignaciones y los eventos de GC (patrón de "sierra").
4. **Network** (Trends con caché vs sin caché) y **Energy** como apoyo.
5. **APK:** *Build ▸ Analyze APK* del release antes (commit anterior) y después (M7).
6. Hacer lo mismo en el commit anterior al Sprint 4 (antes) y en el actual (después) y poner las dos capturas lado a lado.

**iOS (Xcode Instruments, clase *Profiling iOS*):**
1. `Product ▸ Profile` en un iPhone real.
2. **Time Profiler:** escribir en Search; con "Hide system libraries" y "Invert call tree", anotar el tiempo de `VideoSearch.filter` / `VideoSearchIndex`.
3. **Allocations:** *Mark Generation* antes y después de escribir; comparar memoria y objetos creados.
4. **SwiftUI:** invocaciones de `body` de `LibraryView` al abrir la biblioteca.
5. Comparar con el commit anterior. Las pruebas `MicroOptimizationPerformanceTests` (XCTest `measure`) sirven como apoyo, no reemplazan Instruments.

## 7. Propuesta de valor (punto 2)

> Escribir con sus palabras. Puntos de apoyo con lo implementado y los datos:

- **Un solo lugar para lo guardado en TikTok, Instagram y YouTube**: guardar por link con plataforma, metadatos, tags y carpeta sugeridos (BQ2, BQ4); 5.5 guardados por usuario por semana (BQ5, simulado).
- **Volver a encontrarlo**: búsqueda rápida (BQ1, BQ9), organización (BQ8: lo organizado se reabre más), y ahora Rediscover, Watch later y Creators (BQ11, BQ12, BQ14).
- **Funciona sin conexión**: modo offline del Sprint 3 y todas las vistas nuevas.
- **Modelo de ingresos** (del Sprint 1): conectar con BQ10 (auto-organización con IA como feature premium) y BQ15 (Insights como feature que motiva a organizar). *Completar con el modelo que definieron en el Sprint 1 y comparar la propuesta original con lo que quedó implementado.*

## 8. Entrega en dispositivos reales (puntos 6 y 7)

- **Android:** invitación de Firebase App Distribution a los 7 monitores con el nombre `isis3510-17-android-Sprint4`, con una descripción que incluya la URL del backend (túnel), un usuario de prueba y que se puede crear una cuenta nueva (el código llega al correo; revisar spam). Mensaje en el canal general de Teams justo después: "As group 17, we have sent an apk named of [nombre].apk. We work in kotlin for Android".
- **iOS:** mensaje al monitor por Teams para coordinar la prueba en el teléfono: "As group 17, we are ready to have our apps tested by the TAs. We work in swift for iOS".
- Probar antes en un teléfono real: cada bug que encuentren resta 0.5.

## 9. Repositorio (puntos 11 y 13)

Un milestone "Sprint 4", un issue por commit, una rama y un pull request por integrante revisado por un compañero y unido con **Squash and merge**, mensajes de commit detallados, tablero Kanban actualizado. Ver el plan de commits.
