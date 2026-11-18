package com.example.math

data class ScientificConstant(
    val symbol: String,
    val insertToken: String,
    val name: String,
    val displayValue: String,
    val unit: String,
    val category: String
)

object ScientificConstants {
    val all: List<ScientificConstant> = listOf(
        ScientificConstant("π", "π", "Archimedes' Constant (Pi)", "3.141592653589793", "dimensionless", "Math"),
        ScientificConstant("e", "e", "Euler's Number", "2.718281828459045", "dimensionless", "Math"),
        ScientificConstant("φ", "φ", "Golden Ratio", "1.618033988749895", "dimensionless", "Math"),
        ScientificConstant("c", "c_light", "Speed of Light in Vacuum", "299792458", "m/s", "Physics"),
        ScientificConstant("h", "h_planck", "Planck Constant", "6.62607015E-34", "J·s", "Quantum"),
        ScientificConstant("G", "g_grav", "Newtonian Gravitation", "6.67430E-11", "m³/(kg·s²)", "Physics"),
        ScientificConstant("g₀", "g0", "Standard Gravity", "9.80665", "m/s²", "Mechanics"),
        ScientificConstant("Nₐ", "n_a", "Avogadro Constant", "6.02214076E23", "mol⁻¹", "Chemistry"),
        ScientificConstant("R", "r_gas", "Molar Gas Constant", "8.314462618", "J/(mol·K)", "Thermodynamics"),
        ScientificConstant("k_B", "k_b", "Boltzmann Constant", "1.380649E-23", "J/K", "Thermodynamics"),
        ScientificConstant("q_e", "q_e", "Elementary Charge", "1.602176634E-19", "C", "Electromagnetism"),
        ScientificConstant("ε₀", "eps0", "Vacuum Permittivity", "8.8541878188E-12", "F/m", "Electromagnetism"),
        ScientificConstant("μ₀", "mu0", "Vacuum Permeability", "1.25663706127E-6", "N/A²", "Electromagnetism")
    )
}
