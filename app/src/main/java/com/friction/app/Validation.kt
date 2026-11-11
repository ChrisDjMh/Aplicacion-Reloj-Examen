package com.friction.app

const val MAX_AMOUNT = 100000.0
const val MAX_BUDGET = 100000.0

// Deja solo dígitos y un punto decimal (la coma se convierte en punto),
// máximo 6 dígitos enteros y 2 decimales.
fun sanitizeAmount(input: String): String {
    val normalized = input.replace(',', '.')
    val sb = StringBuilder()
    var dot = false
    var decimals = 0
    var integers = 0
    for (ch in normalized) {
        if (ch in '0'..'9') {
            if (dot) {
                if (decimals < 2) {
                    sb.append(ch)
                    decimals++
                }
            } else if (integers < 6) {
                sb.append(ch)
                integers++
            }
        } else if (ch == '.' && !dot) {
            dot = true
            sb.append(ch)
        }
    }
    return sb.toString()
}

// Devuelve el monto si es válido (finito, mayor que 0 y no mayor que max); si no, null.
fun parseAmount(text: String, max: Double): Double? {
    val v = text.toDoubleOrNull() ?: return null
    return if (v.isFinite() && v > 0.0 && v <= max) v else null
}