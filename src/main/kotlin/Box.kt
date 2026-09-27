package org.example

class Box(val numero: Int) {
    var estado: EstadoBox = EstadoBox.Libre
        internal set

    fun descripcion(): String = when (val actual = estado) {
        EstadoBox.Libre -> "Libre"
        is EstadoBox.EnAtencion -> "En atencion: ${actual.paciente.codigoAtencion}"
        is EstadoBox.EnProceso -> "En proceso: ${actual.motivo}"
        is EstadoBox.FueraDeServicio -> "Fuera de servicio: ${actual.motivo}"
    }
}
