package com.example.math

enum class AngleUnit(val label: String) {
    DEG("DEG"),
    RAD("RAD"),
    GRAD("GRAD");

    fun next(): AngleUnit = when (this) {
        DEG -> RAD
        RAD -> GRAD
        GRAD -> DEG
    }
}

enum class NotationMode(val label: String) {
    NORMAL("NORM"),
    SCIENTIFIC("SCI"),
    ENGINEERING("ENG");

    fun next(): NotationMode = when (this) {
        NORMAL -> SCIENTIFIC
        SCIENTIFIC -> ENGINEERING
        ENGINEERING -> NORMAL
    }
}
