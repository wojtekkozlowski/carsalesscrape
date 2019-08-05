import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

fun main() {
    val initialURL = "https://www.carsales.com.au/cars/results/?q=(And.Service.Carsales._.Price.range(25000..30000)._.State.New+South+Wales._.Drive.4x4._.(C.Make.Toyota._.Model.Hilux.))&Sort=%7eOdometer"

    val cars = createLinks(initialURL)
            .parallelStream()
            .map { loadPage(it) }
            .map {
                it.getElementsByClass("listing-item").map {
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
                }

            }
            .collect(toList())
            .flatMap { it.toList() }
}

fun String.toNumber() = toCharArray().filter { it.isDigit() }.joinToString(separator = "").toInt()

private fun createLinks(initialURL: String): List<String> {
    val elementsContainingOwnText = loadPage(initialURL).getElementsContainingOwnText("cars for sale in Australia").single { !it.text().contains("-") }
    val results = elementsContainingOwnText.text().split(" ").first().toInt()
    val pages = results / 12
    return IntStream.rangeClosed(0, pages).mapToObj { "$initialURL&offset=${it * 12}" }.collect(toList())
}

private fun loadPage(initialURL: String): Document {
    var found = false
    var doc: Document? = null
    while (!found) {
        val tempDoc = Jsoup.connect(initialURL).get()
        if (tempDoc.getElementsContainingOwnText("cars for sale in Australia").filter { !it.text().contains("-") }.size == 1) {
            found = true
            doc = tempDoc
            println("loaded ok: $initialURL")
        } else {
            println("loading again url: $initialURL")
        }
    }
    return doc!!
}

data class Car(val name: String, val year: Int, val price: Int, val odometer: Int, val bodyStyle: String, val transmission: String, val engine: String)