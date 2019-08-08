import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.ConcurrentLinkedQueue

data class CarToScrape(val filename: String, val url: String)

fun main() {
    scrape()
//    filterDown()
//    averageKms()
}

fun scrape() {
    createScrapeDir()
    listOf(
            CarToScrape("d22", "https://www.carsales.com.au/cars/results/?q=(And.Service.Carsales._.State.New+South+Wales._.BodyStyle.Ute._.Drive.4x4._.(C.Make.Nissan._.(C.Model.Navara._.Series.D22.)))"),
            CarToScrape("hilux", "https://www.carsales.com.au/cars/results/?q=(And.Service.Carsales._.(C.Make.Toyota._.Model.Hilux.)_.State.New+South+Wales._.BodyStyle.Ute._.Drive.4x4.)"),
            CarToScrape("triton", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Mitsubishi._.Model.Triton.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel"),
            CarToScrape("ranger", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Ford._.Model.Ranger.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel"),
            CarToScrape("amarok", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Volkswagen._.Model.Amarok.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel"),
            CarToScrape("dmax", "https://www.carsales.com.au/cars/results/?q=%28And.Service.Carsales._.%28C.Make.Isuzu._.Model.D-MAX.%29_.State.New%20South%20Wales._.Drive.4x4.%29&WT.z_srchsrcx=makemodel")
    ).forEach { scrape(it) }
    println("done and done")
}

fun scrape(carToScrape: CarToScrape) {
    val links = createLinks(carToScrape)
    print("${links.size} pages to scrape for ${carToScrape.filename} ")
    val cars = scrapeAllLinks(links)
    writeCSV(cars, carToScrape.filename)
    writeJson(cars, carToScrape.filename)
    println("done, ${cars.size} cars")
}

private fun createLinks(carToScrape: CarToScrape): List<String> {
    val doc = Jsoup.connect(carToScrape.url).get()
    val siteVersionA = isSiteVersionA(doc)
    val pages = if (siteVersionA) {
        doc.getElementsContainingOwnText("cars for sale in Australia").single { !it.text().contains("-") }.text().split(" ").first().toNumber() / 12
    } else {
        doc.getElementsContainingOwnText("cars for sale in New South Wales").first { it.text().endsWith(" Wales") }.text().split(" ").first().toNumber() / 12
    }
    return (0..pages).map { "${carToScrape.url}&offset=${it * 12}" }
}

private fun scrapeAllLinks(links: List<String>): List<Car> {
    val allCars = ConcurrentLinkedQueue<Car>()
    runBlocking {
        links.forEach {
            launch {
                val doc = withContext(Dispatchers.IO) { Jsoup.connect(it).get() }
                val cars = if (isSiteVersionA(doc)) scrapeCarsFromVersionAPage(doc) else scrapeCarsFromVersionBPage(doc)
                allCars.addAll(cars)
            }
        }
    }
    return allCars.toList()
}

fun scrapeCarsFromVersionAPage(it: Document): List<Car> {
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

fun scrapeCarsFromVersionBPage(doc: Document): List<Car> {
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

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            scrape()
        }
    }
}


