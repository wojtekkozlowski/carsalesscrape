import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val homeDir: String = System.getProperty("user.home")
const val scrapeDir = "carsales-scrape"
val scrapePath = File("$homeDir/$scrapeDir")
val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun createScrapeDir() {
    if (!scrapePath.exists()) {
        scrapePath.mkdir()
    }
}

fun writeJson(cars: List<Car>, filename: String) {
    FileWriter("$scrapePath/${filename}_$now.json").apply {
        Gson().toJson(cars, this)
        flush()
        close()
    }
}

fun writeCSV(cars: List<Car>, filename: String) {
    File("$scrapePath/${filename}_$now.csv").bufferedWriter().apply {
        write("name,year,price,odometer,bodyStyle,transmission,engine,url\n")
        cars.forEach { write("${it.name},${it.year},${it.price},${it.odometer},${it.bodyStyle},${it.transmission},${it.engine},${it.url}\n") }
        flush()
        close()
    }
}

fun loadCars(s: String): List<Car> = Gson().fromJson(FileReader(s), TypeToken.getParameterized(ArrayList::class.java, Car::class.java).type)

