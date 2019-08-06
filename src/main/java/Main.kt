import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

data class Car(val name: String, val year: Int, val price: Int, val odometer: Int, val bodyStyle: String, val transmission: String, val engine: String, val url: String)
data class CarToScrape(val filename: String, val url: String)

fun isSiteVersionA(doc: Document) = doc.getElementsByClass("carsales").size == 4
fun String.toNumber() = toCharArray().filter { it.isDigit() }.joinToString(separator = "").toInt()

fun main() {
    listOf(
            CarToScrape("/tmp/hilux.csv", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.State.New%20South%20Wales._.Drive.4x4._.%28C.Make.Toyota._.Model.Hilux.%29%29&Sort=~Odometer"),
            CarToScrape("/tmp/triton.csv", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Mitsubishi._.Model.Triton.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel"),
            CarToScrape("/tmp/ranger.csv", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Ford._.Model.Ranger.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel"),
            CarToScrape("/tmp/amarok.csv", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Volkswagen._.Model.Amarok.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel"),
            CarToScrape("/tmp/dmax.csv", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Isuzu._.Model.D-MAX.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel")
    ).forEach { scrape(it) }
    println("done and doneit co")
}

fun scrape(carToScrape: CarToScrape) {
    val links = createLinks(carToScrape)
    print("${links.size} pages to scrape for ${carToScrape.filename} ")
    val cars = getAllCars(links)
    writeCSV(cars, carToScrape.filename)
    println("done")
}

private fun createLinks(carToScrape: CarToScrape): List<String> {
    val doc = Jsoup.connect(carToScrape.url).get()
    val pages = if (isSiteVersionA(doc)) {
        doc.getElementsContainingOwnText("cars for sale in Australia").single { !it.text().contains("-") }.text().split(" ").first().toNumber() / 12
    } else {
        doc.getElementsContainingOwnText("cars for sale in New South Wales").first { it.text().endsWith(" Wales") }.text().split(" ").first().toNumber() / 12
    }
    return IntStream.rangeClosed(0, pages).mapToObj { "${carToScrape.url}&offset=${it * 12}" }.collect(toList())
}

private fun getAllCars(links: List<String>): List<Car> {
    val allCars = ConcurrentLinkedQueue<Car>()
    runBlocking {
        links.forEach {
            launch {
                val doc = withContext(Dispatchers.IO) { Jsoup.connect(it).get() }
                val cars = if (isSiteVersionA(doc)) scrapeForVersionA(doc) else scrapeForVersionB(doc)
                allCars.addAll(cars)
            }
        }
    }
    return allCars.toList()
}

fun scrapeForVersionA(it: Document): List<Car> {
    val cars = it.getElementsByClass("listing-item").mapNotNull { element ->
        try {
            val nameYear = element.getElementsByAttribute("data-webm-clickvalue").first { it -> it.attributes().map { it.value }.contains("sv-title") }.text()
            val year = nameYear.split(" ").first().toInt()
            val name = nameYear.split(" ").drop(1).joinToString(" ")
            val price = element.getElementsByClass("price").first().text().toNumber()
            val keyDetails = element.getElementsByClass("key-details__value")
            val odometer = keyDetails.single { it.attributes().get("data-type") == "Odometer" }.text().toNumber()
            val bodyStyle = keyDetails.single { it.attributes().get("data-type") == "Body Style" }.text()
            val transmission = keyDetails.single { it.attributes().get("data-type") == "Transmission" }.text()
            val engine = keyDetails.single { it.attributes().get("data-type") == "Engine" }.text()
            val url = "https://www.carsales.com.au" + element.getElementsByAttribute("href").filter { it.attr("href").startsWith("/cars/details") }.first().attr("href")
            Car(name, year, price, odometer, bodyStyle, transmission, engine, url)
        } catch (e: Exception) {
            null
        }
    }
    print(".")
    return cars
}

fun scrapeForVersionB(doc: Document): List<Car> {
    val cars = doc.getElementsByClass("listing-item").mapNotNull { element ->
        try {
            val nameYear = element.getElementsByAttribute("data-webm-clickvalue").first { it -> it.attributes().map { it.value }.contains("sv-view-title") }.text()
            val year = nameYear.split(" ").first().toInt()
            val name = nameYear.split(" ").drop(1).joinToString(" ")
            val keyDetails = element.getElementsByClass("vehicle-features")[0].allElements.map { it.text() }
            val odometer = keyDetails.single { it.startsWith("Odometer ") && it.endsWith(" km") }.split(" ")[1].toNumber()
            val bodyStyle = keyDetails.single { it.startsWith("Body ") }.split(" ")[1]
            val engine = keyDetails.single { it.startsWith("Engine ") }.split(" ").drop(1).joinToString(separator = " ")
            val transmission = keyDetails.single { it.startsWith("Transmission ") }.split(" ").drop(1).joinToString(separator = " ")
            val price = element.getElementsByClass("price").text().toNumber()
            val url = "https://www.carsales.com.au" + element.getElementsByAttribute("href").filter { it.attr("href").startsWith("/cars/details") }.first().attr("href")
            Car(name, year, price, odometer, bodyStyle, transmission, engine, url)
        } catch (e: Exception) {
            null
        }
    }
    print(".")
    return cars
}

private fun writeCSV(cars: List<Car>, filename: String) {
    File(filename).bufferedWriter().apply {
        write("name,year,price,odometer,bodyStyle,transmission,engine,url\n")
        cars.forEach { write("${it.name},${it.year},${it.price},${it.odometer},${it.bodyStyle},${it.transmission},${it.engine},${it.url}\n") }
        flush()
        close()
    }
}
