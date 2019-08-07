data class Car(val name: String, val year: Int, val price: Int, val odometer: Int, val bodyStyle: String, val transmission: String, val engine: String, val url: String)

fun Car.score(): Double {
    val decreaseValue: (acc: Double, Double) -> Double = { a, b -> a * (1 - b) }
    val odo = odometer / 10.0
    val pr = price / 1_000.0

    val depreciation = when (2019 - year) {
        0 -> 1.0
        1 -> 1 - 0.25
        2 -> listOf(0.25, 0.12).fold(1.0, decreaseValue)
        3 -> listOf(0.25, 0.12, 0.12).fold(1.0, decreaseValue)
        4 -> listOf(0.25, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        5 -> listOf(0.25, 0.12, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        6 -> listOf(0.25, 0.12, 0.12, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        7 -> listOf(0.25, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        8 -> listOf(0.25, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        9 -> listOf(0.25, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        10 -> listOf(0.25, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12).fold(1.0, decreaseValue)
        else -> 0.1
    }
    return Math.sqrt(Math.pow(odo, 2.0) + Math.pow(pr, 2.0)) * (1.0 / depreciation)
}

val scoreComparator = Comparator<Car> { o1, o2 ->
    when {
        o1.score() - o2.score() == 0.0 -> 0
        o1.score() - o2.score() < 0 -> -1
        else -> 1
    }
}