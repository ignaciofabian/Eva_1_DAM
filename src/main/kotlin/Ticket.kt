package org.example

data class Ticket(
    val numeroTicket: Int,
    val paciente: Paciente,
    val tiempoMinutos: Int,
    val montoPagado: Double
) {
    val tipoPaciente: String get() = paciente.tipoPaciente
    val codigoAtencion: String get() = paciente.codigoAtencion
}
