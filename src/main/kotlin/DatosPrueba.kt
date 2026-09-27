package org.example

import java.time.LocalDateTime

fun datosPrueba(ingreso: LocalDateTime): List<Pair<Paciente, Int>> = listOf(
    Canino("CA12CD", "Max", "Golden Retriever", ingreso, TipoDueno.CONVENIO) to 75,
    Canino("CA99ZA", "Luna", "Labrador", ingreso, TipoDueno.PARTICULAR) to 180,
    Felino("FE22TO", "Misi", "Siames", ingreso, TipoDueno.PARTICULAR) to 18,
    Exotico("EX44RG", "Loro", "Amazonico", ingreso, TipoDueno.MUNICIPAL, true) to 120,
    Exotico("EX77RG", "Iguana", "Verde", ingreso, TipoDueno.PARTICULAR, false) to 45
)
