import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

val total = AtomicInteger()
val done = AtomicInteger()

fun main() {
    val initialURL = "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.State.New%20South%20Wales._.Drive.4x4._.%28C.Make.Toyota._.Model.Hilux.%29%29&Sort=~Odometer"
    val doc = Jsoup.connect(initialURL).get()

    val siteVersion = siteVersion(doc)

    val pages = if (siteVersion == 4) {
        doc.getElementsContainingOwnText("cars for sale in Australia").single { !it.text().contains("-") }.text().split(" ").first().toNumber() / 12
    } else {
        doc.getElementsContainingOwnText("cars for sale in New South Wales").first { it.text().endsWith(" Wales") }.text().split(" ").first().toNumber() / 12
    }

    val links = IntStream.rangeClosed(0, pages).mapToObj { "$initialURL&offset=${it * 12}" }.collect(toList())
    total.set(links.size)

    val cars = links.parallelStream()
            .map { Jsoup.connect(it).get() }
            .map {
                if (siteVersion(it) == 4) {
                    getForVersionA(it)
                } else {
                    getForVersionB(it)
                }
            }
            .collect(toList())
            .filter(Objects::nonNull)
            .flatMap { it!!.toList() }
    println(cars.size)
}

private fun siteVersion(doc: Document): Int {
    return doc.getElementsByClass("carsales").size
}

data class Car(val name: String, val year: Int, val price: Int, val odometer: Int, val bodyStyle: String, val transmission: String, val engine: String)

fun String.toNumber() = toCharArray().filter { it.isDigit() }.joinToString(separator = "").toInt()

private fun getForVersionA(it: Document): List<Car> {
    val cars =  it.getElementsByClass("listing-item").mapNotNull {
        try {
            val nameYear = it.getElementsByAttribute("data-webm-clickvalue").first { it -> it.attributes().map { it.value }.contains("sv-title") }.text()
            val year = nameYear.split(" ").first().toInt()
            val name = nameYear.split(" ").drop(1).joinToString(" ")
            val price = it.getElementsByClass("price").first().text().toNumber()
            val keyDetails = it.getElementsByClass("key-details__value")
            val odometer = keyDetails.single { it.attributes().get("data-type") == "Odometer" }.text().toNumber()
            val bodyStyle = keyDetails.single { it.attributes().get("data-type") == "Body Style" }.text()
            val transmission = keyDetails.single { it.attributes().get("data-type") == "Transmission" }.text()
            val engine = keyDetails.single { it.attributes().get("data-type") == "Engine" }.text()
            Car(name, year, price, odometer, bodyStyle, transmission, engine)
        } catch(e:Exception){
            null
        }
    }
    println("done ${done.incrementAndGet()} / ${total.get()}")
    return cars
}

private fun getForVersionB(doc: Document): List<Car> {
    val cars =  doc.getElementsByClass("listing-item").mapNotNull {
        try {
            val nameYear = it.getElementsByAttribute("data-webm-clickvalue").first { it -> it.attributes().map { it.value }.contains("sv-view-title") }.text()
            val year = nameYear.split(" ").first().toInt()
            val name = nameYear.split(" ").drop(1).joinToString(" ")
            val keyDetails = it.getElementsByClass("vehicle-features")[0].allElements.map { it.text() }
            val odometer = keyDetails.single { it.startsWith("Odometer ") && it.endsWith(" km") }.split(" ")[1].toNumber()
            val bodyStyle = keyDetails.single { it.startsWith("Body ") }.split(" ")[1]
            val engine = keyDetails.single { it.startsWith("Engine ") }.split(" ").drop(1).joinToString(separator = " ")
            val transmission = keyDetails.single { it.startsWith("Transmission ") }.split(" ").drop(1).joinToString(separator = " ")
            val price = it.getElementsByClass("price").text().toNumber()
            Car(name, year, price, odometer, bodyStyle, transmission, engine)
        } catch (e:Exception){
            null
        }
    }
    println("done ${done.incrementAndGet()} / ${total.get()}")
    return cars
}
