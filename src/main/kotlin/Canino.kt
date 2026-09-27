package org.example

import java.time.LocalDateTime

class Canino(codigoAtencion: String, nombre: String, especie: String,
             fechaHoraIngreso: LocalDateTime, tipoDueno: TipoDueno) :
    Paciente(codigoAtencion, nombre, especie, fechaHoraIngreso, tipoDueno) {
    override val tipoPaciente = "Canino"

    override fun calcularCosto(tiempoMinutos: Int): Double {
        val costo = costoPorTiempo(tiempoMinutos, 12000.0)
        return if (tipoDueno == TipoDueno.CONVENIO) costo * 0.8 else costo
    }
}
