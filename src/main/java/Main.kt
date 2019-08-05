import org.jsoup.Jsoup
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

fun main() {
    val initialURL = "https://www.carsales.com.au/cars/results/?q=(And.Service.Carsales._.Price.range(25000..30000)._.State.New+South+Wales._.Drive.4x4._.(C.Make.Toyota._.Model.Hilux.))&Sort=%7eOdometer"
    val doc = Jsoup.connect(initialURL).get()

    val cars = doc.getElementsByClass("listing-item").map {
        val nameYear = it.getElementsByAttribute("data-webm-clickvalue").first { it -> it.attributes().map { it.value }.contains("sv-title") }.text()
        val year = nameYear.split(" ").first().toInt()
        val name = nameYear.split(" ").drop(1).joinToString(" ")
        val price = it.getElementsByClass("price").first().text().toNumber()
        val keyDetails = it.getElementsByClass("key-details__value")
        val odometer = keyDetails.single { it.attributes().get("data-type") == "Odometer" }.text().toNumber()
        val bodyStyle = keyDetails.single { it.attributes().get("data-type") == "Body Style" }.text()
        val transmission = keyDetails.single { it.attributes().get("data-type") == "Transmission" }.text()
        val engine = keyDetails.single { it.attributes().get("data-type") == "Engine" }.text()
        Car(name, year , price, odometer, bodyStyle, transmission, engine)
    }
}

fun String.toNumber() = toCharArray().filter { it.isDigit() }.joinToString(separator = "").toInt()

private fun createLinks(): List<String> {

    val initialURL = "https://www.carsales.com.au/cars/results/?q=(And.Service.Carsales._.Price.range(25000..30000)._.State.New+South+Wales._.Drive.4x4._.(C.Make.Toyota._.Model.Hilux.))&Sort=%7eOdometer"
    val doc = Jsoup.connect(initialURL).get()
    val elementsContainingOwnText = doc.getElementsContainingOwnText("cars for sale in Australia").single { !it.text().contains("-") }
    val results = elementsContainingOwnText.text().split(" ").first().toInt()
    val pages = results / 12

    return IntStream.rangeClosed(0, pages).mapToObj { "$initialURL&offset=${it * 12}" }.collect(toList())
}


data class Car(val name: String, val year: Int, val price: Int, val odometer: Int, val bodyStyle: String, val transmission: String, val engine: String)