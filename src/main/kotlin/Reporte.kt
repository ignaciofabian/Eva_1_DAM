package org.example

import java.text.NumberFormat
import java.util.Locale

fun dinero(monto: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(monto)

fun mostrarReporte(sistema: PetCare) {
    println("\n========== PETCARE: CIERRE DE TURNO ==========")
    if (sistema.historial.isEmpty()) println("No hubo pacientes atendidos.")
    sistema.historial.forEach { ticket ->
        println("Ticket ${ticket.numeroTicket} | ${ticket.tipoPaciente} | ${ticket.codigoAtencion} | " +
            "${ticket.tiempoMinutos} min | ${dinero(ticket.montoPagado)}")
        println("  ${ticket.paciente.detalle()}")
    }
    println("--------------------------------------------")
    println("Total recaudado: ${dinero(sistema.totalRecaudado)}")
    println("Pacientes atendidos: ${sistema.historial.size}")
    println("Ingreso promedio: ${dinero(sistema.ingresoPromedio())}")
    sistema.recaudacionPorTipo().forEach { (tipo, monto) -> println("Recaudacion $tipo: ${dinero(monto)}") }
    println("Tipo con mas ingresos: ${sistema.tipoConMayorIngreso() ?: "Sin atenciones"}")
    println("Boxes disponibles al cierre: ${sistema.boxesDisponibles()}")
    println("Pacientes CONVENIO: ${sistema.pacientesConvenio().joinToString { it.nombre }.ifEmpty { "Ninguno" }}")
    println("Codigos finalizados: ${sistema.codigosFinalizados().joinToString().ifEmpty { "Ninguno" }}")
    println("Paciente con mayor tiempo: ${sistema.pacienteMayorTiempo()?.nombre ?: "Sin atenciones"}")
}
