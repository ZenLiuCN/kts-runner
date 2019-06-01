import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

abstract class XmindNode(
    open val xid: String,
    open val modifier: String,
    open val timestamp: Long
) {
    constructor(node: Node) : this(
        xid = node.el("@id")!!.text!!,
        modifier = node.el("@modified-by")!!.text!!,
        timestamp = node.el("@timestamp")!!.text!!.toLong()
    )

    override fun toString(): String = "${this::class.simpleName}(xid=$xid,modifier=$modifier,timestamp=$timestamp)"
}

data class XNode(
    override val xid: String,
    override val modifier: String,
    override val timestamp: Long
) : XmindNode(
    xid,
    modifier,
    timestamp
)

fun createXNode(
    xid: String,
    modifier: String,
    timestamp: Long
) = XNode(
    xid,
    modifier,
    timestamp
)

class Topic(
    var title: String,
    val child: Set<Topic>?,
    val marker: Set<String>?,
    val label: String?,
    val note: String?,
    xid: String,
    modifier: String,
    timestamp: Long
) : XmindNode(
    xid,
    modifier,
    timestamp
), Cloneable {
    constructor(node: Node) : this(
        title = node.el("title")!!.text!!,
        label = node.el("labels")?.el("label")?.text,
        note = node.el("notes")?.el("plain")?.text,
        child = node.el("children")?.el("topics")?.childNodes?.map { Topic(it) }?.toSet(),
        marker = node.el("marker-refs")?.childNodes?.map { it.el("@marker-id")?.textContent }?.filterNotNull()?.toSet(),
        xid = node.el("@id")!!.text!!,
        modifier = node.el("@modified-by")!!.text!!,
        timestamp = node.el("@timestamp")!!.text!!.toLong()
    )

    fun markerOf(marker: String) = this.marker?.find { it == marker }
    override fun clone(): Topic = super.clone() as Topic
    fun deepClone(): Topic = this.clone().let {
        Topic(
            title = it.title,
            child = it.child?.map { it.deepClone() }?.toSet(),
            marker = it.marker,
            label = it.label,
            note = it.note,
            xid = it.xid,
            modifier = it.modifier,
            timestamp = it.timestamp
        )
    }

    override fun toString(): String =
        "${this::class.simpleName}(xid=$xid,modifier=$modifier,timestamp=$timestamp,title=$title,label=$label,note=$note,children=$child,marker=$marker)"

    companion object {
        fun parse(node: Node) = Topic(node)
    }
}

class Sheet(node: Node) : XmindNode(node) {
    val title: String
    val root: Topic
    val relationships: Set<Relationship>?

    init {
        title = node.el("title")!!.text!!
        root = node.el("topic")?.let { Topic(it) }!!
        relationships = node.el("relationships")?.childNodes?.map { Relationship(it) }?.toSet()
    }

    override fun toString(): String =
        "${this::class.simpleName}(xid=$xid,modifier=$modifier,timestamp=$timestamp,title=$title,root=$root)"

    companion object {
        fun parse(node: Node) = Sheet(node)
    }
}

class Relationship(node: Node) : XmindNode(node) {
    val from: String
    val to: String
    val title: String?

    init {
        from = node.el("@end1")!!.text!!
        to = node.el("@end2")!!.text!!
        title = node.el("title").text
    }

    override fun toString(): String =
        "${this::class.simpleName}(xid=$xid,modifier=$modifier,timestamp=$timestamp,title=$title,from=$from,to=$to)"

    companion object {
        fun parse(node: Node) = Relationship(node)
    }
}

object XmindParser {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun parse(path: String) = run {
        val uri = File(path).apply {
            try {
                if (!exists()) throw  Exception("$path is not exists")
                if (!isFile()) throw  Exception("$path is not file")
                if (!canRead()) throw  Exception("$path is not readable")
            } catch (e: Exception) {
                log.error(e.message)
                throw e
            }
        }.let {
            URI.create("jar:file:/${it.absolutePath.replace("\\", "/")}")
        }
        val xml = FileSystems.newFileSystem(uri, HashMap<String, Any>()).getPath("/content.xml")
        val os = ByteArrayOutputStream()
        Files.copy(xml, os)
        val doc =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(os.toByteArray()))
        doc.documentElement.childNodes.map { sheet ->
            Sheet(sheet)
        }.filterNotNull()
    }
}

