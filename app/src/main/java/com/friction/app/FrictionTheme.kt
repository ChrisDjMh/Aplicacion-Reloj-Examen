package com.friction.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object FColors {
    val Green = Color(0xFF4DB872)
    val Dark = Color(0xFF1F7A4D)
    val Deep = Color(0xFF14532D)
    val Blue = Color(0xFF3B9AE1)
    val Muted = Color(0xFF6B7C73)
    val Soft = Color(0xFFF1F7F3)
    val Border = Color(0xFFD8E6DC)
    val Ink = Color(0xFF153D29)
    val High = Color(0xFFD32F2F)
    val Medium = Color(0xFFF59E0B)
    val Low = Color(0xFF2E9E5B)
}

const val USER_NAME = "Alex"

// Fuente del diseño: Poppins. Para usarla, pon poppins_regular.ttf, poppins_medium.ttf,
// poppins_semibold.ttf, poppins_bold.ttf y poppins_extrabold.ttf en res/font y cambia esto por:
// FontFamily(Font(R.font.poppins_regular, FontWeight.Normal), Font(R.font.poppins_medium, FontWeight.Medium),
//            Font(R.font.poppins_semibold, FontWeight.SemiBold), Font(R.font.poppins_bold, FontWeight.Bold),
//            Font(R.font.poppins_extrabold, FontWeight.ExtraBold))
val AppFont: FontFamily = FontFamily.SansSerif