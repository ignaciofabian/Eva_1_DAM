package org.example

import java.time.LocalDateTime

class Felino(codigoAtencion: String, nombre: String, especie: String,
             fechaHoraIngreso: LocalDateTime, tipoDueno: TipoDueno) :
    Paciente(codigoAtencion, nombre, especie, fechaHoraIngreso, tipoDueno) {
    override val tipoPaciente = "Felino"

    override fun calcularCosto(tiempoMinutos: Int): Double {
        val costo = costoPorTiempo(tiempoMinutos, 9000.0)
        return if (tiempoMinutos < 20) 0.0 else costo
    }
}
