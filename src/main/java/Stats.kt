import kotlin.math.roundToInt

fun averageKms() {
    listOf(
            Pair("ranger", "$scrapePath/ranger_$now.json"),
            Pair("hilux", "$scrapePath/hilux_$now.json"),
            Pair("triton", "$scrapePath/triton_$now.json")
//            Pair("d22", "$scrapePath/d22_$now.json")
    ).forEach {
        println("--- ${it.first} ---")
        averageKms(it.second)
    }
}

fun averageKms(s: String) {
    loadCars(s)
            .filter { it.bodyStyle == "Ute" }
            .filter { it.transmission == "Automatic" }
            .groupBy { it.year }
            .toSortedMap()
            .forEach { (year, cars) ->
                println("$year,${cars.map { it.price }.average().roundToInt()}")
            }
}

fun filterDown() {
    listOf(
            "$scrapePath/ranger_$now.json",
            "$scrapePath/hilux_$now.json",
            "$scrapePath/triton_$now.json"
    )
            .flatMap { loadCars(it) }
            .filter { it.price < 25000 }
            .filter { it.bodyStyle == "Ute" }
            .filter { it.transmission == "Automatic" }
            .sortedWith(scoreComparator)
            .forEachIndexed { index, car ->
                println("${index + 1}. (score ${car.score().roundToInt()}): ${car.url}")
            }
}

