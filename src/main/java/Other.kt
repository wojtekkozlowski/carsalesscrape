import org.jsoup.nodes.Document

fun isSiteVersionA(doc: Document) = doc.getElementsByClass("carsales").size == 4
fun String.toNumber() = toCharArray().filter { it.isDigit() }.joinToString(separator = "").toInt()
