package org.example

import java.time.LocalDateTime

class Exotico(codigoAtencion: String, nombre: String, especie: String,
              fechaHoraIngreso: LocalDateTime, tipoDueno: TipoDueno, val silvestre: Boolean) :
    Paciente(codigoAtencion, nombre, especie, fechaHoraIngreso, tipoDueno) {
    override val tipoPaciente = "Exotico"

    override fun calcularCosto(tiempoMinutos: Int): Double {
        val costo = costoPorTiempo(tiempoMinutos, 20000.0)
        return if (silvestre) costo * 1.3 else costo
    }

    override fun detalle(): String = "${super.detalle()} | silvestre: ${if (silvestre) "si" else "no"}"
}
