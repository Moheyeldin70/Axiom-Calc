package com.example.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class UnitCategory(val title: String) {
    LENGTH("Length"),
    MASS("Mass"),
    TIME("Time"),
    TEMPERATURE("Temperature"),
    AREA("Area"),
    VOLUME("Volume"),
    SPEED("Speed"),
    FORCE("Force"),
    PRESSURE("Pressure"),
    ENERGY("Energy"),
    POWER("Power"),
    ANGLE("Angle"),
    DATA("Digital Data"),
    FREQUENCY("Frequency")
}

data class EngineeringUnit(
    val id: String,
    val symbol: String,
    val name: String,
    val toBaseFactor: BigDecimal, // valueInBase = value * toBaseFactor (for linear units)
    val isTemperature: Boolean = false
)

object UnitConverterEngine {
    private val MC = MathContext(34, RoundingMode.HALF_EVEN)

    val unitsByCategory: Map<UnitCategory, List<EngineeringUnit>> = mapOf(
        UnitCategory.LENGTH to listOf(
            EngineeringUnit("m", "m", "Meter", BigDecimal("1")),
            EngineeringUnit("km", "km", "Kilometer", BigDecimal("1000")),
            EngineeringUnit("cm", "cm", "Centimeter", BigDecimal("0.01")),
            EngineeringUnit("mm", "mm", "Millimeter", BigDecimal("0.001")),
            EngineeringUnit("um", "µm", "Micrometer", BigDecimal("1e-6")),
            EngineeringUnit("nm", "nm", "Nanometer", BigDecimal("1e-9")),
            EngineeringUnit("in", "in", "Inch", BigDecimal("0.0254")),
            EngineeringUnit("ft", "ft", "Foot", BigDecimal("0.3048")),
            EngineeringUnit("yd", "yd", "Yard", BigDecimal("0.9144")),
            EngineeringUnit("mi", "mi", "Mile", BigDecimal("1609.344")),
            EngineeringUnit("nmi", "nmi", "Nautical Mile", BigDecimal("1852"))
        ),
        UnitCategory.MASS to listOf(
            EngineeringUnit("kg", "kg", "Kilogram", BigDecimal("1")),
            EngineeringUnit("g", "g", "Gram", BigDecimal("0.001")),
            EngineeringUnit("mg", "mg", "Milligram", BigDecimal("1e-6")),
            EngineeringUnit("ug", "µg", "Microgram", BigDecimal("1e-9")),
            EngineeringUnit("t", "t", "Metric Tonne", BigDecimal("1000")),
            EngineeringUnit("lb", "lb", "Pound", BigDecimal("0.45359237")),
            EngineeringUnit("oz", "oz", "Ounce", BigDecimal("0.028349523125")),
            EngineeringUnit("slug", "slug", "Slug", BigDecimal("14.5939029372"))
        ),
        UnitCategory.TIME to listOf(
            EngineeringUnit("s", "s", "Second", BigDecimal("1")),
            EngineeringUnit("ms", "ms", "Millisecond", BigDecimal("0.001")),
            EngineeringUnit("us", "µs", "Microsecond", BigDecimal("1e-6")),
            EngineeringUnit("ns", "ns", "Nanosecond", BigDecimal("1e-9")),
            EngineeringUnit("min", "min", "Minute", BigDecimal("60")),
            EngineeringUnit("h", "h", "Hour", BigDecimal("3600")),
            EngineeringUnit("day", "d", "Day", BigDecimal("86400")),
            EngineeringUnit("wk", "wk", "Week", BigDecimal("604800"))
        ),
        UnitCategory.TEMPERATURE to listOf(
            EngineeringUnit("c", "°C", "Celsius", BigDecimal.ONE, isTemperature = true),
            EngineeringUnit("f", "°F", "Fahrenheit", BigDecimal.ONE, isTemperature = true),
            EngineeringUnit("k", "K", "Kelvin", BigDecimal.ONE, isTemperature = true),
            EngineeringUnit("r", "°R", "Rankine", BigDecimal.ONE, isTemperature = true)
        ),
        UnitCategory.AREA to listOf(
            EngineeringUnit("m2", "m²", "Square Meter", BigDecimal("1")),
            EngineeringUnit("km2", "km²", "Square Kilometer", BigDecimal("1e6")),
            EngineeringUnit("cm2", "cm²", "Square Centimeter", BigDecimal("1e-4")),
            EngineeringUnit("mm2", "mm²", "Square Millimeter", BigDecimal("1e-6")),
            EngineeringUnit("ha", "ha", "Hectare", BigDecimal("10000")),
            EngineeringUnit("acre", "ac", "Acre", BigDecimal("4046.8564224")),
            EngineeringUnit("ft2", "ft²", "Square Foot", BigDecimal("0.09290304")),
            EngineeringUnit("in2", "in²", "Square Inch", BigDecimal("0.00064516"))
        ),
        UnitCategory.VOLUME to listOf(
            EngineeringUnit("l", "L", "Liter", BigDecimal("0.001")),
            EngineeringUnit("m3", "m³", "Cubic Meter", BigDecimal("1")),
            EngineeringUnit("ml", "mL", "Milliliter (cm³)", BigDecimal("1e-6")),
            EngineeringUnit("gal", "gal", "Gallon (US)", BigDecimal("0.003785411784")),
            EngineeringUnit("qt", "qt", "Quart (US)", BigDecimal("0.000946352946")),
            EngineeringUnit("floz", "fl oz", "Fluid Ounce (US)", BigDecimal("2.95735295625e-5")),
            EngineeringUnit("ft3", "ft³", "Cubic Foot", BigDecimal("0.028316846592")),
            EngineeringUnit("in3", "in³", "Cubic Inch", BigDecimal("1.6387064e-5"))
        ),
        UnitCategory.SPEED to listOf(
            EngineeringUnit("ms", "m/s", "Meters per Second", BigDecimal("1")),
            EngineeringUnit("kmh", "km/h", "Kilometers per Hour", BigDecimal("1").divide(BigDecimal("3.6"), MC)),
            EngineeringUnit("mph", "mph", "Miles per Hour", BigDecimal("0.44704")),
            EngineeringUnit("knot", "kn", "Knot", BigDecimal("1852").divide(BigDecimal("3600"), MC)),
            EngineeringUnit("fts", "ft/s", "Feet per Second", BigDecimal("0.3048")),
            EngineeringUnit("mach", "Ma", "Mach (Std Atm)", BigDecimal("340.29"))
        ),
        UnitCategory.FORCE to listOf(
            EngineeringUnit("n", "N", "Newton", BigDecimal("1")),
            EngineeringUnit("kn", "kN", "Kilonewton", BigDecimal("1000")),
            EngineeringUnit("mn", "MN", "Meganewton", BigDecimal("1e6")),
            EngineeringUnit("dyn", "dyn", "Dyne", BigDecimal("1e-5")),
            EngineeringUnit("lbf", "lbf", "Pound-force", BigDecimal("4.4482216152605")),
            EngineeringUnit("kgf", "kgf", "Kilogram-force", BigDecimal("9.80665"))
        ),
        UnitCategory.PRESSURE to listOf(
            EngineeringUnit("pa", "Pa", "Pascal", BigDecimal("1")),
            EngineeringUnit("kpa", "kPa", "Kilopascal", BigDecimal("1000")),
            EngineeringUnit("mpa", "MPa", "Megapascal", BigDecimal("1e6")),
            EngineeringUnit("bar", "bar", "Bar", BigDecimal("100000")),
            EngineeringUnit("mbar", "mbar", "Millibar", BigDecimal("100")),
            EngineeringUnit("atm", "atm", "Standard Atmosphere", BigDecimal("101325")),
            EngineeringUnit("psi", "psi", "Pound per Sq Inch", BigDecimal("6894.757293168361")),
            EngineeringUnit("mmhg", "mmHg", "Millimeter of Mercury", BigDecimal("133.322387415"))
        ),
        UnitCategory.ENERGY to listOf(
            EngineeringUnit("j", "J", "Joule", BigDecimal("1")),
            EngineeringUnit("kj", "kJ", "Kilojoule", BigDecimal("1000")),
            EngineeringUnit("mj", "MJ", "Megajoule", BigDecimal("1e6")),
            EngineeringUnit("cal", "cal", "Calorie (thermochem)", BigDecimal("4.184")),
            EngineeringUnit("kcal", "kcal", "Kilocalorie", BigDecimal("4184")),
            EngineeringUnit("wh", "Wh", "Watt-hour", BigDecimal("3600")),
            EngineeringUnit("kwh", "kWh", "Kilowatt-hour", BigDecimal("3600000")),
            EngineeringUnit("ev", "eV", "Electronvolt", BigDecimal("1.602176634e-19")),
            EngineeringUnit("btu", "BTU", "British Thermal Unit", BigDecimal("1055.05585262")),
            EngineeringUnit("ftlbf", "ft·lbf", "Foot-pound", BigDecimal("1.3558179483314"))
        ),
        UnitCategory.POWER to listOf(
            EngineeringUnit("w", "W", "Watt", BigDecimal("1")),
            EngineeringUnit("kw", "kW", "Kilowatt", BigDecimal("1000")),
            EngineeringUnit("mw", "MW", "Megawatt", BigDecimal("1e6")),
            EngineeringUnit("hp", "hp", "Horsepower (Mech)", BigDecimal("745.6998715822702")),
            EngineeringUnit("ps", "PS", "Horsepower (Metric)", BigDecimal("735.49875")),
            EngineeringUnit("btuh", "BTU/h", "BTU per Hour", BigDecimal("1055.05585262").divide(BigDecimal("3600"), MC))
        ),
        UnitCategory.ANGLE to listOf(
            EngineeringUnit("deg", "°", "Degree", BigMath.PI.divide(BigDecimal("180"), MC)),
            EngineeringUnit("rad", "rad", "Radian", BigDecimal("1")),
            EngineeringUnit("grad", "gon", "Gradian", BigMath.PI.divide(BigDecimal("200"), MC)),
            EngineeringUnit("arcmin", "′", "Arcminute", BigMath.PI.divide(BigDecimal("10800"), MC)),
            EngineeringUnit("arcsec", "″", "Arcsecond", BigMath.PI.divide(BigDecimal("648000"), MC)),
            EngineeringUnit("rev", "rev", "Revolution", BigMath.TWO_PI)
        ),
        UnitCategory.DATA to listOf(
            EngineeringUnit("bit", "b", "Bit", BigDecimal("0.125")),
            EngineeringUnit("byte", "B", "Byte", BigDecimal("1")),
            EngineeringUnit("kb", "KB", "Kilobyte (10³)", BigDecimal("1000")),
            EngineeringUnit("mb", "MB", "Megabyte (10⁶)", BigDecimal("1e6")),
            EngineeringUnit("gb", "GB", "Gigabyte (10⁹)", BigDecimal("1e9")),
            EngineeringUnit("tb", "TB", "Terabyte (10¹²)", BigDecimal("1e12")),
            EngineeringUnit("kib", "KiB", "Kibibyte (2¹⁰)", BigDecimal("1024")),
            EngineeringUnit("mib", "MiB", "Mebibyte (2²⁰)", BigDecimal("1048576")),
            EngineeringUnit("gib", "GiB", "Gibibyte (2³⁰)", BigDecimal("1073741824"))
        ),
        UnitCategory.FREQUENCY to listOf(
            EngineeringUnit("hz", "Hz", "Hertz", BigDecimal("1")),
            EngineeringUnit("khz", "kHz", "Kilohertz", BigDecimal("1000")),
            EngineeringUnit("mhz", "MHz", "Megahertz", BigDecimal("1e6")),
            EngineeringUnit("ghz", "GHz", "Gigahertz", BigDecimal("1e9")),
            EngineeringUnit("rpm", "rpm", "Revolutions / Min", BigDecimal.ONE.divide(BigDecimal("60"), MC)),
            EngineeringUnit("rads", "rad/s", "Radians / Sec", BigDecimal.ONE.divide(BigMath.TWO_PI, MC))
        )
    )

    fun convert(
        value: BigDecimal,
        from: EngineeringUnit,
        to: EngineeringUnit
    ): BigDecimal {
        if (from.id == to.id) return value
        if (from.isTemperature && to.isTemperature) {
            return convertTemperature(value, from.id, to.id)
        }
        val inBase = value.multiply(from.toBaseFactor, MC)
        return BigMath.cleanZero(inBase.divide(to.toBaseFactor, MC))
    }

    private fun convertTemperature(value: BigDecimal, fromId: String, toId: String): BigDecimal {
        val cZeroOffset = BigDecimal("273.15")
        val nineFifths = BigDecimal("1.8")
        val thirtyTwo = BigDecimal("32")
        val kelvin = when (fromId) {
            "c" -> value.add(cZeroOffset, MC)
            "k" -> value
            "f" -> value.subtract(thirtyTwo, MC).divide(nineFifths, MC).add(cZeroOffset, MC)
            "r" -> value.divide(nineFifths, MC)
            else -> value
        }
        val result = when (toId) {
            "c" -> kelvin.subtract(cZeroOffset, MC)
            "k" -> kelvin
            "f" -> kelvin.subtract(cZeroOffset, MC).multiply(nineFifths, MC).add(thirtyTwo, MC)
            "r" -> kelvin.multiply(nineFifths, MC)
            else -> kelvin
        }
        return BigMath.cleanZero(result)
    }
}
