package org.example

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PetCareTest {
    private val ingreso = LocalDateTime.of(2026, 9, 26, 9, 0)
    private fun canino(codigo: String = "CA12CD") =
        Canino(codigo, "Max", "Golden Retriever", ingreso, TipoDueno.CONVENIO)

    @Test
    fun turnoCompleto() = runTest {
        val sistema = PetCare()
        val casos = datosPrueba(ingreso)
        val esperados = listOf(14280.0, 42840.0, 0.0, 30940.0, 17850.0)
        casos.forEach { (paciente, _) -> assertTrue(sistema.registrarEntrada(paciente)) }
        assertEquals(5, sistema.boxesDisponibles())
        casos.forEachIndexed { indice, (paciente, minutos) ->
            val ticket = assertNotNull(sistema.registrarSalida(paciente.codigoAtencion, ingreso.plusMinutes(minutos.toLong())))
            assertEquals(indice + 1, ticket.numeroTicket)
            assertEquals(minutos, ticket.tiempoMinutos)
            assertEquals(esperados[indice], ticket.montoPagado, 0.000001)
        }
        assertEquals(105910.0, sistema.totalRecaudado, 0.000001)
        assertEquals(21182.0, sistema.ingresoPromedio(), 0.000001)
        assertEquals(57120.0, sistema.recaudacionPorTipo().getValue("Canino"), 0.000001)
        assertEquals(48790.0, sistema.recaudacionPorTipo().getValue("Exotico"), 0.000001)
        assertEquals(0.0, sistema.recaudacionPorTipo().getValue("Felino"))
        assertEquals(listOf("Max"), sistema.pacientesConvenio().map { it.nombre })
        assertEquals(casos.map { it.first.codigoAtencion }, sistema.codigosFinalizados())
        assertEquals("Luna", sistema.pacienteMayorTiempo()?.nombre)
        assertEquals("Canino", sistema.tipoConMayorIngreso())
        assertEquals(10, sistema.boxesDisponibles())
        assertEquals(47500L, testScheduler.currentTime)
    }

    @Test
    fun tarifasYDescuentos() {
        TipoDueno.entries.forEach { dueno ->
            val felino: Paciente = Felino("FE22TO", "Misi", "Siames", ingreso, dueno)
            assertEquals(0.0, felino.calcularMontoFinal(19))
            val esperado = if (dueno == TipoDueno.MUNICIPAL) 1785.0 else 3570.0
            assertEquals(esperado, felino.calcularMontoFinal(20), 0.000001)
        }
        val municipal: Paciente = Canino("CA11AA", "Perro", "Mestizo", ingreso, TipoDueno.MUNICIPAL)
        assertEquals(7140.0, municipal.calcularMontoFinal(60), 0.000001)
        assertTrue((datosPrueba(ingreso)[3].first as Exotico).detalle().contains("silvestre: si"))
        assertTrue((datosPrueba(ingreso)[4].first as Exotico).detalle().contains("silvestre: no"))
    }

    @Test
    fun erroresDeRegistro() = runTest {
        val sistema = PetCare()
        listOf("123ABC", "CA1CD", "CA123CD", " CA12CD", "CA12CD\n").forEach {
            assertFalse(sistema.registrarEntrada(canino(it)))
        }
        assertFalse(sistema.registrarEntrada(Canino("CA12CD", " ", "Raza", ingreso, TipoDueno.PARTICULAR)))
        assertFalse(sistema.registrarEntrada(Canino("CA12CD", "Max", " ", ingreso, TipoDueno.PARTICULAR)))
        assertNull(sistema.registrarSalida("ZZ00ZZ"))
        assertEquals(10, sistema.boxesDisponibles())
        assertTrue(sistema.registrarEntrada(canino()))
        assertFalse(sistema.registrarEntrada(canino("ca12cd")))
        assertNotNull(sistema.registrarSalida("CA12CD", ingreso.plusHours(1)))
        assertFalse(sistema.registrarEntrada(canino()))
        assertEquals(1, sistema.historial.size)
    }

    @Test
    fun capacidadDeBoxes() = runTest {
        val sistema = PetCare()
        repeat(10) { assertTrue(sistema.registrarEntrada(canino("CA${it.toString().padStart(2, '0')}CD"))) }
        assertEquals(0, sistema.boxesDisponibles())
        assertFalse(sistema.registrarEntrada(canino("CA99CD")))
        assertNotNull(sistema.registrarSalida("CA00CD", ingreso.plusHours(1)))
        assertTrue(sistema.registrarEntrada(canino("CA99CD")))
        assertEquals("CA99CD", (sistema.boxes.first().estado as EstadoBox.EnAtencion).paciente.codigoAtencion)
    }

    @Test
    fun estadosYEsperas() = runTest {
        val sistema = PetCare()
        val entrada = launch { sistema.registrarEntrada(canino()) }
        runCurrent()
        assertEquals(EstadoBox.EnProceso("Registrando entrada"), sistema.boxes[0].estado)
        assertEquals(9, sistema.boxesDisponibles())
        assertFalse(sistema.registrarEntrada(canino()))
        assertFalse(sistema.ponerFueraDeServicio(1, "Limpieza"))
        advanceTimeBy(2999)
        assertIs<EstadoBox.EnProceso>(sistema.boxes[0].estado)
        advanceTimeBy(1)
        runCurrent()
        entrada.join()
        assertIs<EstadoBox.EnAtencion>(sistema.boxes[0].estado)
        val salida = launch { sistema.registrarSalida("CA12CD", ingreso.plusHours(1)) }
        runCurrent()
        assertEquals(EstadoBox.EnProceso("Calculando tarifa"), sistema.boxes[0].estado)
        assertNull(sistema.registrarSalida("CA12CD", ingreso.plusHours(1)))
        advanceTimeBy(6499)
        assertTrue(sistema.historial.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        salida.join()
        assertEquals(EstadoBox.Libre, sistema.boxes[0].estado)
        assertEquals(1, sistema.historial.size)
    }

    @Test
    fun tarifasInvalidas() = runTest {
        listOf(-1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE).forEach { tarifa ->
            val sistema = PetCare()
            val paciente = object : Paciente("AA11BB", "Prueba", "Prueba", ingreso, TipoDueno.PARTICULAR) {
                override fun calcularCosto(tiempoMinutos: Int) = tarifa
            }
            assertTrue(sistema.registrarEntrada(paciente))
            assertNull(sistema.registrarSalida("AA11BB", ingreso.plusHours(1)))
            assertIs<EstadoBox.EnAtencion>(sistema.boxes[0].estado)
            assertTrue(sistema.historial.isEmpty())
            assertEquals(0.0, sistema.totalRecaudado)
        }
    }

    @Test
    fun tiemposInvalidos() = runTest {
        val sistema = PetCare()
        assertTrue(sistema.registrarEntrada(canino()))
        listOf(ingreso.minusMinutes(1), ingreso, ingreso.plusSeconds(59)).forEach {
            assertNull(sistema.registrarSalida("CA12CD", it))
            assertEquals(9, sistema.boxesDisponibles())
        }
        assertNotNull(sistema.registrarSalida("CA12CD", ingreso.plusMinutes(75)))
        assertFailsWith<IllegalArgumentException> { canino().calcularMontoFinal(-1) }
        assertFailsWith<IllegalArgumentException> {
            Felino("FE22TO", "Misi", "Siames", ingreso, TipoDueno.PARTICULAR).calcularMontoFinal(0)
        }
    }

    @Test
    fun turnoVacioYMantencion() {
        val sistema = PetCare()
        assertEquals(0.0, sistema.ingresoPromedio())
        assertNull(sistema.pacienteMayorTiempo())
        assertNull(sistema.tipoConMayorIngreso())
        assertTrue(sistema.codigosFinalizados().isEmpty())
        assertTrue(sistema.pacientesConvenio().isEmpty())
        assertTrue(sistema.ponerFueraDeServicio(1, "Mantencion"))
        assertEquals(9, sistema.boxesDisponibles())
        assertEquals("Fuera de servicio: Mantencion", sistema.boxes[0].descripcion())
        assertFalse(sistema.habilitarBox(11))
        assertFalse(sistema.ponerFueraDeServicio(2, " "))
        assertTrue(sistema.habilitarBox(1))
        assertEquals(10, sistema.boxesDisponibles())
        mostrarReporte(sistema)
    }

    @Test
    fun cancelarRegistro() = runTest {
        val sistema = PetCare()
        val entrada = launch { sistema.registrarEntrada(canino()) }
        runCurrent()
        entrada.cancel()
        entrada.join()
        assertEquals(10, sistema.boxesDisponibles())
        assertTrue(sistema.registrarEntrada(canino()))
        val salida = launch { sistema.registrarSalida("CA12CD", ingreso.plusHours(1)) }
        runCurrent()
        salida.cancel()
        salida.join()
        assertIs<EstadoBox.EnAtencion>(sistema.boxes[0].estado)
        assertTrue(sistema.historial.isEmpty())
        assertNotNull(sistema.registrarSalida("CA12CD", ingreso.plusHours(1)))
    }
}
