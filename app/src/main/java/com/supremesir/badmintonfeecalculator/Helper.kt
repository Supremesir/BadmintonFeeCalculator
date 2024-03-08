package com.supremesir.badmintonfeecalculator

fun String.safeToDouble() = this.toDoubleOrNull() ?: 0.0

fun String.safeToInt() = this.toIntOrNull() ?: 0