package org.example

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

// Usar desde runBlocking, en un solo hilo.
class PetCare {
    val boxes: List<Box> = List(10) { Box(it + 1) }
    private val tickets = mutableListOf<Ticket>()
    private val codigosRegistrados = mutableListOf<String>()
    val historial: List<Ticket> get() = tickets.toList()
    // Los ingresos se calculan desde los tickets.
    val totalRecaudado: Double get() = tickets.sumOf { it.montoPagado }

    fun recaudacionPorTipo(): Map<String, Double> =
        listOf("Canino", "Felino", "Exotico").associateWith { tipo ->
            tickets.filter { it.tipoPaciente == tipo }.sumOf { it.montoPagado }
        }

    private fun estaLibre(box: Box): Boolean = when (box.estado) {
        EstadoBox.Libre -> true
        is EstadoBox.EnAtencion, is EstadoBox.EnProceso, is EstadoBox.FueraDeServicio -> false
    }

    fun boxesDisponibles(): Int = boxes.filter { estaLibre(it) }.size
    fun pacientesConvenio(): List<Paciente> = tickets
        .filter { it.paciente.tipoDueno == TipoDueno.CONVENIO }.map { it.paciente }
    fun ingresoPromedio(): Double = if (tickets.isEmpty()) 0.0 else totalRecaudado / tickets.size
    fun codigosFinalizados(): List<String> = tickets.map { it.codigoAtencion }
    fun pacienteMayorTiempo(): Paciente? = tickets.maxByOrNull { it.tiempoMinutos }?.paciente
    fun tipoConMayorIngreso(): String? =
        if (tickets.isEmpty()) null else recaudacionPorTipo().maxByOrNull { it.value }?.key

    suspend fun registrarEntrada(paciente: Paciente): Boolean {
        var boxReservado: Box? = null
        var codigoReservado = false
        try {
            paciente.validar()
            require(codigosRegistrados.none { it.equals(paciente.codigoAtencion, ignoreCase = true) }) {
                "El codigo ${paciente.codigoAtencion} ya fue registrado en este turno."
            }
            val box = boxes.firstOrNull { estaLibre(it) }
                ?: throw IllegalArgumentException("Sistema sin capacidad: no hay boxes libres.")
            boxReservado = box
            // Reservar el box y el codigo antes de esperar.
            codigosRegistrados.add(paciente.codigoAtencion)
            codigoReservado = true
            box.estado = EstadoBox.EnProceso("Registrando entrada")
            println("Box ${box.numero}: ${box.descripcion()}")
            delay(3000)
            box.estado = EstadoBox.EnAtencion(paciente)
            println("Entrada registrada en box ${box.numero}: ${paciente.codigoAtencion} | ${paciente.detalle()}")
            return true
        } catch (error: CancellationException) {
            boxReservado?.estado = EstadoBox.Libre
            if (codigoReservado) codigosRegistrados.remove(paciente.codigoAtencion)
            throw error
        } catch (error: IllegalArgumentException) {
            boxReservado?.estado = EstadoBox.Libre
            if (codigoReservado) codigosRegistrados.remove(paciente.codigoAtencion)
            println("No se pudo registrar la entrada: ${error.message}")
            return false
        }
    }

    suspend fun registrarSalida(codigo: String, fechaHoraSalida: LocalDateTime = LocalDateTime.now()): Ticket? {
        var boxReservado: Box? = null
        var atencionAnterior: EstadoBox.EnAtencion? = null
        try {
            val box = boxes.firstOrNull { box ->
                when (val estado = box.estado) {
                    is EstadoBox.EnAtencion -> estado.paciente.codigoAtencion.equals(codigo, ignoreCase = true)
                    EstadoBox.Libre, is EstadoBox.EnProceso, is EstadoBox.FueraDeServicio -> false
                }
            } ?: throw IllegalArgumentException("Paciente no encontrado en atencion: $codigo.")
            val atencion = box.estado as EstadoBox.EnAtencion
            val paciente = atencion.paciente
            boxReservado = box
            atencionAnterior = atencion
            box.estado = EstadoBox.EnProceso("Calculando tarifa")
            println("Box ${box.numero}: ${box.descripcion()}")
            delay(6500)
            val minutos = Duration.between(paciente.fechaHoraIngreso, fechaHoraSalida).toMinutes()
            require(minutos in 1..Int.MAX_VALUE.toLong()) { "El tiempo de atencion debe ser positivo y valido." }
            val monto = paciente.calcularMontoFinal(minutos.toInt())
            val ticket = Ticket(tickets.size + 1, paciente, minutos.toInt(), monto)
            tickets.add(ticket)
            box.estado = EstadoBox.Libre
            println("Salida registrada: ticket ${ticket.numeroTicket} | $codigo | ${ticket.tiempoMinutos} min | ${dinero(monto)}")
            return ticket
        } catch (error: CancellationException) {
            if (boxReservado != null && atencionAnterior != null) boxReservado.estado = atencionAnterior
            throw error
        } catch (error: IllegalArgumentException) {
            // Si falla el cobro, el paciente sigue en atencion.
            if (boxReservado != null && atencionAnterior != null) boxReservado.estado = atencionAnterior
            println("No se pudo registrar la salida: ${error.message}")
            return null
        }
    }

    fun ponerFueraDeServicio(numero: Int, motivo: String): Boolean = cambiarDisponibilidad(numero, motivo)
    fun habilitarBox(numero: Int): Boolean = cambiarDisponibilidad(numero, null)

    private fun cambiarDisponibilidad(numero: Int, motivo: String?): Boolean {
        return try {
            val box = boxes.firstOrNull { it.numero == numero }
                ?: throw IllegalArgumentException("No existe el box $numero.")
            require(motivo == null || motivo.isNotBlank()) { "Debes indicar el motivo." }
            when (box.estado) {
                EstadoBox.Libre, is EstadoBox.FueraDeServicio ->
                    box.estado = if (motivo == null) EstadoBox.Libre else EstadoBox.FueraDeServicio(motivo)
                is EstadoBox.EnAtencion, is EstadoBox.EnProceso ->
                    throw IllegalArgumentException("El box $numero esta ocupado o en proceso.")
            }
            true
        } catch (error: IllegalArgumentException) {
            println("No se pudo cambiar el box: ${error.message}")
            false
        }
    }
}
