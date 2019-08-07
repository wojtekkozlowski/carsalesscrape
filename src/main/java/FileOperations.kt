import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

fun writeJson(cars: List<Car>, filename: String) {
    FileWriter("$filename.json").apply {
        Gson().toJson(cars, this)
        flush()
        close()
    }
}

fun writeCSV(cars: List<Car>, filename: String) {
    File("$filename.csv").bufferedWriter().apply {
        write("name,year,price,odometer,bodyStyle,transmission,engine,url\n")
        cars.forEach { write("${it.name},${it.year},${it.price},${it.odometer},${it.bodyStyle},${it.transmission},${it.engine},${it.url}\n") }
        flush()
        close()
    }
}

fun loadCars(s: String): List<Car> = Gson().fromJson(FileReader(s), TypeToken.getParameterized(ArrayList::class.java, Car::class.java).type)