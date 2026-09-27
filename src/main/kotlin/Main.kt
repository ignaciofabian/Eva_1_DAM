package org.example

import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

fun main() = runBlocking {
    val sistema = PetCare()
    val ingreso = LocalDateTime.of(2026, 9, 26, 9, 0)
    val casos = datosPrueba(ingreso)
    println("========== PETCARE: INICIO DE TURNO ==========")
    println("Capacidad: ${sistema.boxesDisponibles()} boxes")
    println("Simulacion con fechas de salida; esperas reales de registro y cobro.")

    sistema.registrarEntrada(Canino("123ABC", "Error", "Prueba", ingreso, TipoDueno.PARTICULAR))
    casos.forEach { (paciente, _) -> sistema.registrarEntrada(paciente) }
    sistema.registrarSalida("ZZ00ZZ", ingreso.plusHours(1))
    casos.forEach { (paciente, minutos) ->
        sistema.registrarSalida(paciente.codigoAtencion, paciente.fechaHoraIngreso.plusMinutes(minutos.toLong()))
    }
    mostrarReporte(sistema)
}
