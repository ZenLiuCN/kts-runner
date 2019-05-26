import org.slf4j.LoggerFactory

fun getLogger(name: String) = LoggerFactory.getLogger(name)
fun getLogger(name: Class<*>) = LoggerFactory.getLogger(name)
