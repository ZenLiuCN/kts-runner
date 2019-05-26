import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory


fun parseXml(data: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    .parse(ByteArrayInputStream(data.toByteArray()))

fun parseXml(data: ByteArray) = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    .parse(ByteArrayInputStream(data))

fun parseXml(data: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    .parse(data)

inline fun NodeList.foreachIndexed(action: (Node, Int) -> Unit) {
    for (i in 0 until length) {
        action.invoke(this.item(i), i)
    }
}

inline fun NodeList.foreach(action: (Node) -> Unit) {
    for (i in 0 until length) {
        action.invoke(this.item(i))
    }
}

inline fun <T> NodeList.map(action: (Node) -> T) = run {
    val maped = mutableListOf<T>()
    for (i in 0 until length) {
        maped.add(action.invoke(this.item(i)))
    }
    maped
}

inline fun NodeList.find(action: (Node) -> Boolean): Node? = run {
    for (i in 0 until length) {
        if (action.invoke(this.item(i))) {
            return this.item(i)
        }
    }
    return null
}

fun Node?.el(path: String) = when {
    path.startsWith("@") -> this?.attributes?.getNamedItem(path.removePrefix("@"))
    else -> this?.childNodes?.find { it.nodeName == path }
}

val Node?.text: String? get() = this?.textContent
