# PetCare

Aplicacion de consola Kotlin/JVM para gestionar un turno de diez boxes veterinarios.

## Ejecutar en Windows

Requiere JDK 25 (toolchain del proyecto original). Gradle Wrapper descarga las dependencias en la primera ejecucion.

```powershell
.\gradlew.bat build
.\gradlew.bat run
.\gradlew.bat test
```

En IntelliJ IDEA, abrir el proyecto como Gradle, sincronizar y ejecutar `Main.kt`.
Los textos usan ASCII simple para evitar problemas de encoding en Windows.
La simulacion ejecuta los cinco casos obligatorios y los errores de codigo invalido y paciente inexistente.
Las esperas reales suman aproximadamente 47,5 segundos. Las pruebas usan `runTest` con tiempo virtual,
conservando las mismas funciones de produccion y sus `delay(3000)` y `delay(6500)`.

## Archivos y pauta

Todos los archivos de produccion estan en `src/main/kotlin`, con paquete `org.example`.

| Archivo | Responsabilidad y requisitos |
| --- | --- |
| `Main.kt` | Punto de entrada; `runBlocking`, simulacion, errores y cierre. |
| `DatosPrueba.kt` | Los cinco pacientes exigidos y sus tiempos de atencion. |
| `Paciente.kt` | `open class`, datos inmutables, Regex, validacion, calculo proporcional, IVA y descuento municipal. |
| `Canino.kt` | Herencia, `override`, tarifa de $12.000/h y descuento de convenio del 20 %. |
| `Felino.kt` | Herencia, `override`, tarifa de $9.000/h y gratuidad bajo 20 minutos. |
| `Exotico.kt` | Herencia, `override`, tarifa de $20.000/h, `Boolean` silvestre y recargo del 30 %. |
| `TipoDueno.kt` | Enum PARTICULAR, CONVENIO y MUNICIPAL. |
| `EstadoBox.kt` | `sealed class` con los cuatro estados exactos y sus datos. |
| `Box.kt` | Numero, estado mutable y descripcion mediante `when` exhaustivo. |
| `Ticket.kt` | Numero de ticket, paciente, tipo, codigo, minutos y pago. |
| `PetCare.kt` | Capacidad, registro y salida con `suspend`, estados, historial, ingresos, consultas y `try-catch`. |
| `Reporte.kt` | Tickets, detalles, ingresos por tipo, resumen del turno y cinco consultas de negocio. |
| `src/test/kotlin/PetCareTest.kt` | Nueve pruebas con escenarios de tarifas, estados, tiempos, errores, capacidad y recuperacion. |
| `build.gradle.kts` | Kotlin 2.4.20, plugin application, coroutines 1.11.0, pruebas JUnit Platform y toolchain 25. |

La aplicacion utiliza `Int`, `Double`, `String`, `Boolean`, `val`, `var`, operadores aritmeticos,
`if/else`, `when`, funciones reutilizables, `List` y `MutableList`.

## Resultados esperados

| Paciente | Codigo | Minutos | Pago final |
| --- | --- | ---: | ---: |
| Max | CA12CD | 75 | $14.280 |
| Luna | CA99ZA | 180 | $42.840 |
| Misi | FE22TO | 18 | $0 |
| Loro | EX44RG | 120 | $30.940 |
| Iguana | EX77RG | 45 | $17.850 |

Total: **$105.910**. Promedio: **$21.182**. Cinco pacientes atendidos y diez boxes disponibles al cierre.
Caninos: $57.120; felinos: $0; exoticos: $48.790. Mayor ingreso: Canino.
Paciente con mayor tiempo: Luna (180 minutos). Paciente de convenio: Max.

## Decisiones de diseno

- Es una simulacion automatica de consola, con datos en memoria durante un turno.
- El tiempo se obtiene de ingreso y salida mediante `LocalDateTime` y `Duration`, en minutos completos.
  Se rechazan duraciones menores de un minuto, negativas o fuera del rango de `Int`.
  Las fechas de los casos son fijas para reproducir los tiempos de la pauta sin esperar horas.
- El cobro es proporcional a los minutos: tarifa por hora * minutos / 60.
  Se aplican las reglas de especie, despues IVA y finalmente descuento municipal.
  Se usa `Double` para evidenciar la rubrica; el formato monetario muestra dos decimales.
- La recaudacion total y por tipo se deriva del historial: agregar un ticket actualiza ambas consultas
  y evita mantener contadores que puedan quedar desincronizados.
- Los codigos se validan al registrar, aceptan letras ASCII mayusculas o minusculas y son unicos
  durante todo el turno, sin distinguir mayusculas.
- El sistema reserva el box y codigo antes de suspenderse. Esta disenado para el contexto de un hilo
  de `runBlocking`, incluyendo corrutinas que se intercalan en ese contexto.
- Si falla el calculo, se conserva al paciente en atencion, sin ticket ni ingreso nuevo.
  Si se cancela una corrutina, se restaura el estado previo y se propaga la cancelacion.
- En empates de tiempo o ingresos se devuelve el primer maximo encontrado.

## Conceptos para estudiar

- **Herencia:** Canino, Felino y Exotico reutilizan los datos y funciones de Paciente.
- **Polimorfismo:** una referencia de tipo Paciente ejecuta el `calcularCosto` sobrescrito por su especie.
- **Sealed class:** limita los estados posibles del box y permite que `when` revise todos ellos.
- **filter:** selecciona boxes libres, pacientes de convenio o tickets de una especie.
- **map:** transforma tickets en pacientes o codigos de atencion.
- **sumOf:** suma los pagos registrados.
- **maxByOrNull:** encuentra el paciente de mayor duracion o el tipo de mayor recaudacion; puede devolver null.
- **Corrutinas:** las funciones `suspend` esperan con `delay` sin bloquear el hilo; `runBlocking` mantiene
  el programa de consola activo hasta completar el turno.
- **try-catch:** captura errores previstos y muestra mensajes amigables para continuar las operaciones.

Referencia de pruebas con tiempo virtual: [documentacion oficial de kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/).
