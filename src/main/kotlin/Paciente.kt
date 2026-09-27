package org.example

import java.time.LocalDateTime

open class Paciente(
    val codigoAtencion: String,
    val nombre: String,
    val especie: String,
    val fechaHoraIngreso: LocalDateTime,
    val tipoDueno: TipoDueno
) {
    open val tipoPaciente: String = "Paciente"

    fun validar() {
        require(Regex("[A-Za-z]{2}[0-9]{2}[A-Za-z]{2}").matches(codigoAtencion)) {
            "Codigo invalido: debe contener dos letras, dos digitos y dos letras (CA12CD)."
        }
        require(nombre.isNotBlank()) { "El nombre no puede estar vacio." }
        require(especie.isNotBlank()) { "La especie o raza no puede estar vacia." }
    }

    protected fun costoPorTiempo(tiempoMinutos: Int, tarifaHora: Double): Double {
        require(tiempoMinutos > 0) { "El tiempo debe ser mayor que cero minutos." }
        require(tarifaHora.isFinite() && tarifaHora > 0) { "La tarifa por hora debe ser positiva." }
        return tarifaHora * tiempoMinutos / 60.0
    }

    open fun calcularCosto(tiempoMinutos: Int): Double =
        throw IllegalArgumentException("El paciente debe tener un tipo con tarifa definida.")

    fun calcularMontoFinal(tiempoMinutos: Int): Double {
        require(tiempoMinutos > 0) { "El tiempo debe ser mayor que cero minutos." }
        val costo = calcularCosto(tiempoMinutos)
        val gratuito = this is Felino && tiempoMinutos < 20
        require(costo.isFinite() && (costo > 0 || (costo == 0.0 && gratuito))) {
            "Resultado de tarifa invalido: el costo debe ser positivo, salvo la gratuidad felina."
        }
        // Aplicar IVA antes del descuento municipal.
        val conIva = costo * 1.19
        val monto = if (tipoDueno == TipoDueno.MUNICIPAL) conIva * 0.5 else conIva
        require(monto.isFinite()) { "Resultado de tarifa invalido: monto fuera de rango." }
        return monto
    }

    open fun detalle(): String = "$nombre | $especie | dueno: $tipoDueno"
}
